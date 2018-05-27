package fr.placeholder.vulkanproject;

import static fr.placeholder.vulkanproject.Context.transfer;
import static fr.placeholder.vulkanproject.Context.win;
import static fr.placeholder.vulkanproject.Main.render;
import static fr.placeholder.vulkanproject.Main.renderer;
import static fr.placeholder.vulkanproject.Main.terrain;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class Orchestrator {
   
   public static void init() {
      for(int i = 0; i<SEM_COUNT; i++) {
         sem[i] = Synchronization.createSemaphore();
      }
   }
   
   private static int img;
   private static boolean first = true;
   
   private static final int SEM_COUNT = 4;
   private static final int acquire_render = 0, render_present = 1, render_transfer = 2, transfer_render = 3;
   private static final int[] sem = new int[SEM_COUNT];
   
   public static void run() {
      while (!glfwWindowShouldClose(win.window)) {
         
         glfwPollEvents();
         renderer.signalTo(0, sem[acquire_render]);
         terrain.update();
	 img = renderer.acquire();  // Concurrent
         
         
         if(!first) render.waitOnLast(1, sem[transfer_render]);
         render.waitOn(0, sem[acquire_render])
               .signalTo(0, sem[render_present]).signalTo(1, sem[render_transfer]);
         render.render(img);
         
         renderer.waitOn(0, sem[render_present]);
         transfer.waitOn(0, sem[render_transfer]).signalTo(0, sem[transfer_render]);
         renderer.present();
	 transfer.flush(); // Concurrent
         
         if(first) first = false;
         Synchronization.step();
         
      }
   }
   
   public static void dispose() {
      
   }
   
}
