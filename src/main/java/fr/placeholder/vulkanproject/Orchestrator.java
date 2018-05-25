package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.device;
import static fr.placeholder.vulkanproject.Context.transfer;
import static fr.placeholder.vulkanproject.Context.win;
import static fr.placeholder.vulkanproject.Main.render;
import static fr.placeholder.vulkanproject.Main.renderer;
import static fr.placeholder.vulkanproject.Main.terrain;
import static fr.placeholder.vulkanproject.SwapChain.NUM_FRAMES;
import static fr.placeholder.vulkanproject.Utils.vkAssert;
import java.nio.LongBuffer;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

public class Orchestrator {
   
   public static void init() {
      try(MemoryStack stack = stackPush()) {
         VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.callocStack(stack)
                 .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                 .pNext(VK_NULL_HANDLE)
                 .flags(0);
         LongBuffer psemaphores = stack.mallocLong(1);
	 for(int i = 0; i<SEM_COUNT; i++) {
	    vkAssert(vkCreateSemaphore(device.logical, semaphoreCreateInfo, null, psemaphores));
	    sem[i][0] = psemaphores.get(0);
	    vkAssert(vkCreateSemaphore(device.logical, semaphoreCreateInfo, null, psemaphores));
	    sem[i][1] = psemaphores.get(0);
	 }
         
      }
   }
   
   public static int img;
   private static int index = 0, frame = 0;
   
   private static final int SEM_COUNT = 4;
   private static final int acquire_render = 0, render_present = 1, render_transfer = 2, transfer_render = 3;
   private static final long[][] sem = new long[SEM_COUNT][NUM_FRAMES];
           
   
   public static void run() {
      while (!glfwWindowShouldClose(win.window)) {
         
         glfwPollEvents();
         renderer.signalTo(sem[acquire_render][index]);
         terrain.update();
	 index = renderer.acquire();  // Concurrent
         
         
         render.waitOn(sem[acquire_render][index])
               .signalTo(1, sem[render_present][index]).signalTo(0, sem[render_transfer][index]);
         loop(render, 1, transfer_render);
	 
         render.render(index);
         
         renderer.waitOn(sem[render_present][index]);
         transfer.waitOn(sem[render_transfer][index]).signalTo(sem[transfer_render][index]);
         renderer.present();
	 transfer.flush(); // Concurrent
         
         index = (index + 1)%NUM_FRAMES;
         frame++;
      }
   }
   
   private static void loop(Orchestrated orc, int place, int i) {
      if(frame > 0) {
	 orc.waitSemaphores.clear();
	 orc.waitOn(place, sem[i][(index-1+NUM_FRAMES)%NUM_FRAMES]);
      }
   }
   
   
}
