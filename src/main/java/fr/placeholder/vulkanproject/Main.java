package fr.placeholder.vulkanproject;

public class Main {
   
   public static void main(String[] args) {
      Context.init();
      TriangleLoop.loop();
      Context.dispose();
   }
   
}
