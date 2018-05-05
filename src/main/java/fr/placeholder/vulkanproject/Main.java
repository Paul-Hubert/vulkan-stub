package fr.placeholder.vulkanproject;

import fr.placeholder.terrain.Terrain;
import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.win;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;

public class Main {
   
   public static void main(String[] args) {
      Main main = new Main();
      main.init();
      main.run();
      main.dispose();
   }
   
   public static Renderer renderer;
   public static RenderPass render;
   public static Terrain terrain;

   private void init() {
      Context.init();
      
      terrain = new Terrain();
      terrain.init();
      
      render = new RenderPass();
      renderer = new Renderer();
      renderer.init();
      
      Orchestrator.init();
   }

   private void run() {
      win.show();
      Orchestrator.run();
   }

   private void dispose() {
      vkQueueWaitIdle(device.graphics);
      vkDeviceWaitIdle(device.logical);
      
      renderer.dispose();
      render.dispose();
      terrain.dispose();
      Context.dispose();
   }
   
}
