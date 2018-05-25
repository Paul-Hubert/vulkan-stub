package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.CommandPool.createCommandPool;
import static fr.placeholder.vulkanproject.Device.getDevice;
import static fr.placeholder.vulkanproject.Instance.createInstance;
import static fr.placeholder.vulkanproject.SwapChain.createSwapChain;
import static fr.placeholder.vulkanproject.Transfer.createTransfer;
import static fr.placeholder.vulkanproject.Windu.createWindu;
import static fr.placeholder.vulkanproject.Windu.initGLFWContext;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.EXTDebugReport.*;

public class Context {

   public static Instance instance;
   public static Windu win;
   public static Device device;
   public static SwapChain swap;
   public static Transfer transfer;
   
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
      
      device = getDevice();
      
      swap = createSwapChain();
      
      transfer = createTransfer();
      
   }

   protected static void dispose() {
      transfer.dispose();
      swap.dispose();
      device.dispose();
      win.dispose();
      instance.dispose();
   }

}
