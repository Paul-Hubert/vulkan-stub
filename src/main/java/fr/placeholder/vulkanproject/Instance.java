package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTDebugReport.vkCreateDebugReportCallbackEXT;
import static org.lwjgl.vulkan.EXTDebugReport.vkDestroyDebugReportCallbackEXT;
import org.lwjgl.vulkan.EXTDebugUtils;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

public class Instance {
   
   public VkInstance vulkan;
   private long debugCallback;
   private VkDebugUtilsMessengerCallbackEXT callback;
   
   public static String[] enabledExtensionNames = {VK_EXT_DEBUG_REPORT_EXTENSION_NAME};
   public static String[] enabledLayerNames = {"VK_LAYER_LUNARG_standard_validation"};
   
   public static Instance createInstance() {
      Instance inst = new Instance();
      inst.init();
      return inst;
   }
   
   private void init() {
      try (MemoryStack stack = stackPush()) {
         VkApplicationInfo appInfo = VkApplicationInfo.callocStack(stack)
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
            System.out.println(s);
         }
         ppEnabledExtensionNames.flip();
         
         PointerBuffer ppEnabledLayerNames = stack.mallocPointer(enabledLayerNames.length);
         for (String s : enabledLayerNames) {
            ppEnabledLayerNames.put(stack.UTF8(s));
         }
         ppEnabledLayerNames.flip();

         VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                 .pNext(NULL)
                 .pApplicationInfo(appInfo)
                 .ppEnabledExtensionNames(ppEnabledExtensionNames)
                 .ppEnabledLayerNames(ppEnabledLayerNames);

         PointerBuffer pInstance = stack.mallocPointer(1);
         vkAssert(vkCreateInstance(pCreateInfo, null, pInstance));

         vulkan = new VkInstance(pInstance.get(0), pCreateInfo);
         
         /*
         IntBuffer pnum = stack.callocInt(1);
         VK10.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, pnum, null);
         
         VkExtensionProperties.Buffer pProps = VkExtensionProperties.callocStack(pnum.get(0), stack);
         VK10.vkEnumerateInstanceExtensionProperties((ByteBuffer) null, pnum, pProps);
         
         for(int i = 0; i<pnum.get(0); i++) {
            System.out.println(pProps.get(i).extensionNameString());
         }
         */
         
         setupDebugging(new VkDebugUtilsMessengerCallbackEXT() {
            @Override
            public int invoke(int messageSeverity, int messageType, long pCallbackData, long pUserData) {
               System.err.println("ERROR OCCURED: " + VkDebugUtilsMessengerCallbackDataEXT.npMessage(pCallbackData));
               return 0;
            }
         });
      }
   }

   public void setupDebugging(VkDebugUtilsMessengerCallbackEXT callback) {
      try (MemoryStack stack = stackPush()) {
         this.callback = callback;
         VkDebugUtilsMessengerCreateInfoEXT dbgCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack)
                 .sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                 .pNext(NULL)
                 .pfnUserCallback(callback)
                 .pUserData(NULL)
                 .flags(0x11111111);
         LongBuffer pCallback = stack.mallocLong(1);
         vkAssert(EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(vulkan, dbgCreateInfo, null, pCallback));
         debugCallback = pCallback.get(0);
      }
   }
   
   public void dispose() {
      callback.free();
      vkDestroyDebugUtilsMessengerEXT(vulkan, debugCallback, null);
      vkDestroyInstance(vulkan, null);
   }

}
