package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import org.lwjgl.vulkan.VK10;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

public class CommandPool {

   public final long ptr;
   
   public static CommandPool createCommandPool(int graphicsI) {
      CommandPool pool = new CommandPool(graphicsI);
      return pool;
   }

   private CommandPool(int queueFamilyIndex) {
      try (MemoryStack stack = stackPush()) {
         VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                 .queueFamilyIndex(queueFamilyIndex)
                 .flags(0);
         LongBuffer pCmdPool = stack.mallocLong(1);
         vkAssert(vkCreateCommandPool(device.logical, cmdPoolInfo, null, pCmdPool));
         ptr = pCmdPool.get(0);
      }
   }

   public VkCommandBuffer createCommandBuffer() {
      try (MemoryStack stack = stackPush()) {
         VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                 .commandPool(ptr)
                 .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                 .commandBufferCount(1);
         PointerBuffer pCommandBuffer = stack.mallocPointer(1);
         vkAssert(vkAllocateCommandBuffers(device.logical, cmdBufAllocateInfo, pCommandBuffer));
         long commandBuffer = pCommandBuffer.get(0);
         return new VkCommandBuffer(commandBuffer, device.logical);
      }
   }

   public void dispose() {
      VK10.vkDestroyCommandPool(device.logical, ptr, null);
   }

}
