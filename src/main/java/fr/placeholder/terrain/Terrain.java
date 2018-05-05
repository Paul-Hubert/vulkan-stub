package fr.placeholder.terrain;

import fr.placeholder.vulkanproject.Orchestrated;
import java.nio.LongBuffer;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import org.lwjgl.vulkan.VkCommandBuffer;

public class Terrain extends Orchestrated {

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
      vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipe.line);
      vkCmdBindVertexBuffers(commandBuffer, 0, data.vertexBuffers, data.vertexOffsets);
      vkCmdDraw(commandBuffer, 3, 1, 0, 0);
   }

   public LongBuffer waitSemaphores, signalSemaphores;
   
   public void update() {
      
      
      
   }

   public void dispose() {
      pipe.dispose();
      data.dispose();
   }
   
}
