package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.transfer;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import org.lwjgl.vulkan.VK10;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

public class Transfer extends Orchestrated {
   
   public static Transfer createTransfer() {
      Transfer transfer = new Transfer();
      transfer.init();
      return transfer;
   }
   
   @Override
   public void init() {
      super.init();
      
      pCommands = memAllocPointer(3);
      tempCommands = memAllocPointer(3);
      waitDstStageMask = MemoryUtil.memAllocInt(waitSemaphores.capacity());
      waitDstStageMask.put(0, VK10.VK_PIPELINE_STAGE_VERTEX_INPUT_BIT);
      
      pool = new CommandPool(device.transferI, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
      
      submit = VkSubmitInfo.calloc()
              .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
              .pNext(NULL)
              .pCommandBuffers(pCommands)
              .pWaitSemaphores(waitSemaphores)
	      .pWaitDstStageMask(waitDstStageMask)
              .pSignalSemaphores(signalSemaphores);
      
      fence = Synchronization.createFence(false);
   }
   
   public CommandPool pool;
   
   // SYNCHRONIZER !!!
   
   public void add(VkCommandBuffer command) {
      if(pCommands.capacity() <= commandCount) {
         PointerBuffer temp = pCommands;
         pCommands = MemoryUtil.memAllocPointer(commandCount*2);
         tempCommands = MemoryUtil.memAllocPointer(pCommands.capacity());
         for(int i = 0; i < commandCount; i++) {
            pCommands.put(i, temp.get(i));
         }
         memFree(temp);
      }
      pCommands.put(commandCount, command);
      commandCount++;
   }
   
   private VkSubmitInfo submit;
   private IntBuffer waitDstStageMask;
   
   private PointerBuffer pCommands, tempCommands;
   private int fence;
   private int commandCount = 0;
   
   public void flush() {
      pCommands.limit(commandCount);
      waitDstStageMask.limit(waitSemaphores.remaining());
      
      long curFence = Synchronization.getFence(fence);
      
      submit.waitSemaphoreCount(waitSemaphores.remaining())
	      .pCommandBuffers(pCommands)
              .pWaitSemaphores(waitSemaphores)
	      .pWaitDstStageMask(waitDstStageMask)
              .pSignalSemaphores(signalSemaphores);
      vkAssert(vkQueueSubmit(device.transfer, submit, curFence));
      
      for(int i = 0; i < commandCount; i++) {
         tempCommands.put(i, pCommands.get(i));
      }
      
      Utils.dispatch(() -> {
         VK10.vkWaitForFences(device.logical, curFence, false, Long.MAX_VALUE);
         VK10.vkResetFences(device.logical, curFence);
         VK10.vkFreeCommandBuffers(device.logical, pool.ptr, pCommands);
      });
      
      commandCount = 0;
      
   }
   
   public void updateBuffer(ByteBuffer vertex, VkiBuffer stage, VkiBuffer target, long dstoffset) {
      try (MemoryStack stack = stackPush()) {
         // Write to staging buffer 
         PointerBuffer pData = stack.mallocPointer(1);
         long size = vertex.remaining();
         vkAssert(vkMapMemory(device.logical, stage.memory().get(), 0, size, 0, pData));
         long data = pData.get(0);

         MemoryUtil.memCopy(memAddress(vertex), data, size);

         vkUnmapMemory(device.logical, stage.memory().get());
	 
	 VkBufferCopy.Buffer regions = VkBufferCopy.callocStack(1,stack);
	 regions.get(0).set(0, dstoffset, size);
         
         VkCommandBuffer copyCommand = transfer.pool.createCommandBuffer();
         
         VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                 .flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
	 
         vkBeginCommandBuffer(copyCommand, beginInfo);
         
         vkCmdCopyBuffer(copyCommand, stage.get(), target.get(), regions);
         
         vkAssert(vkEndCommandBuffer(copyCommand));
         
         transfer.add(copyCommand);
      }
   }
   
   @Override
   public void dispose() {
      super.dispose();
      submit.free();
      memFree(pCommands);
      memFree(tempCommands);
      memFree(waitDstStageMask);
      pool.dispose();
   }
   
}
