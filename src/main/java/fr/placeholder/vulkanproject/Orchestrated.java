package fr.placeholder.vulkanproject;

import java.nio.LongBuffer;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;

public abstract class Orchestrated {
   
   public LongBuffer waitSemaphores, signalSemaphores;
   private int maxW = 0, maxS = 0;
   
   public void init() {
      waitSemaphores = memAllocLong(3);
      signalSemaphores = memAllocLong(3);
      waitSemaphores.limit(maxW);
      signalSemaphores.limit(maxS);
   }
   
   public Orchestrated waitOn(int index, int semindex) {
      if(index >= maxW) {maxW = index+1; waitSemaphores.limit(maxW);}
      waitSemaphores.put(index, Synchronization.getSemaphore(semindex));
      return this;
   }
   
   public Orchestrated waitOnLast(int index, int semindex) {
      if(index >= maxW) {maxW = index+1; waitSemaphores.limit(maxW);}
      waitSemaphores.put(index, Synchronization.getLastSemaphore(semindex));
      return this;
   }
   
   public Orchestrated signalTo(int index, int semindex) {
      if(index >= maxS) {maxS = index+1; signalSemaphores.limit(maxS);}
      signalSemaphores.put(index, Synchronization.getSemaphore(semindex));
      return this;
   }
   
   public void dispose() {
      memFree(waitSemaphores);
      memFree(signalSemaphores);
   }
}
