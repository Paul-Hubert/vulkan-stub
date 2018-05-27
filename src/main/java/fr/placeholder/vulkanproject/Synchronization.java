/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import java.util.ArrayList;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import org.lwjgl.vulkan.VK10;
import static org.lwjgl.vulkan.VK10.VK_FENCE_CREATE_SIGNALED_BIT;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateFence;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

/**
 *
 * @author PaulHubert
 */
public class Synchronization {
   
   public static void init() {
      semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc()
              .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
              .pNext(VK_NULL_HANDLE)
              .flags(0);
      fenceCreateInfo = VkFenceCreateInfo.calloc()
              .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
              .pNext(VK_NULL_HANDLE)
              .flags(0);
      p = memAllocLong(1);
   }
   
   private static LongBuffer p;
   
   private static VkSemaphoreCreateInfo semaphoreCreateInfo;
   private static ArrayList<Long> semaphores = new ArrayList<>();
   
   private static VkFenceCreateInfo fenceCreateInfo;
   private static ArrayList<Long> fences = new ArrayList<>();
   
   private static int frameIndex = 0;
   
   public static int createSemaphore() {
      for(int i = 0; i<SwapChain.NUM_FRAMES; i++) {
         vkAssert(vkCreateSemaphore(device.logical, semaphoreCreateInfo, null, p));
         semaphores.add(p.get(0));
      }
      return semaphores.size()/SwapChain.NUM_FRAMES-1;
   }
   
   public static int createFence(boolean signaled) {
      fenceCreateInfo.flags(signaled ? VK_FENCE_CREATE_SIGNALED_BIT : 0);
      for(int i = 0; i<SwapChain.NUM_FRAMES; i++) {
         vkAssert(vkCreateFence(device.logical, fenceCreateInfo, null, p));
         fences.add(p.get(0));
      }
      return fences.size()/SwapChain.NUM_FRAMES-1;
   }
   
   public static long getSemaphore(int index) {
      return semaphores.get(index*2+frameIndex);
   }
   
   public static long getFence(int index) {
      return fences.get(index*2+frameIndex);
   }
   
   public static long getLastSemaphore(int index) {
      return semaphores.get(index*2+(frameIndex-1+SwapChain.NUM_FRAMES)%SwapChain.NUM_FRAMES);
   }
   
   public static long getLastFence(int index) {
      return fences.get(index*2+(frameIndex-1+SwapChain.NUM_FRAMES)%SwapChain.NUM_FRAMES);
   }
   
   public static void step() {
      frameIndex = (frameIndex+1)%SwapChain.NUM_FRAMES;
   }
   
   public static void dispose() {
      semaphoreCreateInfo.free();
      fenceCreateInfo.free();
      memFree(p);
      for(Long semaphore : semaphores) {
         VK10.vkDestroySemaphore(device.logical, semaphore, null);
      }
      for(Long fence : fences) {
         VK10.vkDestroyFence(device.logical, fence, null);
      }
   }
   
   
}
