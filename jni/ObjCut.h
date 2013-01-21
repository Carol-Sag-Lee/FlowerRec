#pragma once

#include "ocgmm.h"
#include "BandCut.h"
#include <windows.h>
#include <stdio.h>
#include <math.h>
#include <time.h>
#include <cv.h>
#include <highgui.h>
//#include <opencv2\opencv.hpp>

#include "vis/Vis_Maxflow.h"
#include "vis/MaxflowGraph.h"

#define GC_BGD 0
#define GC_PR_BGD 100
#define GC_FGD 255
#define GC_PR_FGD 180
#define GC_UKNW 128

class CBoundingBox
{
public:
	int x1;
	int y1;
	int x2;
	int y2;
	int width;
	int height;

	CBoundingBox(int leftUpX, int leftUpY, int rightDownX, int rightDownY)
	{
		x1 = leftUpX;
		y1 = leftUpY;
		x2 = rightDownX;
		y2 = rightDownY;
		width = x2 - x1;
		height = y2 - y1;
	}
};

class CObjCut
{
public:
	CObjCut(void);
	~CObjCut(void);

private:
	PBYTE  m_pImageData, m_pDrawingMask;
	int m_nWidth, m_nHeight, m_nImageStride, m_nMaskStride;
	float m_nodeModifier, m_MaxNodeModifier, m_edgeModifier;
	// color models
	int *m_nGMMAssign; 
	OCGMM m_foreGMM, m_backGMM;

	// weights on edges
	double *m_leftW, *m_upleftW, *m_upW, *m_uprightW;

	// max flow solver
	typedef vis::GridGraph8 GraphType;
	GraphType *m_pMaxflow;

private:
	void AllocateMemory();
	void ReleaseMemory();

	double CalcBeta();		//compute energies
	void GetColor(int x, int y, double *color);
	void CalcEdgeWeights(double beta, double gamma);
	int PickColors(double *pfForeground, int &nForeground, double *pfBackground, int &nBackground, int *foreIndex, int *backIndex);

	void testAssign(IplImage *img);
	void AssignGMMsComponents(CBoundingBox box, double *pfForeground, int &nForeground, double *pfBackground, int &nBackground);
	void UpdateGMMs(double *pfForeground, int &nForeground, double *pfBackground, int &nBackground);
	void InitialGraph();
	void ConstructGraph(double gamma, double beta, double lambda);
	void EstimateSegmentation(int *label, int &diffNum, int &foreNum);

	int CutImage(IplImage *img, IplImage *mask, int nMaxIter, int *label, CBoundingBox box);
	void DownSample(IplImage *img, IplImage *mask, int nMaxIter, int *labelDownImg, CBoundingBox box, double scale);

public:
	int Cut(IplImage *img, IplImage *mask, int nMaxIter, int *label, CBoundingBox box, int length = 400, bool bUpSample = true);
};

