package fr.placeholder.vulkanproject;

import fr.placeholder.terrain.Terrain;
import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.transfer;
import static fr.placeholder.vulkanproject.Context.win;
import org.lwjgl.system.Configuration;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;

public class Main {
   
   public static void main(String[] args) {
      Configuration.DEBUG.set(true);
      Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
      Configuration.DEBUG_STACK.set(true);
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
      
      System.out.println("\n\n\n");
      
      terrain = new Terrain();
      terrain.init();
      
      render = new RenderPass();
      render.init();
      renderer = new Renderer();
      renderer.init();
      
      Orchestrator.init();
      
      transfer.flush();
      
      vkDeviceWaitIdle(device.logical);
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