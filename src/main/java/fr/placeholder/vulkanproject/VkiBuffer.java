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
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import org.lwjgl.vulkan.VkBufferCreateInfo;

public class VkiBuffer {
   
   private final long ptr;
   private Memory mem;
   private long memOffset;
   
   public VkiBuffer(long size, int usage) {
      try(MemoryStack stack = stackPush()) {
         VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                 .size(size)
                 .usage(usage)
                 .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
         
         LongBuffer pbuffer = stack.mallocLong(1);
         vkAssert(vkCreateBuffer(device.logical, bufferInfo, null, pbuffer));
         ptr = pbuffer.get(0);
      }
   }
   
   public void bind(Memory mem, long offset) {
      this.mem = mem;
      this.memOffset = offset;
      vkAssert(vkBindBufferMemory(device.logical, this.ptr, mem.get(), offset));
   }
   
   public long get() {
      return ptr;
   }
   
   public long memOffset() {
      return memOffset;
   }
   
   public Memory memory() {
      return mem;
   }
   
   public void dispose() {
      vkDestroyBuffer(device.logical, ptr, null);
   }
}
