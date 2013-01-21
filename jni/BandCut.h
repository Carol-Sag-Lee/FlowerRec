#pragma once

#include "bdmf/graph.h"
#include "ocgmm.h"
#include <windows.h>
#include <stdio.h>
#include <math.h>
#include <time.h>
#include <queue>
#include <vector>
using namespace std;
#include <cv.h>
//#include <opencv2\opencv.hpp>

#define GC_BGD 0
#define GC_PR_BGD 100
#define GC_FGD 255
#define GC_PR_FGD 180
#define GC_UKNW 128

class CBandCut
{
public:
	static const int nMinImgPixels = 400 * 400;

private:
	OCGMM m_foreGMM, m_backGMM;
	vector<int> m_boundaryEdgePos; // the pixel's position of each band edge
	vector<vector<int>> m_bandEdgePos; 
	
	// max flow solver
	typedef Graph<double, double, double> GraphType;
	GraphType *m_pMaxflow;

private:
	bool IsValidPixel(int x, int y, int width, int height);
	void FindOnEdgePixel(IplImage *mask);
	void FindContour(IplImage *mask, int formerWidth);
	void EncodeEdges(int startPos, bool *bOnEdge, bool *bVisited, int width, int height);
	void PickEdgeColor(vector<int> &vtxPos, IplImage *img, IplImage *mask,
		double *pfForeground, int &nForeground, double *pfBackground, int &nBackground);
	void GetEdgeColor(int x, int y, double *color, IplImage *img);
	double CalcEdgeBeta(vector<int> &vtxPos, IplImage *img);
	void GetPixelIndex(int *pixelIndex, vector<int> &vtxPos, int size);
	void ConstructGraph(vector<int> &vtxPos, int *pixelIndex, IplImage *mask, IplImage *img);
	void UpdateEdge(IplImage *mask, vector<int> &vtxPos, int *pixelIndex);
	void CutEdge(IplImage *img, IplImage *mask);
	void UpdateMask(IplImage *mask, IplImage *tmpMask);
	void SetLabelByMask(int *label, IplImage *mask);	

public:
	void UpSample(IplImage *img, IplImage *mask, int *label, int *labelDownImg, double scale);
	void UpSampleTrivial(IplImage *mask, int *label, int *labelDownImg, double scale);
};