package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTDebugReport.VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.vkCreateDebugReportCallbackEXT;
import static org.lwjgl.vulkan.EXTDebugReport.vkDestroyDebugReportCallbackEXT;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

public class Instance {
   
   public VkInstance vulkan;
   public long debugCallback;
   
   public static String[] enabledExtensionNames = {VK_EXT_DEBUG_REPORT_EXTENSION_NAME};
   public static String[] enabledLayerNames = {"VK_LAYER_LUNARG_standard_validation"};
   
   public static Instance createInstance() {
      Instance inst = new Instance();
      inst.init();
      return inst;
   }
   
   private void init() {
      try (MemoryStack stack = stackPush()) {
         VkApplicationInfo appInfo = VkApplicationInfo.mallocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                 .pApplicationName(stack.UTF8("Java Vulkan"))
                 .pEngineName(stack.UTF8("Vulkanite"))
                 .apiVersion(VK_MAKE_VERSION(1, 1, 0));

         PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
         if (requiredExtensions == null) {
            throw new AssertionError("Failed to find list of required Vulkan extensions");
         }
         PointerBuffer ppEnabledExtensionNames = stack.mallocPointer(requiredExtensions.remaining() + enabledLayerNames.length);
         ppEnabledExtensionNames.put(requiredExtensions);
         for (String s : enabledExtensionNames) {
            ppEnabledExtensionNames.put(stack.UTF8(s));
         }
         ppEnabledExtensionNames.flip();
         
         PointerBuffer ppEnabledLayerNames = stack.mallocPointer(enabledLayerNames.length);
         for (String s : enabledLayerNames) {
            ppEnabledLayerNames.put(stack.UTF8(s));
         }
         ppEnabledLayerNames.flip();

         VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.mallocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                 .pNext(NULL)
                 .pApplicationInfo(appInfo)
                 .ppEnabledExtensionNames(ppEnabledExtensionNames)
                 .ppEnabledLayerNames(ppEnabledLayerNames);

         PointerBuffer pInstance = stack.mallocPointer(1);
         vkAssert(vkCreateInstance(pCreateInfo, null, pInstance));

         vulkan = new VkInstance(pInstance.get(0), pCreateInfo);
      }
   }

   public void setupDebugging(int flags, VkDebugReportCallbackEXT callback) {
      try (MemoryStack stack = stackPush()) {
         VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.mallocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
                 .pNext(NULL)
                 .pfnCallback(callback)
                 .pUserData(NULL)
                 .flags(flags);
         LongBuffer pCallback = stack.mallocLong(1);
         vkAssert(vkCreateDebugReportCallbackEXT(vulkan, dbgCreateInfo, null, pCallback));

         debugCallback = pCallback.get(0);
      }
   }
   
   public void dispose() {
      vkDestroyDebugReportCallbackEXT(vulkan, debugCallback, null);
      vkDestroyInstance(vulkan, null);
   }

}
