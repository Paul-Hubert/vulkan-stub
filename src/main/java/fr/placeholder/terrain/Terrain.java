package fr.placeholder.terrain;

import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import org.lwjgl.vulkan.VkCommandBuffer;

public class Terrain {

   public void init() {
      
      pipe = new TerrainPipeline();
      
      
   }
   
   private TerrainPipeline pipe; 
   
   public void prepare(long renderpass, int subpass) {
      pipe.init(renderpass, subpass);
   }
   
   public void draw(VkCommandBuffer commandBuffer) {
      vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipe.line);
      
      vkCmdDraw(commandBuffer, 3, 1, 0, 0);
   }

   public void update() {
      
      
      
   }

   public void dispose() {
      pipe.dispose();
   }
   
}
