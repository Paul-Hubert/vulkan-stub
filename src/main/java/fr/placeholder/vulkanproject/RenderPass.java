package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.swap;
import static fr.placeholder.vulkanproject.Main.terrain;
import static fr.placeholder.vulkanproject.SwapChain.NUM_FRAMES;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

public class RenderPass extends Orchestrated {
   
   public RenderPass() {
      
      //Create RenderPass
      try (MemoryStack stack = stackPush()) {
         VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(1,stack);
         attachments.get(0).format(swap.surfaceFormat.format())
                 .samples(VK_SAMPLE_COUNT_1_BIT)
                 .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                 .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                 .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

         // Now we enumerate the attachments for a subpass.  We have to have at least one subpass.
         VkAttachmentReference.Buffer colorRef = VkAttachmentReference.callocStack(1, stack)
                 .attachment(0)
                 .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
         
         // Basically is this graphics or compute
         VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack)
                 .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                 .colorAttachmentCount(1)
                 .pColorAttachments(colorRef);
         
         VkSubpassDependency.Buffer pdependency = VkSubpassDependency.callocStack(1, stack)
                 .srcSubpass(VK_SUBPASS_EXTERNAL)
                 .dstSubpass(0)
                 .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                 .srcAccessMask(0)
                 .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                 .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
         
         VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attachments)
                .pSubpasses(subpass)
                .pDependencies(pdependency);
         
         LongBuffer pRenderPass = stack.mallocLong(1);
         vkAssert(vkCreateRenderPass(device.logical, renderPassInfo, null, pRenderPass));
         pass = pRenderPass.get(0);
      }
      
      
      
      // Create Framebuffer
      try (MemoryStack stack = stackPush()) {
         LongBuffer pattachments = stack.mallocLong(1);
         VkFramebufferCreateInfo fci = VkFramebufferCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                 .pAttachments(pattachments)
                 .flags(0)
                 .height(swap.extent.height())
                 .width(swap.extent.width())
                 .layers(1)
                 .pNext(NULL)
                 .renderPass(pass);
         // Create a framebuffer for each swapchain image
         framebuffers = new long[NUM_FRAMES];
         LongBuffer pFramebuffer = stack.mallocLong(1);
         for (int i = 0; i < NUM_FRAMES; i++) {
            pattachments.put(0, swap.imageViews[i]);
            vkAssert(vkCreateFramebuffer(device.logical, fci, null, pFramebuffer));
            long framebuffer = pFramebuffer.get(0);
            framebuffers[i] = framebuffer;
         }
      
      }
      
      
      // Create CommandBuffers
      
      pool = new CommandPool(device.graphicsI, 0);
      
      // Create the render command buffers (one command buffer per framebuffer image)
      renderCommandBuffers = new VkCommandBuffer[SwapChain.NUM_FRAMES];
      for (int i = 0; i < SwapChain.NUM_FRAMES; i++) {
         renderCommandBuffers[i] = pool.createCommandBuffer();
      }
      
      
      // Record RenderCommandBuffers

      try (MemoryStack stack = stackPush()) {
         
         // Create the command buffer begin structure
         VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                 .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                 .pNext(NULL);

         // Specify clear color (cornflower blue)
         VkClearValue.Buffer clearValues = VkClearValue.callocStack(1, stack);
         clearValues.color()
                 .float32(0, 100 / 255.0f)
                 .float32(1, 149 / 255.0f)
                 .float32(2, 237 / 255.0f)
                 .float32(3, 1.0f);

         // Specify everything to begin a render pass
         VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                 .pNext(NULL)
                 .renderPass(pass)
                 .pClearValues(clearValues);
         VkRect2D renderArea = renderPassBeginInfo.renderArea();
         renderArea.offset().set(0, 0);
         renderArea.extent(swap.extent);
         
         terrain.prepare(pass, 0);

         for (int i = 0; i < SwapChain.NUM_FRAMES; ++i) {
            // Set target frame buffer
            renderPassBeginInfo.framebuffer(framebuffers[i]);

            vkAssert(vkBeginCommandBuffer(renderCommandBuffers[i], cmdBufInfo));

            vkCmdBeginRenderPass(renderCommandBuffers[i], renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
            
            terrain.draw(renderCommandBuffers[i]);
            
            vkCmdEndRenderPass(renderCommandBuffers[i]);

            vkAssert(vkEndCommandBuffer(renderCommandBuffers[i]));
         }
      }
      
   }
   
   @Override
   public void init() {
      super.init();
      
      pCommandBuffers = memAllocPointer(1);
      // Info struct to submit a command buffer which will wait on the semaphore
      waitDstStageMask = memAllocInt(3);
      waitDstStageMask.put(1, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT);
      waitDstStageMask.put(0, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT);
      submitInfo = VkSubmitInfo.calloc()
              .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
              .pWaitSemaphores(waitSemaphores)
              .pSignalSemaphores(signalSemaphores)
              .pWaitDstStageMask(waitDstStageMask)
              .pCommandBuffers(pCommandBuffers);
      
   }
   
   public long pass;
   public CommandPool pool;
   public VkCommandBuffer[] renderCommandBuffers;
   public long[] framebuffers;
   
   public PointerBuffer pCommandBuffers;
   public VkSubmitInfo submitInfo;
   public IntBuffer waitDstStageMask;
   
   public void render(int i) {
      pCommandBuffers.put(0, renderCommandBuffers[i]);
      waitDstStageMask.limit(waitSemaphores.limit());
      submitInfo.waitSemaphoreCount(waitSemaphores.remaining())
	      .pWaitSemaphores(waitSemaphores)
              .pSignalSemaphores(signalSemaphores)
              .pWaitDstStageMask(waitDstStageMask)
              .pCommandBuffers(pCommandBuffers);
      vkAssert(vkQueueSubmit(device.graphics, submitInfo, VK_NULL_HANDLE));
   }
   
   
   @Override
   public void dispose() {
      super.dispose();
      
      memFree(submitInfo.pWaitDstStageMask());
      submitInfo.free();
      vkFreeCommandBuffers(device.logical, pool.ptr, renderCommandBuffers[0]);
      vkFreeCommandBuffers(device.logical, pool.ptr, renderCommandBuffers[1]);
      memFree(pCommandBuffers);
      pool.dispose();
      vkDestroyFramebuffer(device.logical, framebuffers[0], null);
      vkDestroyFramebuffer(device.logical, framebuffers[1], null);
      vkDestroyRenderPass(device.logical, pass, null);
   }

}
