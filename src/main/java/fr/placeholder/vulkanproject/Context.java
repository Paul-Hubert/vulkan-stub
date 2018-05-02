package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.CommandPool.createCommandPool;
import static fr.placeholder.vulkanproject.Device.getPhysicalDevices;
import static fr.placeholder.vulkanproject.Instance.createInstance;
import static fr.placeholder.vulkanproject.Pipeline.createPipeline;
import static fr.placeholder.vulkanproject.RenderPass.createRenderPass;
import static fr.placeholder.vulkanproject.SwapChain.createSwapChain;
import static fr.placeholder.vulkanproject.Windu.createWindu;
import static fr.placeholder.vulkanproject.Windu.initGLFWContext;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.EXTDebugReport.*;

public class Context {

   public static Instance instance;
   public static Windu win;
   public static Device device;
   public static SwapChain swap;
   public static RenderPass render;
   public static Pipeline pipe;
   public static CommandPool pool;
   
   public static void init() {
      initGLFWContext();
      
      instance = createInstance();
      
      instance.setupDebugging(VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT, new VkDebugReportCallbackEXT() {
         @Override
         public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
            System.err.println("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage));
            return 0;
         }
      });
      
      win = createWindu();
      
      device = getPhysicalDevices();
      
      pool = createCommandPool();
      
      swap = createSwapChain();
      
      render = createRenderPass();
      
      pipe = createPipeline();
      
      render.createRenderCommandBuffers();
      
      win.show();
      
   }

   protected static void dispose() {
      pool.dispose();
      pipe.dispose();
      render.dispose();
      swap.dispose();
      device.dispose();
      win.dispose();
      instance.dispose();
   }

}
