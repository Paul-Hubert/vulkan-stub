package fr.placeholder.terrain;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.transfer;
import fr.placeholder.vulkanproject.GPUBuffer;
import fr.placeholder.vulkanproject.Memory;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import org.lwjgl.vulkan.VK10;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkBindBufferMemory;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

public class TerrainData {

   public TerrainData(Terrain t) {
      terrain = t;
   }

   public Terrain terrain;

   public void init() {
      vertexBuffers = memAllocLong(1);
      vertexOffsets = memAllocLong(1);
      
      try (MemoryStack stack = stackPush()) {
         int vertexSize = 3 * 2 * 4;
         int totalSize = 512;

         ByteBuffer vertex = stack.malloc(vertexSize);
         FloatBuffer fb = vertex.asFloatBuffer();
         // The triangle will showup upside-down, because Vulkan does not do proper viewport transformation to
         // account for inverted Y axis between the window coordinate system and clip space/NDC
         fb.put(-0.5f).put(-0.5f);
         fb.put(0.5f).put(-0.5f);
         fb.put(0.0f).put(0.5f);

         stagingBuffer = new GPUBuffer(totalSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT);

         stagingMemory = new Memory(totalSize, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, stagingBuffer);

         vertexBuffer = new GPUBuffer(totalSize, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT);
         vertexBuffers.put(0, vertexBuffer.ptr);
         vertexOffsets.put(0, 0);

         vertexMemory = new Memory(totalSize, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, vertexBuffer);

         vkAssert(vkBindBufferMemory(device.logical, vertexBuffer.ptr, vertexMemory.ptr, vertexOffsets.get(0)));
         vkAssert(vkBindBufferMemory(device.logical, stagingBuffer.ptr, stagingMemory.ptr, 0));

         copyCommand = transfer.createCommandBuffer();
         
	 updateBuffer(vertex, vertexSize, 0, 0);
      }
   }

   
   
   LongBuffer vertexBuffers, vertexOffsets;
   GPUBuffer vertexBuffer, stagingBuffer;
   Memory vertexMemory, stagingMemory;
   
   VkCommandBuffer copyCommand;
   
   

   public VkPipelineVertexInputStateCreateInfo getVertexState() {
      // Binding description
      VkVertexInputBindingDescription.Buffer bindingDescriptor = VkVertexInputBindingDescription.callocStack(1)
              .binding(0) // <- we bind our vertex buffer to point 0
              .stride(2 * 4)
              .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

      // Attribute descriptions
      // Describes memory layout and shader attribute locations
      VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.callocStack(1);
      // Location 0 : Position
      attributeDescriptions.get(0)
              .binding(0) // <- binding point used in the VkVertexInputBindingDescription
              .location(0) // <- location in the shader's attribute layout (inside the shader source)
              .format(VK_FORMAT_R32G32_SFLOAT)
              .offset(0);

      // Assign to vertex buffer
      VkPipelineVertexInputStateCreateInfo vi = VkPipelineVertexInputStateCreateInfo.callocStack()
              .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
              .pVertexBindingDescriptions(bindingDescriptor)
              .pVertexAttributeDescriptions(attributeDescriptions);

      return vi;
   }

   public void updateBuffer(ByteBuffer vertex, long size, long srcoffset, long dstoffset) {
      try (MemoryStack stack = stackPush()) {
         // Write to staging buffer 
         PointerBuffer pData = stack.mallocPointer(1);
         vkAssert(vkMapMemory(device.logical, stagingMemory.ptr, 0, stagingMemory.size, 0, pData));
         long data = pData.get(0);

         MemoryUtil.memCopy(memAddress(vertex), data, size);

         vkUnmapMemory(device.logical, stagingMemory.ptr);
	 
	 VkBufferCopy.Buffer region = VkBufferCopy.callocStack(1,stack);
	 region.get(0).set(srcoffset, dstoffset, size);
         
         VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                 .flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
	 
         vkBeginCommandBuffer(copyCommand, beginInfo);
         
         vkCmdCopyBuffer(copyCommand, stagingBuffer.ptr, vertexBuffer.ptr, region);
         
         vkAssert(vkEndCommandBuffer(copyCommand));
         
         transfer.add(copyCommand);
         
      }
   }

   public void dispose() {
      vertexBuffer.dispose();
      vertexMemory.dispose();
      stagingBuffer.dispose();
      stagingMemory.dispose();
      memFree(vertexBuffers);
      memFree(vertexOffsets);
   }

}
