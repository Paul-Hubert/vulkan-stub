package fr.placeholder.vulkanproject;

import java.nio.LongBuffer;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;

public abstract class Orchestrated {
   
   public LongBuffer waitSemaphores, signalSemaphores;
   
   public void init(int waits, int signals) {
      waitSemaphores = memAllocLong(waits);
      signalSemaphores = memAllocLong(signals);
   }
   
   public Orchestrated waitOn(long semaphore) {
      waitSemaphores.put(0, semaphore);
      return this;
   }
   
   public Orchestrated signalTo(long semaphore) {
      signalSemaphores.put(0, semaphore);
      return this;
   }
   
   public Orchestrated waitOn(int index, long semaphore) {
      waitSemaphores.put(index, semaphore);
      return this;
   }
   
   public Orchestrated signalTo(int index, long semaphore) {
      signalSemaphores.put(index, semaphore);
      return this;
   }
   
   public void dispose() {
      memFree(waitSemaphores);
      memFree(signalSemaphores);
   }
}
