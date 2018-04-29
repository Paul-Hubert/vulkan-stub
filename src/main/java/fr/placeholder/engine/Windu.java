package fr.placeholder.engine;

import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWKeyCallback;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.VK10.*;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import static fr.placeholder.engine.Context.*;
import static fr.placeholder.engine.Utils.*;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

public class Windu {

   private static int width, height;
   private static long window;

   public Windu() {
      if (!glfwInit()) {
         throw new RuntimeException("Failed to initialize GLFW");
      }
      if (!glfwVulkanSupported()) {
         throw new AssertionError("GLFW failed to find the Vulkan loader");
      }

      PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
      if (requiredExtensions == null) {
         throw new AssertionError("Failed to find list of required Vulkan extensions");
      }

      createInstance(requiredExtensions);
      final VkDebugReportCallbackEXT debugCallback = new VkDebugReportCallbackEXT() {
         @Override
         public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
            System.err.println("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage));
            return 0;
         }
      };
      final long debugCallbackHandle = setupDebugging(instance, VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT, debugCallback);

      // Create GLFW window
      glfwDefaultWindowHints();
      glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
      glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
      window = glfwCreateWindow(800, 600, "GLFW Vulkan Demo", NULL, NULL);
      glfwSetKeyCallback(window, new GLFWKeyCallback() {
         @Override
         public void invoke(long window, int key, int scancode, int action, int mods) {
            if (action != GLFW_RELEASE) {
               return;
            }
            if (key == GLFW_KEY_ESCAPE) {
               glfwSetWindowShouldClose(window, true);
            }
         }
      });
      LongBuffer pSurface = memAllocLong(1);
      vkAssert(glfwCreateWindowSurface(instance, window, null, pSurface));
      final long surface = pSurface.get(0);
      
      
      // Handle canvas resize
      glfwSetWindowSizeCallback(window, new GLFWWindowSizeCallback() {
         @Override
         public void invoke(long window, int width, int height) {
            if (width <= 0 || height <= 0) {
               return;
            }
            Windu.width = width;
            Windu.height = height;
            //swapchainRecreator.mustRecreate = true;
         }
      });
      glfwShowWindow(window);

   }
   
   public int getWidth() {
      return width;
   }
   public int getHeigth() {
      return height;
   }
   
   public void dispose() {
      Context.dispose();
      glfwDestroyWindow(window);
      glfwTerminate();
   }

}
