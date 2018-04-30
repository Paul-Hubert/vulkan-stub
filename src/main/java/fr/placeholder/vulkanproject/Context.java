package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Utils.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import java.nio.LongBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import static org.lwjgl.vulkan.EXTDebugReport.*;

public class Context {

   public static VkInstance instance;
   public static long debugCallback;
   public static long surface;
   public static Device device;
   
   public static String[] enabledExtensionNames = {VK_EXT_DEBUG_REPORT_EXTENSION_NAME};
   public static String[] enabledLayerNames = {"VK_LAYER_LUNARG_standard_validation"};

   protected static void createInstance(PointerBuffer requiredExtensions) {
      try (MemoryStack stack = stackPush()) {
         VkApplicationInfo appInfo = VkApplicationInfo.mallocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                 .pApplicationName(stack.UTF8("GLFW Vulkan Demo"))
                 .pEngineName(stack.UTF8("Vulkanite"))
                 .apiVersion(VK_MAKE_VERSION(1, 1, 0));

         PointerBuffer ppEnabledExtensionNames = stack.mallocPointer(requiredExtensions.remaining() + enabledLayerNames.length);
         ppEnabledExtensionNames.put(requiredExtensions);
         for(String s : enabledExtensionNames) ppEnabledExtensionNames.put(stack.UTF8(s));
         ppEnabledExtensionNames.flip();

         PointerBuffer ppEnabledLayerNames = stack.mallocPointer(enabledLayerNames.length);
         for(String s : enabledLayerNames) ppEnabledLayerNames.put(stack.UTF8(s));
         ppEnabledLayerNames.flip();

         VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.mallocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                 .pNext(NULL)
                 .pApplicationInfo(appInfo)
                 .ppEnabledExtensionNames(ppEnabledExtensionNames)
                 .ppEnabledLayerNames(ppEnabledLayerNames);

         PointerBuffer pInstance = stack.mallocPointer(1);
         vkAssert(vkCreateInstance(pCreateInfo, null, pInstance));

         instance = new VkInstance(pInstance.get(0), pCreateInfo);
      }
   }

   protected static void setupDebugging(VkInstance instance, int flags, VkDebugReportCallbackEXT callback) {
      try (MemoryStack stack = stackPush()) {
         VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.mallocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
                 .pNext(NULL)
                 .pfnCallback(callback)
                 .pUserData(NULL)
                 .flags(flags);
         LongBuffer pCallback = stack.mallocLong(1);
         vkAssert(vkCreateDebugReportCallbackEXT(instance, dbgCreateInfo, null, pCallback));
         
         debugCallback = pCallback.get(0);
      }
   }

   protected static void dispose() {
      device.dispose();
      vkDestroyDebugReportCallbackEXT(instance, debugCallback, null);
      vkDestroyInstance(instance, null);
   }

}
