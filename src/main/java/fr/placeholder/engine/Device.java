package fr.placeholder.engine;

import static fr.placeholder.engine.Context.*;
import static fr.placeholder.engine.Utils.vkAssert;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import org.lwjgl.vulkan.*;

public class Device {

   public static void getPhysicalDevices() {
      IntBuffer pnum = memAllocInt(1);
      
      vkAssert(vkEnumeratePhysicalDevices(instance, pnum, null));
      int numDevices = pnum.get(0);
      if (numDevices <= 0) {
         throw new AssertionError("No devices were found");
      }

      PointerBuffer pdevices = memAllocPointer(pnum.get(0));

      vkAssert(vkEnumeratePhysicalDevices(instance, pnum, pdevices));

      Device[] devices = new Device[pnum.get(0)];

      //Get properties of each devices and get highest index score
      int max = 0, index = -1;
      for (int i = 0; i < numDevices; ++i) {
         VkPhysicalDevice idevice = new VkPhysicalDevice(pdevices.get(0), instance);
         devices[i] = new Device(idevice);
         int score = devices[i].getScore();
         if (score >= max) {
            max = score;
            index = i;
         }
      }
      if (index < 0) {
         throw new AssertionError("No suitable devices were found");
      }

      //Dispose all other devices that aren't used
      for (int i = 0; i < numDevices; ++i) {
         if (i != index) {
            devices[i].dispose();
         }
      }
      
      Context.device = devices[index];
      Context.device.select();
      
      memFree(pnum);
      memFree(pdevices);
   }

   public VkPhysicalDevice physical;
   public VkDevice logical;
   public VkQueue graphics, compute, transfer;

   private Device(VkPhysicalDevice d) {
      this.physical = d;
   }

