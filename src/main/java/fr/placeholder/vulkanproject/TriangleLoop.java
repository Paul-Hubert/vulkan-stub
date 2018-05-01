package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.CommandPool.submitPostPresentBarrier;
import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.pool;
import static fr.placeholder.vulkanproject.Context.swap;
import static fr.placeholder.vulkanproject.Context.win;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.PointerBuffer;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK10.vkDestroySemaphore;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

public class TriangleLoop {

   public static void loop() {
      IntBuffer pImageIndex = memAllocInt(1);
      int currentBuffer = 0;
      PointerBuffer pCommandBuffers = memAllocPointer(1);
      LongBuffer pSwapchains = memAllocLong(1);
      LongBuffer pImageAcquiredSemaphore = memAllocLong(1);
      LongBuffer pRenderCompleteSemaphore = memAllocLong(1);

      // Info struct to create a semaphore
      VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc()
              .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
              .pNext(NULL)
              .flags(0);

      // Info struct to submit a command buffer which will wait on the semaphore
      IntBuffer pWaitDstStageMask = memAllocInt(1);
      pWaitDstStageMask.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
      VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
              .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
              .pNext(NULL)
              .waitSemaphoreCount(pImageAcquiredSemaphore.remaining())
              .pWaitSemaphores(pImageAcquiredSemaphore)
              .pWaitDstStageMask(pWaitDstStageMask)
              .pCommandBuffers(pCommandBuffers)
              .pSignalSemaphores(pRenderCompleteSemaphore);

      // Info struct to present the current swapchain image to the display
      VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc()
              .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
              .pNext(NULL)
              .pWaitSemaphores(pRenderCompleteSemaphore)
              .swapchainCount(pSwapchains.remaining())
              .pSwapchains(pSwapchains)
              .pImageIndices(pImageIndex)
              .pResults(null);
      
      // The render loop
      while (!glfwWindowShouldClose(win.window)) {
         // Handle window messages. Resize events happen exactly here.
         // So it is safe to use the new swapchain images and framebuffers afterwards.
         glfwPollEvents();
         
         // Create a semaphore to wait for the swapchain to acquire the next image
         vkAssert(vkCreateSemaphore(device.logical, semaphoreCreateInfo, null, pImageAcquiredSemaphore));
         
         // Create a semaphore to wait for the render to complete, before presenting
         vkAssert(vkCreateSemaphore(device.logical, semaphoreCreateInfo, null, pRenderCompleteSemaphore));

         // Get next image from the swap chain (back/front buffer).
         // This will setup the imageAquiredSemaphore to be signalled when the operation is complete
         vkAssert(vkAcquireNextImageKHR(device.logical, swap.chain, Long.MAX_VALUE, pImageAcquiredSemaphore.get(0), NULL, pImageIndex));
         currentBuffer = pImageIndex.get(0);
         
         
         // Select the command buffer for the current framebuffer image/attachment
         pCommandBuffers.put(0, pool.renderCommandBuffers[currentBuffer]);
         // Submit to the graphics queue
         vkAssert(vkQueueSubmit(device.graphics, submitInfo, NULL));
         // Present the current buffer to the swap chain
         // This will display the image
         pSwapchains.put(0, swap.chain);
         vkAssert(vkQueuePresentKHR(device.graphics, presentInfo));
         // Create and submit post present barrier
         vkQueueWaitIdle(device.graphics);

         // Destroy this semaphore (we will create a new one in the next frame)
         vkDestroySemaphore(device.logical, pImageAcquiredSemaphore.get(0), null);
         vkDestroySemaphore(device.logical, pRenderCompleteSemaphore.get(0), null);
         submitPostPresentBarrier(swap.images[currentBuffer], pool.postPresentCommandBuffer);
         
     }
      
      vkDeviceWaitIdle(device.logical);
      
      presentInfo.free();
      memFree(pWaitDstStageMask);
      submitInfo.free();
      memFree(pImageAcquiredSemaphore);
      memFree(pRenderCompleteSemaphore);
      semaphoreCreateInfo.free();
      memFree(pSwapchains);
      memFree(pCommandBuffers);
   }

}
