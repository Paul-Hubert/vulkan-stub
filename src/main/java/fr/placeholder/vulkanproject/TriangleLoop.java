package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.render;
import static fr.placeholder.vulkanproject.Context.swap;
import static fr.placeholder.vulkanproject.Context.win;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
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
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

public class TriangleLoop {

   public static void loop() {
      try(MemoryStack stack = stackPush()) {
         int currentBuffer = 0;
         IntBuffer pImageIndex = stack.mallocInt(1);
         LongBuffer pSwapchains = stack.mallocLong(1);
         
         // Create semaphores
         int semaphoreCount = 2;
         long[] imageAcquired = new long[semaphoreCount], renderComplete = new long[semaphoreCount];
         int semaphoreIndex = 0;
         VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                 .pNext(NULL)
                 .flags(0);
         LongBuffer pImageSemaphores = stack.mallocLong(1);
         LongBuffer pRenderSemaphores = stack.mallocLong(1);
         for(int i = 0; i<semaphoreCount; i++) {
            vkAssert(vkCreateSemaphore(device.logical, semaphoreCreateInfo, null, pImageSemaphores));
            imageAcquired[i] = pImageSemaphores.get(0);
            vkAssert(vkCreateSemaphore(device.logical, semaphoreCreateInfo, null, pRenderSemaphores));
            renderComplete[i] = pRenderSemaphores.get(0);
         }
         
         
         PointerBuffer pCommandBuffers = stack.mallocPointer(1);

         // Info struct to submit a command buffer which will wait on the semaphore
         IntBuffer pWaitDstStageMask = stack.mallocInt(1);
         pWaitDstStageMask.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
         VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                 .pNext(NULL)
                 .waitSemaphoreCount(1)
                 .pWaitSemaphores(pImageSemaphores)
                 .pWaitDstStageMask(pWaitDstStageMask)
                 .pCommandBuffers(pCommandBuffers)
                 .pSignalSemaphores(pRenderSemaphores);

         pSwapchains.put(0, swap.chain);
         // Info struct to present the current swapchain image to the display
         VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                 .pNext(NULL)
                 .pWaitSemaphores(pRenderSemaphores)
                 .swapchainCount(1)
                 .pSwapchains(pSwapchains)
                 .pImageIndices(pImageIndex)
                 .pResults(null);

         // The render loop
         long last = System.currentTimeMillis(), sum = 0;
         int count = 0;
         while (!glfwWindowShouldClose(win.window)) {
            // Handle window messages. Resize events happen exactly here.
            // So it is safe to use the new swapchain images and framebuffers afterwards.
            glfwPollEvents();

            // Get next image from the swap chain (back/front buffer).
            // This will setup the imageAquiredSemaphore to be signalled when the operation is complete
             
            vkAssert(vkAcquireNextImageKHR(device.logical, swap.chain, Long.MAX_VALUE, imageAcquired[semaphoreIndex], NULL, pImageIndex));
            currentBuffer = pImageIndex.get(0);
            
            // Select the command buffer for the current framebuffer image/attachment
            pCommandBuffers.put(0, render.renderCommandBuffers[currentBuffer]);
            pImageSemaphores.put(0, imageAcquired[semaphoreIndex]);
            pRenderSemaphores.put(0, renderComplete[semaphoreIndex]);
            
            // Submit to the graphics queue
            vkAssert(vkQueueSubmit(device.graphics, submitInfo, NULL));
            
            // This will display the image
            vkAssert(vkQueuePresentKHR(device.graphics, presentInfo));
            
            semaphoreIndex=(semaphoreIndex+1)%semaphoreCount;
            
            count++;
            sum += System.currentTimeMillis() - last;
            if(count >= 100) {
               System.out.println(1000.0/(sum/100.0));
               count = 0;
               sum = 0;
            }
            last = System.currentTimeMillis();
        }

         vkDeviceWaitIdle(device.logical);
         
         for(int i = 0; i<semaphoreCount; i++) {
            vkDestroySemaphore(device.logical, imageAcquired[i], null);
            vkDestroySemaphore(device.logical, renderComplete[i], null);
         }
         
      }
   }

}
