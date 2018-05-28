/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.placeholder.vulkanproject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author paul.boursin
 */
public class Threading {
   
   private static final ExecutorService service = Executors.newCachedThreadPool();
   
   public static void dispatch(Runnable r) {
      service.submit(r);
   }
   
}
