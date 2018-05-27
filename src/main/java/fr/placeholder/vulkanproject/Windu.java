package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.instance;
import java.nio.LongBuffer;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWKeyCallback;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static fr.placeholder.vulkanproject.Utils.*;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class Windu {

   public int width = 800, height = 600;
   public long window;
   public long surface;
   
   public static void initGLFWContext() {
      if (!glfwInit()) {
         throw new RuntimeException("Failed to initialize GLFW");
      }
      if (!glfwVulkanSupported()) {
         throw new AssertionError("GLFW failed to find the Vulkan loader");
      }
   }
   
   public static Windu createWindu() {
      Windu windu = new Windu();
      windu.init();
      return windu;
   }
   
   private void init() {
      // Create GLFW window
      glfwDefaultWindowHints();
      glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
      glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
      window = glfwCreateWindow(width, height, "GLFW Vulkan Demo", NULL, NULL);
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
      
      try(MemoryStack stack = stackPush()) {
         LongBuffer pSurface = stack.mallocLong(1);
         vkAssert(glfwCreateWindowSurface(instance.vulkan, window, null, pSurface));
         surface = pSurface.get(0);
      }
   }
   
   public void show() {
      // Handle canvas resize
      glfwSetWindowSizeCallback(window, new GLFWWindowSizeCallback() {
         @Override
         public void invoke(long window, int width, int height) {
            if (width <= 0 || height <= 0) {
               return;
            }
            Context.win.width = width;
            Context.win.height = height;
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
      glfwFreeCallbacks(window);
      glfwDestroyWindow(window);
      glfwTerminate();
   }

}
