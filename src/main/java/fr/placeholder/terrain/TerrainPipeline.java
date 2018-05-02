package fr.placeholder.terrain;

import fr.placeholder.vulkanproject.Utils;
import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.swap;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.vulkan.VK10;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_BACK_BIT;
import static org.lwjgl.vulkan.VK10.VK_LOGIC_OP_COPY;
import static org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

public class TerrainPipeline {
   
   public long line;
   public long layout;

   public void init(long renderpass, int subpass) {
      long vertexShader = Utils.loadShader("src/main/resources/vert.spv"), fragmentShader = Utils.loadShader("src/main/resources/frag.spv");
      
      try(MemoryStack stack = stackPush()) {
      
         VkPipelineShaderStageCreateInfo.Buffer shaderStageInfo = VkPipelineShaderStageCreateInfo.callocStack(2, stack);
         shaderStageInfo.get(0)
                 .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                 .stage(VK_SHADER_STAGE_VERTEX_BIT)
                 .module(vertexShader)
                 .pName(stack.UTF8("main"));
         shaderStageInfo.get(1)
                 .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                 .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                 .module(fragmentShader)
                 .pName(stack.UTF8("main"));
         
         VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                 .pVertexBindingDescriptions(null)
                 .pVertexAttributeDescriptions(null);
         
         VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                 .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                 .primitiveRestartEnable(false);
         
         VkViewport.Buffer viewport = VkViewport.callocStack(1,stack)
                 .x(0.0f)
                 .y(0.0f)
                 .width((float) swap.extent.width())
                 .height((float) swap.extent.height())
                 .minDepth(0.0f)
                 .maxDepth(1.0f);
         
         VkRect2D.Buffer scissor = VkRect2D.callocStack(1,stack)
                 .offset(VkOffset2D.callocStack(stack).set(0, 0))
                 .extent(swap.extent);
         
         VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                 .pViewports(viewport)
                 .pScissors(scissor);
         
         VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                 .depthClampEnable(false)
                 .rasterizerDiscardEnable(false)
                 .polygonMode(VK_POLYGON_MODE_FILL)
                 .lineWidth(1.0f)
                 .cullMode(VK_CULL_MODE_BACK_BIT)
                 .frontFace(VK10.VK_FRONT_FACE_CLOCKWISE)
                 .depthBiasEnable(false);
         
         VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                 .sampleShadingEnable(false)
                 .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                 .minSampleShading(1.0f)
                 .pSampleMask(null)
                 .alphaToCoverageEnable(false)
                 .alphaToOneEnable(false);
         
         VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.callocStack(1, stack)
                 .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                 .blendEnable(false);
         
         VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                 .logicOpEnable(false)
                 .logicOp(VK_LOGIC_OP_COPY)
                 .pAttachments(colorBlendAttachment);
         
         VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                 .pSetLayouts(null)
                 .pPushConstantRanges(null);
         
         LongBuffer playout = stack.mallocLong(1);
         vkAssert(vkCreatePipelineLayout(device.logical, pipelineLayoutInfo, null, playout));
         layout = playout.get(0);
         
         VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.callocStack(1, stack)
                 .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                 .pStages(shaderStageInfo)
                 .pVertexInputState(vertexInputInfo)
                 .pInputAssemblyState(inputAssembly)
                 .pViewportState(viewportState)
                 .pRasterizationState(rasterizer)
                 .pMultisampleState(multisampling)
                 .pDepthStencilState(null)
                 .pColorBlendState(colorBlending)
                 .pDynamicState(null)
                 .layout(layout)
                 .renderPass(renderpass)
                 .subpass(subpass)
                 .basePipelineHandle(NULL)
                 .basePipelineIndex(-1);
         
         LongBuffer graphicsPipeline = stack.mallocLong(1);
         vkAssert(VK10.vkCreateGraphicsPipelines(device.logical, NULL, pipelineInfo, null, graphicsPipeline));
         line = graphicsPipeline.get(0);
      }
      
      vkDestroyShaderModule(device.logical, fragmentShader, null);
      vkDestroyShaderModule(device.logical, vertexShader, null);
      vkDestroyPipelineLayout(device.logical, layout, null);
   }

   public void dispose() {
      vkDestroyPipeline(device.logical, line, null);
   }

}