   public void select() {
      try(MemoryStack stack = stackPush()) {
         
         IntBuffer pnum = stack.mallocInt(1);
         vkGetPhysicalDeviceQueueFamilyProperties(physical, pnum, null);
         VkQueueFamilyProperties.Buffer pqueueProperties = VkQueueFamilyProperties.mallocStack(pnum.get(0), stack);
         vkGetPhysicalDeviceQueueFamilyProperties(physical, pnum, pqueueProperties);
         
         /*
         Very primitive way of selecting queues; 
         */
         
         int graphicsQueueIndex = -1, computeQueueIndex = -1, transferQueueIndex = -1;
         for(int i = 0; i<pnum.get(0); i++) {
            if((pqueueProperties.get(0).queueFlags() & VK_QUEUE_GRAPHICS_BIT) == VK_QUEUE_GRAPHICS_BIT) {
               graphicsQueueIndex = i;
               break;
            }
         }
         
         for(int i = 0; i<pnum.get(0); i++) {
            if(i != graphicsQueueIndex) {
               if((pqueueProperties.get(0).queueFlags() & VK_QUEUE_COMPUTE_BIT) == VK_QUEUE_COMPUTE_BIT) {
                  computeQueueIndex = i;
                  if((pqueueProperties.get(0).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != VK_QUEUE_GRAPHICS_BIT) break;
               }
            }
         }
         
         for(int i = 0; i<pnum.get(0); i++) {
            if(i != graphicsQueueIndex && i != computeQueueIndex) {
               if((pqueueProperties.get(0).queueFlags() & VK_QUEUE_TRANSFER_BIT) == VK_QUEUE_TRANSFER_BIT) {
                  transferQueueIndex = i;
               }
            }
         }
         int queueCount = 1;
         
         FloatBuffer pQueuePriorities = stack.callocFloat(1).put(0.0f);
         pQueuePriorities.flip();
         VkDeviceQueueCreateInfo.Buffer qinfo = VkDeviceQueueCreateInfo.callocStack(queueCount,stack);
         qinfo.get(0)
                 .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                 .queueFamilyIndex(graphicsQueueIndex)
                 .pQueuePriorities(pQueuePriorities);
         if(queueCount>=2) {
            qinfo.get(1)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(computeQueueIndex)
                    .pQueuePriorities(pQueuePriorities);
         } if(queueCount>=3) {
            qinfo.get(2)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(transferQueueIndex)
                    .pQueuePriorities(pQueuePriorities);
         }
         
         // These are some features that are enabled for VkNeo
         // If you try to make an API call down the road which 
         // requires something be enabled, you'll more than likely
         // get a validation message telling you what to enable.
         // Thanks Vulkan!
         VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack);
         
         PointerBuffer ppEnabledExtensionNames = stack.mallocPointer(1);
         ppEnabledExtensionNames.put(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME));
         ppEnabledExtensionNames.flip();
         
         PointerBuffer ppEnabledLayerNames = stack.mallocPointer(enabledLayerNames.length);
         for(String s : enabledLayerNames) ppEnabledLayerNames.put(stack.UTF8(s));
         ppEnabledLayerNames.flip();
         
         // Put it all together.
         VkDeviceCreateInfo info = VkDeviceCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                 .pNext(NULL)
                 .pQueueCreateInfos(qinfo)
                 .pEnabledFeatures(deviceFeatures)
                 .ppEnabledExtensionNames(ppEnabledExtensionNames)
                 .ppEnabledLayerNames(ppEnabledLayerNames);
         

         
         // Create the device
         PointerBuffer pDevice = stack.mallocPointer(1);
         vkAssert(vkCreateDevice(physical,  info, null,  pDevice));
         logical = new VkDevice(pDevice.get(0), physical, info);
         
         // Now get the queues from the device we just created.
         PointerBuffer pQueue = stack.mallocPointer(queueCount);
         
         vkGetDeviceQueue(logical, graphicsQueueIndex, 0,  pQueue);
         graphics = new VkQueue(pQueue.get(), logical);
         
      }
   }
   
   public int getScore() {
      int score = 0;
      try(MemoryStack stack = stackPush()) {
         IntBuffer pnum = stack.mallocInt(1);
         
         // VkQueueFamilyProperties
         vkGetPhysicalDeviceQueueFamilyProperties(physical, pnum, null);

         VkQueueFamilyProperties.Buffer pqueueProperties = VkQueueFamilyProperties.mallocStack(pnum.get(0), stack);
         vkGetPhysicalDeviceQueueFamilyProperties(physical, pnum, pqueueProperties);

         // VkExtensionProperties
         vkAssert(vkEnumerateDeviceExtensionProperties(physical, (ByteBuffer) null, pnum, null));

         VkExtensionProperties.Buffer pextensionProperties = VkExtensionProperties.mallocStack(pnum.get(0), stack);
         vkAssert(vkEnumerateDeviceExtensionProperties(physical, (ByteBuffer) null, pnum, pextensionProperties));

         // VkSurfaceCapabilitiesKHR
         VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
         vkAssert(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physical, surface, surfaceCapabilities));

         // VkSurfaceFormatKHR
         vkAssert(vkGetPhysicalDeviceSurfaceFormatsKHR(physical, surface, pnum, null));

         VkSurfaceFormatKHR.Buffer psurfaceFormat = VkSurfaceFormatKHR.mallocStack(pnum.get(0), stack);
         vkAssert(vkGetPhysicalDeviceSurfaceFormatsKHR(physical, surface, pnum, psurfaceFormat));

         // SurfacePresentModesKHR (is an IntBuffer)
         vkAssert(vkGetPhysicalDeviceSurfacePresentModesKHR(physical, surface, pnum, null));

         IntBuffer psurfacePresentModes = stack.mallocInt(pnum.get(0));
         vkAssert(vkGetPhysicalDeviceSurfacePresentModesKHR(physical, surface, pnum, psurfacePresentModes));

         // VkPhysicalDeviceMemoryProperties
         VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.mallocStack(stack);
         vkGetPhysicalDeviceMemoryProperties(physical, memoryProperties);

         // VkPhysicalDeviceProperties
         VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.mallocStack(stack);
         vkGetPhysicalDeviceProperties(physical, properties);
         
         //TODO use properties to calculate if a device is not suitable (<0) or rate it's suitability (>=0)
         
         
         
         
         
         
         
         
      }
      return score;
   }

   public void dispose() {
      vkDestroyDevice(logical, null);
   }

}
