package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

public class Memory {
   
   public long ptr;
   
   public Memory(long size, int properties) {
      try(MemoryStack stack = stackPush()) {
         VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                 .allocationSize(size)
                 .memoryTypeIndex(findMemoryType(properties));

         LongBuffer bufferMemory = stack.mallocLong(1);
         vkAssert(vkAllocateMemory(device.logical, allocInfo, null, bufferMemory));
         ptr = bufferMemory.get(0);
      }
   }
   
   public int findMemoryType(int properties) {
      VkPhysicalDeviceMemoryProperties memProperties = device.memoryProperties;
      
      for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
         if ((memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
            return i;
         }
     }
     
      throw new AssertionError("No suitable Memory Heap was found");
   }
   
}
