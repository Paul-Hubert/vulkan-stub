package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.render;
import static fr.placeholder.vulkanproject.Context.win;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import org.lwjgl.vulkan.VK10;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_A;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_B;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_G;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_R;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

public class SwapChain {

   public static SwapChain createSwapChain() {
      SwapChain swapchain = new SwapChain();
      swapchain.init();
      return swapchain;
   }
   
   public static int NUM_FRAMES = 2;
   
   public long chain;
   public long[] images = new long[NUM_FRAMES], imageViews = new long[NUM_FRAMES];
   public VkSurfaceFormatKHR surfaceFormat;
   public int presentMode;
   public VkExtent2D extent;
   
   public long[] framebuffers;
   
   /*
   =============
   ChooseSurfaceFormat
   =============
   */
   VkSurfaceFormatKHR chooseSurfaceFormat() {
      VkSurfaceFormatKHR.Buffer formats = device.psurfaceFormat;

      // Favor 32 bit rgba and srgb nonlinear colorspace
      for (int i = 0; i < formats.capacity(); i++) {
         VkSurfaceFormatKHR fmt = formats.get(i);
         if (fmt.format() == VK_FORMAT_B8G8R8A8_UNORM && fmt.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
            return fmt;
         }
      }

      return formats.get(0);
   }

   /*
   =============
   ChoosePresentMode
   =============
   */
   int choosePresentMode() {
      int desiredMode = VK_PRESENT_MODE_MAILBOX_KHR;
      
      IntBuffer modes = device.psurfacePresentModes;
      
      // Favor looking for mailbox mode.
      for (int i = 0; i < modes.capacity(); i++) {
         if (modes.get(i) == desiredMode) {
            return desiredMode;
         }
      }

      // If we couldn't find mailbox, then default to FIFO which is always available.
      return VK_PRESENT_MODE_FIFO_KHR;
   }

   /*
   =============
   ChooseSurfaceExtent
   =============
   */
   VkExtent2D chooseSurfaceExtent() {
      VkExtent2D extent = device.surfaceCapabilities.currentExtent();

      // The extent is typically the size of the window we created the surface from.
      // However if Vulkan returns -1 then simply substitute the window size.
      if (extent.width() == -1) {
         extent.width(win.getWidth());
         extent.height(win.getWidth());
      }

      return extent;
   }

   /*
   =============
   CreateSwapChain
   =============
   */
   private void init() {
      try(MemoryStack stack = stackPush()) {
         // Take our selected gpu and pick three things.
         // 1.) Surface format as described earlier.
         // 2.) Present mode. Again refer to documentation I shared.
         // 3.) Surface extent is basically just the size ( width, height ) of the image.
         surfaceFormat = chooseSurfaceFormat();
         presentMode = choosePresentMode();
         extent = chooseSurfaceExtent();
         
         VkSwapchainCreateInfoKHR info = VkSwapchainCreateInfoKHR.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                 .surface(win.surface)
                 .minImageCount(NUM_FRAMES)
                 .imageFormat(surfaceFormat.format())
                 .imageColorSpace(surfaceFormat.colorSpace())
                 .imageExtent(extent)
                 .imageArrayLayers(1) // VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT - This is a color image I'm rendering into.
                 .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT) // VK_IMAGE_USAGE_TRANSFER_SRC_BIT - I'll be copying this image somewhere. ( screenshot, postprocess )
                 .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                 .preTransform(VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) // We just want to leave the image as is.
                 .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                 .presentMode(presentMode)
                 .clipped(true); // Is Vulkan allowed to discard operations outside of the renderable space?
         
         
         LongBuffer swapchain = stack.mallocLong(1);
         // Create the swapchain
         vkAssert(vkCreateSwapchainKHR(device.logical,  info, null, swapchain));
         chain = swapchain.get(0);
         
         // Retrieve the swapchain images from the device.
         IntBuffer pnum = stack.mallocInt(1);
         vkAssert(vkGetSwapchainImagesKHR(device.logical, chain, pnum, null));
         LongBuffer pimages = stack.callocLong(NUM_FRAMES);
         vkAssert(vkGetSwapchainImagesKHR(device.logical, chain, pnum, pimages));
         images[0] = pimages.get(0);
         images[1] = pimages.get(1);
         
         // New concept - Image Views
         // Much like the logical device is an interface to the physical device,
         // image views are interfaces to actual images.  Think of it as this.
         // The image exists outside of you.  But the view is your personal view 
         // ( how you perceive ) the image.
         for (int i = 0; i < NUM_FRAMES; i++) {
            VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.callocStack(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(images[i])
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(surfaceFormat.format())
                    .flags(0);
            
            imageViewCreateInfo.components()
                    .r(VK_COMPONENT_SWIZZLE_R)
                    .g(VK_COMPONENT_SWIZZLE_G)
                    .b(VK_COMPONENT_SWIZZLE_B)
                    .a(VK_COMPONENT_SWIZZLE_A);

            // There are only 4x aspect bits.  And most people will only use 3x.
            // These determine what is affected by your image operations.
            // VK_IMAGE_ASPECT_COLOR_BIT
            // VK_IMAGE_ASPECT_DEPTH_BIT
            // VK_IMAGE_ASPECT_STENCIL_BIT
            imageViewCreateInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);

            // Create the view
            LongBuffer imageView = stack.mallocLong(1);
            vkAssert(vkCreateImageView(device.logical,  imageViewCreateInfo, null,  imageView));
            imageViews[i] = imageView.get(0);
         }
      }
   }
   
   public void createFrameBuffers() {
      try(MemoryStack stack = stackPush()) {
        LongBuffer attachments = stack.mallocLong(1);
        VkFramebufferCreateInfo fci = VkFramebufferCreateInfo.callocStack()
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .pAttachments(attachments)
                .flags(0)
                .height(extent.height())
                .width(extent.width())
                .layers(1)
                .pNext(NULL)
                .renderPass(render.pass);
        // Create a framebuffer for each swapchain image
        framebuffers = new long[NUM_FRAMES];
        LongBuffer pFramebuffer = stack.mallocLong(1);
        for (int i = 0; i < NUM_FRAMES; i++) {
            attachments.put(0, imageViews[i]);
            vkAssert(vkCreateFramebuffer(device.logical, fci, null, pFramebuffer));
            long framebuffer = pFramebuffer.get(0);
            framebuffers[i] = framebuffer;
        }
      }
    }
   
   public void dispose() {
      vkDestroyFramebuffer(device.logical, framebuffers[0], null);
      vkDestroyFramebuffer(device.logical, framebuffers[1], null);
      vkDestroyImageView(device.logical, imageViews[0], null);
      vkDestroyImageView(device.logical, imageViews[1], null);
      vkDestroySwapchainKHR(device.logical, chain, null);
   }
  
}
