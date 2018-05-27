package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

public class Memory {

   private final long ptr;
   private final long size;

   public Memory(long size, int properties, VkiBuffer buf) {
      try (MemoryStack stack = stackPush()) {
         this.size = size;
         VkMemoryRequirements memReqs = VkMemoryRequirements.callocStack(stack);
         vkGetBufferMemoryRequirements(device.logical, buf.get(), memReqs);
         
         VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                 .allocationSize(size)
                 .memoryTypeIndex(getMemoryType(memReqs.memoryTypeBits(), properties));

         LongBuffer bufferMemory = stack.mallocLong(1);
         vkAssert(vkAllocateMemory(device.logical, allocInfo, null, bufferMemory));
         ptr = bufferMemory.get(0);
      }
   }

   public static int getMemoryType(int typeBits, int properties) {
      int bits = typeBits;
      for (int i = 0; i < 32; i++) {
         if ((bits & 1) == 1) {
            if ((device.memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
               return i;
            }
         }
         bits >>= 1;
      }
      return -1;
   }
   
   public long get() {
      return ptr;
   }
   
   public long size() {
      return size;
   }
   
   public void dispose() {
      vkFreeMemory(device.logical, ptr, null);
   }

}
