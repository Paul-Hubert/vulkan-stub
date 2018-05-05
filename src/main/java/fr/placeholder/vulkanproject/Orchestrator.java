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
         LongBuffer psemaphores = stack.mallocLong(NUM_FRAMES * semaphoreCount);
         
         vkAssert(vkCreateSemaphore(device.logical, semaphoreCreateInfo, null, psemaphores));
         for(int i = 0; i<NUM_FRAMES; i++) {
            imageAcquired[i] = psemaphores.get(i+NUM_FRAMES*0);
            renderComplete[i] = psemaphores.get(i+NUM_FRAMES*1);
            renderComplete[i+NUM_FRAMES] = psemaphores.get(i+NUM_FRAMES*2);
            transferComplete[i] = psemaphores.get(i+NUM_FRAMES*3);
         }
         
         terrain.init(0, 0);
         render.init(2,2);
         renderer.init(1,1);
         transfer.init(1,1);
      }
   }
   
   public static int index;
   private static int sem = 0, frame = 0;
   
   private static int semaphoreCount = 4;
   private static final long[] imageAcquired = new long[NUM_FRAMES],
           renderComplete = new long[NUM_FRAMES * 2],
           transferComplete = new long[NUM_FRAMES];
           
   
   public static void run() {
      while (!glfwWindowShouldClose(win.window)) {
         
         glfwPollEvents();
         renderer.signalSemaphores.put(0, imageAcquired[sem]);
         
         terrain.update(); index = renderer.acquire();  // Concurrent
         
         
         render.waitOn(imageAcquired[sem])
               .signalTo(renderComplete[sem]).signalTo(1, renderComplete[sem + NUM_FRAMES]);
         loop(render, 1, transferComplete);
         render.render(index);
         
         renderer.waitOn(renderComplete[sem]);
         transfer.waitOn(renderComplete[sem + NUM_FRAMES]).signalTo(transferComplete[sem]);
         renderer.present();transfer.flush(); // Concurrent
         
         sem = (sem + 1)%NUM_FRAMES;
         frame++;
      }
   }
   
   private static void loop(Orchestrated orc, int place, long[] sems) {
      if(frame == 0) {
         orc.waitSemaphores.limit(place);
         return;
      }
      orc.waitSemaphores.clear();
      orc.waitOn(place, sems[(index-1+NUM_FRAMES)%NUM_FRAMES]);
   }
   
   
}
