package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.pipe;
import static fr.placeholder.vulkanproject.Context.render;
import static fr.placeholder.vulkanproject.Context.swap;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import org.lwjgl.vulkan.VK10;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import static org.lwjgl.vulkan.VK10.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

public class CommandPool {

   public final long ptr;
   public VkCommandBuffer[] renderCommandBuffers;
   public VkCommandBuffer postPresentCommandBuffer;

   public static CommandPool createCommandPool() {
      CommandPool pool = new CommandPool(device.graphicsI);
      pool.createRenderCommandBuffers();
      return pool;
   }

   private CommandPool(int queueFamilyIndex) {
      try (MemoryStack stack = stackPush()) {
         VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
                 .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                 .queueFamilyIndex(queueFamilyIndex)
                 .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
         LongBuffer pCmdPool = stack.mallocLong(1);
         vkAssert(vkCreateCommandPool(device.logical, cmdPoolInfo, null, pCmdPool));
         ptr = pCmdPool.get(0);
      }
   }

   public VkCommandBuffer createCommandBuffer() {
      try (MemoryStack stack = stackPush()) {
         VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                 .commandPool(ptr)
                 .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                 .commandBufferCount(1);
         PointerBuffer pCommandBuffer = stack.mallocPointer(1);
         vkAssert(vkAllocateCommandBuffers(device.logical, cmdBufAllocateInfo, pCommandBuffer));
         long commandBuffer = pCommandBuffer.get(0);
         return new VkCommandBuffer(commandBuffer, device.logical);
      }
   }

   private void createRenderCommandBuffers() {

      postPresentCommandBuffer = createCommandBuffer();

      try (MemoryStack stack = stackPush()) {
         // Create the render command buffers (one command buffer per framebuffer image)
         VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                 .commandPool(ptr)
                 .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                 .commandBufferCount(SwapChain.NUM_FRAMES);
         PointerBuffer pCommandBuffer = stack.mallocPointer(swap.framebuffers.length);
         vkAssert(vkAllocateCommandBuffers(device.logical, cmdBufAllocateInfo, pCommandBuffer));

         renderCommandBuffers = new VkCommandBuffer[swap.framebuffers.length];
         for (int i = 0; i < SwapChain.NUM_FRAMES; i++) {
            renderCommandBuffers[i] = new VkCommandBuffer(pCommandBuffer.get(i), device.logical);
         }
      }

      try (MemoryStack stack = stackPush()) {
         // Create the command buffer begin structure
         VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
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
                 .renderPass(render.pass)
                 .pClearValues(clearValues);
         VkRect2D renderArea = renderPassBeginInfo.renderArea();
         renderArea.offset().set(0, 0);
         renderArea.extent(swap.extent);

         for (int i = 0; i < renderCommandBuffers.length; ++i) {
            // Set target frame buffer
            renderPassBeginInfo.framebuffer(swap.framebuffers[i]);

            vkAssert(vkBeginCommandBuffer(renderCommandBuffers[i], cmdBufInfo));

            vkCmdBeginRenderPass(renderCommandBuffers[i], renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            // Bind the rendering pipeline (including the shaders)
            vkCmdBindPipeline(renderCommandBuffers[i], VK_PIPELINE_BIND_POINT_GRAPHICS, pipe.line);

            // Draw triangle
            vkCmdDraw(renderCommandBuffers[i], 3, 1, 0, 0);

            vkCmdEndRenderPass(renderCommandBuffers[i]);

            // Add a present memory barrier to the end of the command buffer
            // This will transform the frame buffer color attachment to a
            // new layout for presenting it to the windowing system integration 
            VkImageMemoryBarrier.Buffer prePresentBarrier = createPrePresentBarrier(swap.images[i]);
            vkCmdPipelineBarrier(renderCommandBuffers[i],
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                    0,
                    null, // No memory barriers
                    null, // No buffer memory barriers
                    prePresentBarrier); // One image memory barrier

            vkAssert(vkEndCommandBuffer(renderCommandBuffers[i]));
         }
      }
   }

   public static VkImageMemoryBarrier.Buffer createPrePresentBarrier(long presentImage) {
      VkImageMemoryBarrier.Buffer imageMemoryBarrier = VkImageMemoryBarrier.callocStack(1)
              .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
              .pNext(NULL)
              .srcAccessMask(0) // VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
              .dstAccessMask(0)
              .oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
              .newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
              .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
              .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
      imageMemoryBarrier.subresourceRange()
              .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
              .baseMipLevel(0)
              .levelCount(1)
              .baseArrayLayer(0)
              .layerCount(1);
      imageMemoryBarrier.image(presentImage);
      return imageMemoryBarrier;
   }

   public static VkImageMemoryBarrier.Buffer createPostPresentBarrier(long presentImage) {
      VkImageMemoryBarrier.Buffer imageMemoryBarrier = VkImageMemoryBarrier.callocStack(1)
              .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
              .pNext(NULL)
              .srcAccessMask(0)
              .dstAccessMask(0) //VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
              .oldLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
              .newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
              .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
              .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
      imageMemoryBarrier.subresourceRange()
              .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
              .baseMipLevel(0)
              .levelCount(1)
              .baseArrayLayer(0)
              .layerCount(1);
      imageMemoryBarrier.image(presentImage);
      return imageMemoryBarrier;
   }

   public static void submitPostPresentBarrier(long image, VkCommandBuffer commandBuffer) {
      try (MemoryStack stack = stackPush()) {
         VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                 .pNext(NULL);
         vkAssert(vkBeginCommandBuffer(commandBuffer, cmdBufInfo));

         VkImageMemoryBarrier.Buffer postPresentBarrier = createPostPresentBarrier(image);
         vkCmdPipelineBarrier(
                 commandBuffer,
                 VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                 VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                 0,
                 null, // No memory barriers,
                 null, // No buffer barriers,
                 postPresentBarrier); // one image barrier

         vkAssert(vkEndCommandBuffer(commandBuffer));

         // Submit the command buffer
         submitCommandBuffer(device.graphics, commandBuffer);
      }
   }

   public static void submitCommandBuffer(VkQueue queue, VkCommandBuffer commandBuffer) {
      if (commandBuffer == null || commandBuffer.address() == NULL) {
         return;
      }
      try (MemoryStack stack = stackPush()) {
         VkSubmitInfo submitInfo = VkSubmitInfo.callocStack()
                 .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
         PointerBuffer pCommandBuffers = stack.mallocPointer(1)
                 .put(commandBuffer)
                 .flip();
         submitInfo.pCommandBuffers(pCommandBuffers);
         vkAssert(vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE));
      }
   }

   public void dispose() {
      VK10.vkDestroyCommandPool(device.logical, ptr, null);
   }

}
