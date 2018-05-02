package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.swap;
import static fr.placeholder.vulkanproject.Main.render;
import static fr.placeholder.vulkanproject.SwapChain.NUM_FRAMES;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK10.vkDestroySemaphore;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

public class Renderer {
   
   public void init() {
      try(MemoryStack stack = stackPush()) {
         current = 0;
         pImageIndex = memAllocInt(1);
         LongBuffer pSwapchains = stack.mallocLong(1);
         
         // Create semaphores
         imageAcquired = new long[NUM_FRAMES];
         VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                 .pNext(NULL)
                 .flags(0);
         pImageSemaphores = memAllocLong(1);
         pRenderSemaphores = memAllocLong(1);
         LongBuffer psemaphore = stack.mallocLong(1);
         for(int i = 0; i<NUM_FRAMES; i++) {
            vkAssert(vkCreateSemaphore(device.logical, semaphoreCreateInfo, null, psemaphore));
            imageAcquired[i] = psemaphore.get(0);
         }


         pSwapchains.put(0, swap.chain);
         // Info struct to present the current swapchain image to the display
         presentInfo = VkPresentInfoKHR.calloc()
                 .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                 .pNext(NULL)
                 .pWaitSemaphores(pRenderSemaphores)
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
   private LongBuffer pImageSemaphores, pRenderSemaphores;
   private long[] imageAcquired;
   
   private VkPresentInfoKHR presentInfo;

   public void render() {
      
      // Get next image from the swap chain (back/front buffer).
      // This will setup the imageAquiredSemaphore to be signalled when the operation is complete
      pImageSemaphores.put(0, imageAcquired[current]);
      vkAssert(vkAcquireNextImageKHR(device.logical, swap.chain, Long.MAX_VALUE, imageAcquired[current], NULL, pImageIndex));
      current = pImageIndex.get(0);

      render.render(current, pImageSemaphores, pRenderSemaphores);
      
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
         
      for(int i = 0; i<NUM_FRAMES; i++) {
         vkDestroySemaphore(device.logical, imageAcquired[i], null);
      }
      
      memFree(pImageIndex);
      memFree(pImageSemaphores);
      memFree(pRenderSemaphores);
      presentInfo.free();
   }
   
}
