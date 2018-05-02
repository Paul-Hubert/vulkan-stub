package fr.placeholder.vulkanproject;

import fr.placeholder.terrain.Terrain;
import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.swap;
import static fr.placeholder.vulkanproject.Context.win;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.vulkan.VK10;
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
      
   }

   private void run() {
      while (!glfwWindowShouldClose(win.window)) {
         glfwPollEvents();
         terrain.update();
         renderer.render();
      }
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
