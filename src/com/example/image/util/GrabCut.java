package com.example.image.util;



public class GrabCut {
static {
    System.loadLibrary("GrabCut");
}

/** 

   * @param width the current view width 

   * @param height the current view height 
 * @return 

   */ 
public static native int[] grabCut(int[] buf,float width, float height, int preX, int preY, int x, int y) ;
}
