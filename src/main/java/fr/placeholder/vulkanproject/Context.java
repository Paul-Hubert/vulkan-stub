package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Device.getDevice;
import static fr.placeholder.vulkanproject.Instance.createInstance;
import static fr.placeholder.vulkanproject.SwapChain.createSwapChain;
import static fr.placeholder.vulkanproject.Transfer.createTransfer;
import static fr.placeholder.vulkanproject.Windu.createWindu;
import static fr.placeholder.vulkanproject.Windu.initGLFWContext;

public class Context {

   public static Instance instance;
   public static Windu win;
   public static Device device;
   public static SwapChain swap;
   public static Transfer transfer;
   
   public static void init() {
      initGLFWContext();
      
      instance = createInstance();
      
      Synchronization.init();
      
      win = createWindu();
      
      device = getDevice();
      
      swap = createSwapChain();
      
      transfer = createTransfer();
      
   }

   protected static void dispose() {
      transfer.dispose();
      swap.dispose();
      Synchronization.dispose();
      device.dispose();
      win.dispose();
      instance.dispose();
   }

}
