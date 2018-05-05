package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.swap;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import org.lwjgl.vulkan.VkPresentInfoKHR;

public class Renderer extends Orchestrated {
   
   public void init() {
      try(MemoryStack stack = stackPush()) {
         current = 0;
         pImageIndex = memAllocInt(1);
         LongBuffer pSwapchains = stack.mallocLong(1);

         pSwapchains.put(0, swap.chain);
         // Info struct to present the current swapchain image to the display
         presentInfo = VkPresentInfoKHR.calloc()
                 .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                 .pNext(NULL)
                 .pWaitSemaphores(waitSemaphores)
                 .swapchainCount(1)
                 .pSwapchains(pSwapchains)
                 .pImageIndices(pImageIndex)
                 .pResults(null);

         // The render loop
         sum = 0;
         count = 0;
         last = System.currentTimeMillis();
      }
   }
   
   private int current = 0, count;
   private long last, sum;
   private IntBuffer pImageIndex;
   
   private VkPresentInfoKHR presentInfo;

   public int acquire() {
      vkAssert(vkAcquireNextImageKHR(device.logical, swap.chain, Long.MAX_VALUE, signalSemaphores.get(0), NULL, pImageIndex));
      current = pImageIndex.get(0);
      return current;
   }
   
   public void present() {
      // This will display the image
      vkAssert(vkQueuePresentKHR(device.graphics, presentInfo));

      count++;
      sum += System.currentTimeMillis() - last;
      if(count >= 100) {
         System.out.println(1000.0/(sum/100.0));
         count = 0;
         sum = 0;
      }
      last = System.currentTimeMillis();
   }
   
   public void dispose() {
      memFree(pImageIndex);
      presentInfo.free();
   }
   
}
