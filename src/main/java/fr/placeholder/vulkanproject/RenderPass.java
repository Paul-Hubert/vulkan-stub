package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.swap;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDescription;

public class RenderPass {

   public static RenderPass createRenderPass() {
      RenderPass pass = new RenderPass();
      pass.init();
      return pass;
   }
   
   public long pass;
   
   private void init() {
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
         
         VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attachments)
                .pSubpasses(subpass)
                .pDependencies(null);
         
         LongBuffer pRenderPass = stack.mallocLong(1);
         vkAssert(vkCreateRenderPass(device.logical, renderPassInfo, null, pRenderPass));
         pass = pRenderPass.get(0);
         
      }
   }
   
   public void dispose() {
      vkDestroyRenderPass(device.logical, pass, null);
   }

}
