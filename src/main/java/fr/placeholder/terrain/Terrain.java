package fr.placeholder.terrain;

import fr.placeholder.vulkanproject.Orchestrated;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import org.lwjgl.vulkan.VkCommandBuffer;

public class Terrain extends Orchestrated {

   @Override
   public void init() {
      pipe = new TerrainPipeline(this);
      data = new TerrainData(this);
      data.init();
   }
   
   public TerrainPipeline pipe;
   public TerrainData data;
   
   public void prepare(long renderpass, int subpass) {
      pipe.init(renderpass, subpass);
   }
   
   public void draw(VkCommandBuffer commandBuffer) {
      LongBuffer vertexBuffers = MemoryStack.stackMallocLong(1), vertexOffsets = MemoryStack.stackMallocLong(1);
      vertexBuffers.put(0, data.vertexBuffer.get());
      vertexOffsets.put(0, data.vertexBuffer.memOffset());
      vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipe.line);
      vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, vertexOffsets);
      vkCmdDraw(commandBuffer, 3, 1, 0, 0);
   }
   
   public void update() {
      
      
      
   }

   @Override
   public void dispose() {
      super.dispose();
      pipe.dispose();
      data.dispose();
   }
   
}
