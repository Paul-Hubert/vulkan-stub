package fr.placeholder.engine;

import static fr.placeholder.engine.Context.*;
import static fr.placeholder.engine.Utils.vkAssert;
import java.nio.IntBuffer;
import org.lwjgl.PointerBuffer;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

public final class PhysicalDevice {
   
   
   //En haut parce qu'il va souvent falloir le changer plus tard 
   private int getScore() {
      
      return 0;
   }
   
   private static final IntBuffer pnum = memAllocInt(1);
   
   protected static void getPhysicalDevices() {

      vkAssert(vkEnumeratePhysicalDevices(instance, pnum, null));
      int numDevices = pnum.get(0);
      if(numDevices<=0) throw new AssertionError("No devices were found");

      PointerBuffer pdevices = memAllocPointer(pnum.get(0));

      vkAssert(vkEnumeratePhysicalDevices(instance, pnum, pdevices));

      PhysicalDevice[] devices = new PhysicalDevice[pnum.get(0)];
      
      //Get properties of each devices and get highest index score
      int max = 0, index = -1;
      for (int i = 0; i < numDevices; ++i) {
         VkPhysicalDevice idevice = new VkPhysicalDevice(pdevices.get(0), instance);
         devices[i] = new PhysicalDevice(idevice);
         devices[i].getProperties();
         int score = devices[i].getScore();
         if(score >= max) {
            max = score;
            index = i;
         }
      }
      if(index<0) throw new AssertionError("No suitable devices were found");
      
      //Dispose all other devices that aren't used
      for (int i = 0; i < numDevices; ++i) {
         if(i != index) {
            devices[i].dispose();
         }
      }
      
      Context.physicalDevice = devices[index];
      
      memFree(pdevices);
   }
   
   private final VkPhysicalDevice device;
   private VkQueueFamilyProperties.Buffer pqueueProperties;
   private VkExtensionProperties.Buffer pextensionProperties;
   private VkSurfaceCapabilitiesKHR surfaceCapabilities;
   private VkSurfaceFormatKHR.Buffer psurfaceFormat;
   private IntBuffer psurfacePresentModes;
   private VkPhysicalDeviceMemoryProperties memoryProperties;
   private VkPhysicalDeviceProperties properties;
   
   private PhysicalDevice(VkPhysicalDevice d) {
      this.device = d;
   }
   
   private void getProperties() {
      // VkQueueFamilyProperties
      vkGetPhysicalDeviceQueueFamilyProperties(device, pnum, null);

      pqueueProperties = VkQueueFamilyProperties.malloc(pnum.get(0));
      vkGetPhysicalDeviceQueueFamilyProperties(device, pnum, pqueueProperties);

      // VkExtensionProperties
      vkAssert(vkEnumerateDeviceExtensionProperties(device, pLayerName,  pnum, null));

      pextensionProperties = VkExtensionProperties.malloc(pnum.get(0));
      vkAssert(vkEnumerateDeviceExtensionProperties(device, pLayerName, pnum, pextensionProperties));


      // VkSurfaceCapabilitiesKHR
      surfaceCapabilities = VkSurfaceCapabilitiesKHR.malloc();
      vkAssert(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, surfaceCapabilities));

      // VkSurfaceFormatKHR
      vkAssert(vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface,  pnum, null));

      psurfaceFormat = VkSurfaceFormatKHR.malloc(pnum.get(0));
      vkAssert(vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface,  pnum, psurfaceFormat));

      // SurfacePresentModesKHR (is an IntBuffer)
      vkAssert(vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface,  pnum, null));

      psurfacePresentModes = memAllocInt(pnum.get(0));
      vkAssert(vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface,  pnum, psurfacePresentModes));

      // VkPhysicalDeviceMemoryProperties
      memoryProperties = VkPhysicalDeviceMemoryProperties.malloc();
      vkGetPhysicalDeviceMemoryProperties(device, memoryProperties);

      // VkPhysicalDeviceProperties
      properties = VkPhysicalDeviceProperties.malloc();
      vkGetPhysicalDeviceProperties(device,  properties);
   }
   
   public void dispose() {
      System.out.println("disposeing");
      pqueueProperties.free();
      pextensionProperties.free();
      surfaceCapabilities.free();
      psurfaceFormat.free();
      memFree(psurfacePresentModes);
      memoryProperties.free();
      properties.free();
      memFree(pnum);
   }
   
}
