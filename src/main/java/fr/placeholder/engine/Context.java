package fr.placeholder.engine;


import static fr.placeholder.engine.Utils.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.VK10.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;

import static org.lwjgl.vulkan.EXTDebugReport.*;

public class Context {
   
   protected static VkInstance instance;
   
   private static boolean validation = Boolean.parseBoolean(System.getProperty("vulkan.validation", "true"));
   
   protected static void createInstance(PointerBuffer requiredExtensions) {
      VkApplicationInfo appInfo = VkApplicationInfo.calloc()
              .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
              .pApplicationName(memUTF8("GLFW Vulkan Demo"))
              .pEngineName(memUTF8(""))
              .apiVersion(VK_MAKE_VERSION(1, 1, 0));
      
      PointerBuffer ppEnabledExtensionNames = memAllocPointer(requiredExtensions.remaining() + 1);
      ppEnabledExtensionNames.put(requiredExtensions);
      ByteBuffer VK_EXT_DEBUG_REPORT_EXTENSION = memUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
      ppEnabledExtensionNames.put(VK_EXT_DEBUG_REPORT_EXTENSION);
      ppEnabledExtensionNames.flip();
      
      PointerBuffer ppEnabledLayerNames = memAllocPointer(1);
      ppEnabledLayerNames.put(memUTF8("VK_LAYER_LUNARG_standard_validation"));
      ppEnabledLayerNames.flip();
      
      VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
         .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
         .pNext(NULL)
         .pApplicationInfo(appInfo)
         .ppEnabledExtensionNames(ppEnabledExtensionNames)
         .ppEnabledLayerNames(ppEnabledLayerNames);
      
      PointerBuffer pInstance = memAllocPointer(1);
      vkAssert(vkCreateInstance(pCreateInfo, null, pInstance));
      long inst = pInstance.get(0);
      memFree(pInstance);
      
      instance = new VkInstance(inst, pCreateInfo);
      pCreateInfo.free();
      memFree(ppEnabledLayerNames);
      memFree(VK_EXT_DEBUG_REPORT_EXTENSION);
      memFree(ppEnabledExtensionNames);
      memFree(appInfo.pApplicationName());
      memFree(appInfo.pEngineName());
      appInfo.free();
   }
   
   protected static long setupDebugging(VkInstance instance, int flags, VkDebugReportCallbackEXT callback) {
      VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
              .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
              .pNext(NULL)
              .pfnCallback(callback)
              .pUserData(NULL)
              .flags(flags);
      LongBuffer pCallback = memAllocLong(1);
      vkAssert(vkCreateDebugReportCallbackEXT(instance, dbgCreateInfo, null, pCallback));
      long callbackHandle = pCallback.get(0);
      memFree(pCallback);
      dbgCreateInfo.free();
      
      return callbackHandle;
   }

   protected static void dispose() {
      
   }
   
}
