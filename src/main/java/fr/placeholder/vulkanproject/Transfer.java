package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

public class Transfer extends Orchestrated {
   
   public static Transfer createTransfer() {
      Transfer transfer = new Transfer();
      transfer.init();
      return transfer;
   }
   
   public void init() {
      
      pCommands = memAllocPointer(3);
      signalSemaphores = memAllocLong(1);
      
      createCommandPool();
      
      submit = VkSubmitInfo.calloc()
              .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
              .pNext(NULL)
              .pCommandBuffers(pCommands)
              .pSignalSemaphores(waitSemaphores)
              .pSignalSemaphores(signalSemaphores);
      
   }
   
   private long pool;
   
   // SYNCHRONIZER !!!
   
   public void add(VkCommandBuffer command) {
      if(pCommands.capacity() <= commandCount) {
         PointerBuffer temp = pCommands;
         pCommands = temp.getPointerBuffer(commandCount*2);
         memFree(temp);
      }
      pCommands.put(commandCount, command);
      commandCount++;
   }
   
   private VkSubmitInfo submit;
   
   private PointerBuffer pCommands;
   private int commandCount = 0;
   
   public void flush() {
      pCommands.limit(commandCount);
      submit.pCommandBuffers(pCommands);
      submit.waitSemaphoreCount(waitSemaphores.remaining());
      vkAssert(vkQueueSubmit(device.transfer, submit, NULL));
      commandCount = 0;
   }
   
   public void dispose() {
      submit.free();
      memFree(pCommands);
      memFree(signalSemaphores);
      memFree(waitSemaphores);
      vkDestroyCommandPool(device.logical, pool, null);
   }
   
   private void createCommandPool() {
      try (MemoryStack stack = stackPush()) {
         VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
                 .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                 .queueFamilyIndex(device.transferI)
                 .flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
         LongBuffer pCmdPool = stack.mallocLong(1);
         vkAssert(vkCreateCommandPool(device.logical, cmdPoolInfo, null, pCmdPool));
         pool = pCmdPool.get(0);
      }
   }

   public VkCommandBuffer createCommandBuffer() {
      try (MemoryStack stack = stackPush()) {
         VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                 .commandPool(pool)
                 .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                 .commandBufferCount(1);
         PointerBuffer pCommandBuffer = stack.mallocPointer(1);
         vkAssert(vkAllocateCommandBuffers(device.logical, cmdBufAllocateInfo, pCommandBuffer));
         long commandBuffer = pCommandBuffer.get(0);
         return new VkCommandBuffer(commandBuffer, device.logical);
      }
   }
   
}
