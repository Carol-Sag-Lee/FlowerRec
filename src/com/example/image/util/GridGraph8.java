package com.example.image.util;

import java.util.ArrayList;
import java.util.Queue;
import java.util.Vector;

public class GridGraph8 {
    
    //MaxflowGraph.h定义数据
    final int[] gridGraph8XShifts = { 1, 1, 0, -1, -1, -1,  0,  1 };
    final int[] gridGraph8YShifts = { 0, 1, 1,  1,  0, -1, -1, -1 };
    
    static final int NeighborhoodSize = 8;
    
   class Node{
       short[] m_nCap = new short[NeighborhoodSize];  
       short    m_tCap; // if m_tCap > 0, then residual capacity of edge source->node is m_tCap
       // if m_tCap < 0, then residual capacity of edge node->sink is -m_tCap                                  

       long    m_ts;   /// time stamp
       long    m_dist; /// distance to the terminal

       long    m_parent = 4;
       long    m_tree = 1; // 0 = source, 1 = sink (if parent!=PARENT_FREE)
       
       long   m_isDumb = 1;    /// whether participating in the maxflow


       short[]   m_nCapOld = new short[3];    /// for recording the capacity in the modified residual graph               

       short[]   m_nCapSub = new short[3];   /// for sub-division  
       Node() {}
   }
   
   
   class AuxNodeInfo{
       short[] m_nCapOrig = new short[NeighborhoodSize];
   }
    
   class BoundarySegment
   {
       int id0, id1;         /// grid ids (vertical -> left/right , horizontal -> up/down)

       boolean bVertical;       /// vertical or horizontal

       int pos;              /// vertical -> x_left, horizontal -> y_up

       int start, end;       /// [start, end) vertical -> y_up & y_down, horizontal -> x_left & x_right

       int diffNum;          /// number of differently labeled pixels on this boundary

       boolean GreaterThan(BoundarySegment rhs)
       {
           return diffNum > rhs.diffNum;
       }
   }
   
   Vector<BoundarySegment> m_rgBoundarySegments, m_rgBoundarySegmentsBkp;
   class Grid
   {  
       /// whether locked or not
       boolean bLocked;      

       /// disjoint set related
       int id;
       
       int parent_id;

       int rank;   /// facilitate path compression, imposing shorter paths

       /// maxflow related        
       int ts;             /// time stamp                
   }
   Vector<Grid> m_rgGrids, m_rgGridsBkp;    
   class Window{
       int x, y;  /// upper left position
       int width, height;  /// size
   }

   /// special treatment for 8-connected graph, store the nodes at the junctions
   Vector<Node>  m_rgUpperLeftNodes;   /// lower right edge should be reset and restored
   Vector<Node>  m_rgUpperRightNodes;  /// lower left ...
   Vector<Node>  m_rgLowerLeftNodes;   /// upper right ...
   Vector<Node>   m_rgLowerRightNodes;  /// upper left ...
   
   Vector<Short>    m_rgUpperLeftVals;    /// stored edge values
   Vector<Short>    m_rgUpperRightVals;   
   Vector<Short>    m_rgLowerLeftVals;    
   Vector<Short>    m_rgLowerRightVals;    

   int[] m_nodeShifts = new int[NeighborhoodSize]; // for functions GetNeib() and GetAndCheckNeib()
   int         m_stride;   // number of bytes per row

   int         m_sizeX, m_sizeY; // image dimensions
   
   ArrayList<Node> m_nodes;
   ArrayList<Node> m_nodeLast;
   ArrayList<AuxNodeInfo> m_pAuxNodeInfo;
   
   // Queue/Stack of orphans
   Queue<Node> NodeQueue;
   Vector<Node> NodeStack;
   
//   ArrayList<Node> GetNodeId(int x, int y)
//   {
//       assert(x>=0 && x<m_sizeX && y>=0 && y<m_sizeY);
//       return m_nodes.get(index) x + y*m_sizeX;
//   }
//
  
   final int GetSizeX() {
       return m_sizeX;
   }
   
   final int GetSizeY() {
       return m_sizeY;
   }
    
    void SetTEdge(Node p, short w) {
          
          p.m_tCap = w;
    }
    
    Node GetNodeId(int x, int y)  
    {
        return m_nodes.get(x+y);
    }
    
    void SetNEdges(Node p, int edge, short w, short w_rev) {
        p.m_nCap[edge] = w;
        
        m_pAuxNodeInfo.get(m_nodes.indexOf(p)).m_nCapOrig[edge]  = w;
        
        final Node q = GetNeib(p, edge);
        final int e_rev = GetReverseEdge(edge);
        q.m_nCap[e_rev] = w_rev;
        
        m_pAuxNodeInfo.get(m_nodes.indexOf(p)).m_nCapOrig[e_rev] = w_rev;
        
        
    }

    private int GetReverseEdge(int edge) {
        // TODO Auto-generated method stub
        return edge^4;
    }

    private Node GetNeib(Node p, int edge) {
        // TODO Auto-generated method stub
        return m_nodes.get(m_nodes.indexOf(p)+m_nodeShifts[edge]);
    }
    
    byte GetSegmentation(Node p)
    {
        return (byte) ((p.m_tree == 1) ? 0 : 255);
    }
    
    
    void ResetNEdges()
    {
        final int length = m_sizeX * m_sizeY;
        for (int i = 0; i < length; ++ i)
        {
            Node p = m_nodes.get( i );
            if (p.m_isDumb == 1) continue;

            AuxNodeInfo pAuxInfo = m_pAuxNodeInfo.get( i );
            p.m_nCap = pAuxInfo.m_nCapOrig;
          
        }   /// i
    }
    
    long Allocate(int sizeX, int sizeY)
    {
        m_nodeShifts[0] = 1;
        m_nodeShifts[1] = sizeX+1;
        m_nodeShifts[2] = sizeX;
        m_nodeShifts[3] = sizeX-1;
        m_nodeShifts[4] = -1;
        m_nodeShifts[5] = -sizeX-1;
        m_nodeShifts[6] = -sizeX;
        m_nodeShifts[7] = -sizeX+1;
        
        return S_OK;
    }
}
