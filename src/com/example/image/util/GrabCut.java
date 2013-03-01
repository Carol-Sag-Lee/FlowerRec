package com.example.image.util;

public class GrabCut {
static {
    System.loadLibrary("GrabCut");
}

/** 

   * @param width the current view width 

   * @param height the current view height 

   */ 
public static native void grabCut() {
    
}
}
