package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkBindBufferMemory;
import static org.lwjgl.vulkan.VK10.vkCreateBuffer;
import org.lwjgl.vulkan.VkBufferCreateInfo;

public class GPUBuffer {
   
   public long ptr;
   
   public GPUBuffer(long size, long offset, int usage, Memory mem) {
      try(MemoryStack stack = stackPush()) {
         VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                 .size(size)
                 .usage(usage)
                 .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
         
         LongBuffer pbuffer = stack.mallocLong(1);
         vkAssert(vkCreateBuffer(device.logical, bufferInfo, null, pbuffer));
         ptr = pbuffer.get(0);
         
         vkBindBufferMemory(device.logical, ptr, mem.ptr, 0);
      }
   }
}
