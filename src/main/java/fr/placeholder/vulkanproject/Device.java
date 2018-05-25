package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.instance;
import static fr.placeholder.vulkanproject.Context.win;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import org.lwjgl.vulkan.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;

public class Device {

   public static Device getDevice() {
      IntBuffer pnum = memAllocInt(1);
      
      vkAssert(vkEnumeratePhysicalDevices(instance.vulkan, pnum, null));
      int numDevices = pnum.get(0);
      if (numDevices <= 0) {
         throw new AssertionError("No devices were found");
      }

      PointerBuffer pdevices = memAllocPointer(pnum.get(0));

      vkAssert(vkEnumeratePhysicalDevices(instance.vulkan, pnum, pdevices));

      Device[] devices = new Device[pnum.get(0)];

      //Get properties of each devices and get highest index score
      int max = 0, index = -1;
      for (int i = 0; i < numDevices; ++i) {
         VkPhysicalDevice idevice = new VkPhysicalDevice(pdevices.get(0), instance.vulkan);
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
      
      devices[index].select();
      
      memFree(pnum);
      memFree(pdevices);
      
      return devices[index];
   }

   public VkPhysicalDevice physical;
   public VkDevice logical;
   public VkQueue graphics, compute, transfer;
   public int graphicsI = -1, computeI = -1, transferI = -1;

   private Device(VkPhysicalDevice d) {
      this.physical = d;
   }
   
   public void select() {
      getProperties();
      
      try(MemoryStack stack = stackPush()) {
         
         /*
         Very primitive way of selecting queues; 
         */
         
         int qfc = pqueueProperties.capacity();
         
         boolean[][] indices = new boolean[qfc][];
         
         for(int i = 0; i<qfc; i++) {
            indices[i] = new boolean[pqueueProperties.get(i).queueCount()];
         }
         
         int graphicsJ = 0;
         for(int i = 0; i<qfc; i++) {
            if((pqueueProperties.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) == VK_QUEUE_GRAPHICS_BIT) {
               graphicsI = i;
               indices[graphicsI][graphicsJ] = true;
               break;
            }
         }
         
         int computeJ = -1;
         for(int i = 0; i<qfc; i++) {
            if((pqueueProperties.get(i).queueFlags() & VK_QUEUE_COMPUTE_BIT) == VK_QUEUE_COMPUTE_BIT) {
               for(int j = 0; j<indices[i].length; j++) {
                  if(!indices[i][j]) {
                     if(computeI>0&&computeJ>0) indices[computeI][computeJ] = false;
                     computeI = i; computeJ = j;
                     indices[computeI][computeJ] = true;
                     break;
                  }
               }
            }
         }
         
         int transferJ = -1;
         for(int i = 0; i<qfc; i++) {
            if((pqueueProperties.get(i).queueFlags() & VK_QUEUE_TRANSFER_BIT) == VK_QUEUE_TRANSFER_BIT) {
               for(int j = 0; j<indices[i].length; j++) {
                  if(!indices[i][j]) {
                     if(transferI>0&&transferJ>0) indices[transferI][transferJ] = false;
                     transferI = i; transferJ = j;
                     indices[transferI][transferJ] = true;
                     break;
                  }
               }
            }
         }
         
         int count = 1 + (graphicsI!=computeI&&graphicsI!=transferI?1:0) + (computeI!=transferI? 1 : 0);
         int[] countQ = {graphicsI, graphicsI!=computeI?computeI:-1, transferI!=computeI && graphicsI!=transferI ? transferI:-1};
         
         FloatBuffer pQueuePriorities = stack.callocFloat(1).put(0.0f);
         pQueuePriorities.flip();
         VkDeviceQueueCreateInfo.Buffer qinfo = VkDeviceQueueCreateInfo.callocStack(count, stack);
         for(int i = 0; i<count; i++) {
            qinfo.get(i)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(countQ[i])
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
         
         PointerBuffer ppEnabledLayerNames = stack.mallocPointer(Instance.enabledLayerNames.length);
         for(String s : Instance.enabledLayerNames) ppEnabledLayerNames.put(stack.UTF8(s));
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
         PointerBuffer pQueue = stack.mallocPointer(1);
         
         vkGetDeviceQueue(logical, graphicsI, graphicsJ,  pQueue);
         graphics = new VkQueue(pQueue.get(0), logical);
         vkGetDeviceQueue(logical, computeI, computeJ,  pQueue);
         compute = new VkQueue(pQueue.get(0), logical);
         vkGetDeviceQueue(logical, transferI, transferJ,  pQueue);
         transfer = new VkQueue(pQueue.get(0), logical);
         
      }
   }
   
   public int getScore() {
      int score = 0;
      try(MemoryStack stack = stackPush()) {
         IntBuffer pnum = stack.mallocInt(1);
         
         // VkQueueFamilyProperties
         vkGetPhysicalDeviceQueueFamilyProperties(physical, pnum, null);

         pqueueProperties = VkQueueFamilyProperties.mallocStack(pnum.get(0), stack);
         vkGetPhysicalDeviceQueueFamilyProperties(physical, pnum, pqueueProperties);
         
         //TODO use properties to calculate if a device is not suitable (<0) or rate it's suitability (>=0)
         
         int graphicsQueueIndex = -1, computeQueueIndex = -1, transferQueueIndex = -1;
         for(int i = 0; i<pqueueProperties.capacity(); i++) {
            if((pqueueProperties.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) == VK_QUEUE_GRAPHICS_BIT) {
               graphicsQueueIndex = i;
               continue;
            } if((pqueueProperties.get(i).queueFlags() & VK_QUEUE_COMPUTE_BIT) == VK_QUEUE_COMPUTE_BIT) {
               computeQueueIndex = i;
               continue;
            } if((pqueueProperties.get(i).queueFlags() & VK_QUEUE_TRANSFER_BIT) == VK_QUEUE_TRANSFER_BIT) {
               transferQueueIndex = i;
            }
         }
         
         if(graphicsQueueIndex<0 || computeQueueIndex<0 || transferQueueIndex<0) return -1;
         
         IntBuffer pSupported = stack.mallocInt(1);
         vkAssert(vkGetPhysicalDeviceSurfaceSupportKHR(physical, graphicsQueueIndex, win.surface, pSupported));
         if(pSupported.get(0) == VK_FALSE) return -1;
         
      }
      return score;
   }
   
   public VkQueueFamilyProperties.Buffer pqueueProperties;
   public VkExtensionProperties.Buffer pextensionProperties;
   public VkSurfaceCapabilitiesKHR surfaceCapabilities;
   public VkSurfaceFormatKHR.Buffer psurfaceFormat;
   public IntBuffer psurfacePresentModes;
   public VkPhysicalDeviceMemoryProperties memoryProperties;
   public VkPhysicalDeviceProperties properties;
   
   private void getProperties() {
      IntBuffer pnum = memAllocInt(1);
      
      // VkQueueFamilyProperties
      vkGetPhysicalDeviceQueueFamilyProperties(physical, pnum, null);

      pqueueProperties = VkQueueFamilyProperties.malloc(pnum.get(0));
      vkGetPhysicalDeviceQueueFamilyProperties(physical, pnum, pqueueProperties);

      // VkExtensionProperties
      vkAssert(vkEnumerateDeviceExtensionProperties(physical, (ByteBuffer) null, pnum, null));

      pextensionProperties = VkExtensionProperties.malloc(pnum.get(0));
      vkAssert(vkEnumerateDeviceExtensionProperties(physical, (ByteBuffer) null, pnum, pextensionProperties));

      // VkSurfaceCapabilitiesKHR
      surfaceCapabilities = VkSurfaceCapabilitiesKHR.malloc();
      vkAssert(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physical, win.surface, surfaceCapabilities));

      // VkSurfaceFormatKHR
      vkAssert(vkGetPhysicalDeviceSurfaceFormatsKHR(physical, win.surface, pnum, null));
      
      psurfaceFormat = VkSurfaceFormatKHR.malloc(pnum.get(0));
      vkAssert(vkGetPhysicalDeviceSurfaceFormatsKHR(physical, win.surface, pnum, psurfaceFormat));

      // SurfacePresentModesKHR (is an IntBuffer)
      vkAssert(vkGetPhysicalDeviceSurfacePresentModesKHR(physical, win.surface, pnum, null));
      
      psurfacePresentModes = memAllocInt(pnum.get(0));
      vkAssert(vkGetPhysicalDeviceSurfacePresentModesKHR(physical, win.surface, pnum, psurfacePresentModes));

      // VkPhysicalDeviceMemoryProperties
      memoryProperties = VkPhysicalDeviceMemoryProperties.malloc();
      vkGetPhysicalDeviceMemoryProperties(physical, memoryProperties);

      // VkPhysicalDeviceProperties
      properties = VkPhysicalDeviceProperties.malloc();
      vkGetPhysicalDeviceProperties(physical, properties);
   }

   public void dispose() {
      pqueueProperties.free();
      pextensionProperties.free();
      surfaceCapabilities.free();
      psurfaceFormat.free();
      memFree(psurfacePresentModes);
      memoryProperties.free();
      properties.free();
      
      vkDestroyDevice(logical, null);
   }

}
