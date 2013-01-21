//package com.example.image.util;
//
//import java.util.ArrayList;
//
//public class CObjCut {
//    private
//        byte  m_pImageData, m_pDrawingMask;
//        int m_nWidth, m_nHeight, m_nImageStride, m_nMaskStride;
//        float m_nodeModifier, m_MaxNodeModifier, m_edgeModifier;
//        // color models
//        ArrayList<Integer> m_nGMMAssign = new ArrayList<Integer>(); 
//        OCGMM m_foreGMM, m_backGMM;
//
//        // weights on edges
//        ArrayList<Double> m_leftW, m_upleftW, m_upW, m_uprightW;
//
//        // max flow solver
//        typedef vis::GridGraph8 GraphType;
//        GraphType *m_pMaxflow;
//
//}
//
//class CBoundingBox
//{
//public
//    int x1;
//    int y1;
//    int x2;
//    int y2;
//    int width;
//    int height;
//
//    CBoundingBox(int leftUpX, int leftUpY, int rightDownX, int rightDownY)
//    {
//        x1 = leftUpX;
//        y1 = leftUpY;
//        x2 = rightDownX;
//        y2 = rightDownY;
//        width = x2 - x1;
//        height = y2 - y1;
//    }
//}