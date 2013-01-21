
#include "stdafx.h"


#include "ObjCut.h"
#include "cxcore.h"

CObjCut::CObjCut(void)
{
	m_leftW = NULL;
	m_upleftW = NULL;
	m_uprightW = NULL;
	m_upW = NULL;
	m_nGMMAssign = NULL;
}


CObjCut::~CObjCut(void)
{
	if(m_leftW)
		free(m_leftW);
	if(m_upleftW)
		free(m_upleftW);
	if(m_uprightW)
		free(m_uprightW);
	if(m_upW)
		free(m_upW);
	if(m_nGMMAssign)
		free(m_nGMMAssign);
	if(m_pMaxflow)
		delete m_pMaxflow;
}

void CObjCut::AllocateMemory()
{
	m_nGMMAssign = (int*) malloc(sizeof(int) * m_nWidth * m_nHeight);	
	m_leftW = (double*) malloc(sizeof(double) * m_nWidth * m_nHeight);
	m_upleftW = (double*) malloc(sizeof(double) * m_nWidth * m_nHeight);
	m_upW = (double*) malloc(sizeof(double) * m_nWidth * m_nHeight);
	m_uprightW = (double*) malloc(sizeof(double) * m_nWidth * m_nHeight);
	m_pMaxflow = new(std::nothrow) GraphType(m_nWidth, m_nHeight);

	memset(m_leftW, 0, sizeof(double) * m_nWidth * m_nHeight);
	memset(m_upleftW, 0, sizeof(double) * m_nWidth * m_nHeight);
	memset(m_upW, 0, sizeof(double) * m_nWidth * m_nHeight);
	memset(m_uprightW, 0, sizeof(double) * m_nWidth * m_nHeight);
}

void CObjCut::ReleaseMemory()
{
	free(m_nGMMAssign);
	free(m_leftW);
	free(m_upleftW);
	free(m_upW);
	free(m_uprightW);
	m_nGMMAssign = NULL;
	m_leftW = NULL;
	m_upleftW = NULL;
	m_upW = NULL;
	m_uprightW = NULL;

	if(m_pMaxflow != NULL)
	{
		delete m_pMaxflow;
		m_pMaxflow = NULL;
	}
}

int CObjCut::PickColors(double *pfForeground, int &nForeground, double *pfBackground, int &nBackground, int *foreIndex, int *backIndex)
{
	int i, j, k, l;

	for(j = 0, k = 0, l = 0; j < m_nHeight; j++)
	{
		for(i = 0; i < m_nWidth; i++)
		{
			if( m_pDrawingMask[j * m_nMaskStride + i] == GC_FGD
				|| m_pDrawingMask[j * m_nMaskStride + i] == GC_PR_FGD )
			{
				GetColor(i, j, pfForeground + k * 3);
				foreIndex[k] = j * m_nWidth + i;
				k++;				
			}
			else if( m_pDrawingMask[j * m_nMaskStride + i] == GC_BGD
				|| m_pDrawingMask[j * m_nMaskStride + i] == GC_PR_BGD )
			{
				GetColor(i, j, pfBackground + l * 3);				
				backIndex[l] = j * m_nWidth + i;
				l++;				
			}
		}
	}

	srand( time(NULL) );
	if(k < OCGMM::nMinSampleNum)
		for(; k < OCGMM::nMinSampleNum; )
		{
			i = (int) (1.0 * rand() / RAND_MAX * m_nWidth);
			j = (int) (1.0 * rand() / RAND_MAX * m_nHeight);
			if(m_pDrawingMask[j * m_nMaskStride + i] != GC_BGD
				&& m_pDrawingMask[j * m_nMaskStride + i] != GC_PR_BGD)
			{
				GetColor(i, j, pfForeground + k * 3);
				foreIndex[k] = j * m_nWidth + i;
				k++;	
			}
		}
	if(l < OCGMM::nMinSampleNum)
		for(; l < OCGMM::nMinSampleNum; )
		{
			i = (int) (1.0 * rand() / RAND_MAX * m_nWidth);
			j = (int) (1.0 * rand() / RAND_MAX * m_nHeight);
			if(m_pDrawingMask[j * m_nMaskStride + i] != GC_FGD
				&& m_pDrawingMask[j * m_nMaskStride + i] != GC_PR_FGD)
			{
				GetColor(i, j, pfBackground + l * 3);
				backIndex[l] = j * m_nWidth + i;
				l++;
			}
		}

	if(OCGMM::nMaxSampleNum < k)
	{
		for(int i=0; i<OCGMM::nMaxSampleNum; i++)
		{
			double tmp[3];
			int idx = (int) (1.0 * rand() / RAND_MAX * k);
			memcpy(tmp, pfForeground + idx * 3, sizeof(double)*3);
			memcpy(pfForeground + idx*3, pfForeground + i*3, sizeof(double)*3);
			memcpy(pfForeground + i*3, tmp, sizeof(double)*3);

			int tmpIdx = foreIndex[i];
			foreIndex[i] = foreIndex[idx];
			foreIndex[idx] = tmpIdx;
		}
	}

	if(OCGMM::nMaxSampleNum < l)
	{
		for(int i=0; i<OCGMM::nMaxSampleNum; i++)
		{
			double tmp[3];
			int idx = (int) (1.0 * rand() / RAND_MAX * l);
			memcpy(tmp, pfBackground + idx * 3, sizeof(double)*3);
			memcpy(pfBackground + idx*3, pfBackground + i*3, sizeof(double)*3);
			memcpy(pfBackground + i*3, tmp, sizeof(double)*3);

			int tmpIdx = backIndex[i];
			backIndex[i] = backIndex[idx];
			backIndex[idx] = tmpIdx;
		}
	}

	nForeground = min(k, OCGMM::nMaxSampleNum);
	nBackground = min(l, OCGMM::nMaxSampleNum);
	return 1;
}

/*
  Calculate beta - parameter of GrabCut algorithm.
  beta = 1/(2*avg(sqr(||color[i] - color[j]||)))
*/
double CObjCut::CalcBeta()
{
    double beta = 0, color1[3], color2[3], diff[3];

    for(int y = 0; y < m_nHeight; y++)
    {
        for(int x = 0; x < m_nWidth; x++)
        {         
			GetColor(x, y, color1);
            if(x > 0)				// left
            {            
				GetColor(x - 1, y, color2);
				diff[0] = color1[0] - color2[0]; diff[1] = color1[1] - color2[1]; diff[2] = color1[2] - color2[2];
                beta += diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
            }
            if(y > 0 && x > 0)		// upleft
            {    
				GetColor(x - 1, y - 1, color2);
				diff[0] = color1[0] - color2[0]; diff[1] = color1[1] - color2[1]; diff[2] = color1[2] - color2[2];
                beta += diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
            }
            if(y > 0)				// up
            {              
				GetColor(x, y - 1, color2);
				diff[0] = color1[0] - color2[0]; diff[1] = color1[1] - color2[1]; diff[2] = color1[2] - color2[2];
                beta += diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
            }
            if(y > 0 && x < m_nWidth - 1) // upright
            {                
				GetColor(x + 1, y - 1, color2);
				diff[0] = color1[0] - color2[0]; diff[1] = color1[1] - color2[1]; diff[2] = color1[2] - color2[2];
                beta += diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
            }
        }
    }
    beta = (beta == 0) ? 0 : 1 / (2 * beta / (4 * m_nHeight * m_nWidth - 3 * m_nWidth - 3 * m_nHeight + 2));
    return beta;
}

void CObjCut::GetColor(int x, int y, double *color)
{
	color[0] = m_pImageData[y * m_nImageStride + x * 3 + 0];
	color[1] = m_pImageData[y * m_nImageStride + x * 3 + 1];
	color[2] = m_pImageData[y * m_nImageStride + x * 3 + 2];
}

/*
  Calculate weights of noterminal vertices of graph.
  beta and gamma - parameters of GrabCut algorithm.
 */
void CObjCut::CalcEdgeWeights(double beta, double gamma)
{
    double gammaDivSqrt2, color1[3], color2[3], diff[3], norm;

	float edgeScaling[4];
	edgeScaling[0] = edgeScaling[2] = m_edgeModifier;
	edgeScaling[1] = edgeScaling[3] = m_edgeModifier / sqrt(2.0f);

	gammaDivSqrt2 = gamma / sqrt(2.0);
    
    for(int y = 0; y < m_nHeight; y++ )
    {
        for( int x = 0; x < m_nWidth; x++ )
        {
            GetColor(x, y, color1);
            if(x < m_nWidth - 1 ) // right
            {
				GetColor(x + 1, y, color2);
				diff[0] = color2[0] - color1[0]; diff[1] = color2[1] - color1[1]; diff[2] = color2[2] - color1[2];
				norm = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
                //m_leftW[y * m_nWidth + x] = gamma * exp(-beta * norm);
				m_leftW[y * m_nWidth + x] = edgeScaling[0] * 120.f / (beta * norm + .05f);
			}

            if(x < m_nWidth - 1 && y < m_nHeight - 1) // downright
            {
				GetColor(x + 1, y + 1, color2);
                diff[0] = color2[0] - color1[0]; diff[1] = color2[1] - color1[1]; diff[2] = color2[2] - color1[2];
				norm = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
                //m_upleftW[y * m_nWidth + x] = gammaDivSqrt2 * exp(-beta * norm);
				m_upleftW[y * m_nWidth + x] = edgeScaling[1] * 120.f / (beta * norm + .05f);
			}

            if(y < m_nHeight - 1) // down
            {
				GetColor(x, y + 1, color2);
                diff[0] = color2[0] - color1[0]; diff[1] = color2[1] - color1[1]; diff[2] = color2[2] - color1[2];
				norm = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
                //m_upW[y * m_nWidth + x] = gamma * exp(-beta * norm);                
				m_upW[y * m_nWidth + x] = edgeScaling[2] * 120.f / (beta * norm + .05f);
			}

            if(x > 0 && y < m_nHeight - 1) // downleft
            {
				GetColor(x - 1, y + 1, color2);
                diff[0] = color2[0] - color1[0]; diff[1] = color2[1] - color1[1]; diff[2] = color2[2] - color1[2];
				norm = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
                //m_uprightW[y * m_nWidth + x] = gammaDivSqrt2 * exp(-beta * norm);
				m_uprightW[y * m_nWidth + x] = edgeScaling[3] * 120.f / (beta * norm + .05f);
            }
        }
    }
}

void CObjCut::InitialGraph()
{
	short edge;

	GraphType::NodeId p = m_pMaxflow->GetNodeId(0, 0);
	for(int y = 0; y < m_nHeight; y++)
	{
		for(int x = 0; x < m_nWidth; x++, p++)
		{			
			if(x < m_nWidth - 1)
			{
				edge = m_leftW[y * m_nWidth + x];
				m_pMaxflow->SetNEdges(p, 0, edge, edge);
			}
			if(x < m_nWidth - 1 && y < m_nHeight - 1)
			{                
				edge = m_upleftW[y * m_nWidth + x];
				m_pMaxflow->SetNEdges(p, 1, edge, edge);
			}
			if(y < m_nHeight - 1)
			{                
				edge = m_upW[y * m_nWidth + x];
				m_pMaxflow->SetNEdges(p, 2, edge, edge);					
			}
			if(x > 0 && y < m_nHeight - 1)
			{ 
				edge = m_uprightW[y * m_nWidth + x];
				m_pMaxflow->SetNEdges(p, 3, edge, edge);
			}
		}
	}
}

void CObjCut::AssignGMMsComponents(CBoundingBox box, double *pfForeground, int &nForeground, double *pfBackground, int &nBackground)
{
	int x, y, nTmpFore, nTmpBack;

	nTmpFore = nForeground;
	nTmpBack = nBackground;
	nForeground = nBackground = 0;
	for(y = 0; y < m_nHeight; y++)
	{
		for(x = 0; x < m_nWidth; x++)
        {
			int index = y * m_nMaskStride + x;
			if( m_pDrawingMask[index] == GC_BGD || m_pDrawingMask[index] == GC_PR_BGD )
			{
				GetColor(x, y, pfBackground + nBackground * 3);
				nBackground++;	
			}
			else if(m_pDrawingMask[index] == GC_FGD || m_pDrawingMask[index] == GC_PR_FGD)
			{
				GetColor(x, y, pfForeground + nForeground * 3);
				nForeground++;
			}
		}
    }
	if(nForeground < OCGMM::nMinSampleNum)
		nForeground = nTmpFore;
	if(nBackground < OCGMM::nMinSampleNum)
		nBackground = nTmpBack;
}

void CObjCut::UpdateGMMs(double *pfForeground, int &nForeground, double *pfBackground, int &nBackground)
{	
    int ci;

	srand( time(NULL) );
	if(OCGMM::nMaxSampleNum < nForeground)	// random sample
	{
		for(int i=0; i<OCGMM::nMaxSampleNum; i++)
		{
			double tmp[3];
			int idx = (int) (1.0 * rand() / RAND_MAX * nForeground);
			memcpy(tmp, pfForeground + idx * 3, sizeof(double)*3);
			memcpy(pfForeground + idx*3, pfForeground + i*3, sizeof(double)*3);
			memcpy(pfForeground + i*3, tmp, sizeof(double)*3);
		}
		nForeground = OCGMM::nMaxSampleNum;
	}
	if(OCGMM::nMaxSampleNum < nBackground)
	{
		for(int i=0; i<OCGMM::nMaxSampleNum; i++)
		{
			double tmp[3];
			int idx = (int) (1.0 * rand() / RAND_MAX * nBackground);
			memcpy(tmp, pfBackground + idx * 3, sizeof(double) * 3);
			memcpy(pfBackground + idx * 3, pfBackground + i * 3, sizeof(double) * 3);
			memcpy(pfBackground + i * 3, tmp, sizeof(double) * 3);
		}
		nBackground = OCGMM::nMaxSampleNum;
	}
	
    m_foreGMM.initLearning();
	m_backGMM.initLearning();
	for(int i=0; i<nForeground; i++)
	{
		ci = m_foreGMM.whichComponent(pfForeground + i * 3);
		m_foreGMM.addSample(ci, pfForeground + i * 3);
	}
	for(int i=0; i<nBackground; i++)
	{
		ci = m_backGMM.whichComponent(pfBackground + i * 3);
		m_backGMM.addSample(ci, pfBackground + i * 3);
	}
	m_foreGMM.endLearning();
    m_backGMM.endLearning();

}

void NormalizeFBweights(float& a, float& b)
{
	float sum = a + b;
	const float scale = 64;

	if(sum == 0)
	{
		a = b = 0.5f * scale;
	}
	else
	{
		a *= scale / sum;
		b *= scale / sum;				
	}
}   

void CObjCut::ConstructGraph(double gamma, double beta, double lambda)
{
	int x, y;
	double data[3];
	GraphType::NodeId p = m_pMaxflow->GetNodeId(0, 0);

	for(y = 0; y < m_nHeight; y++)
	{
		for(x = 0; x < m_nWidth; x++, p++)
		{	
			if( m_pDrawingMask[y * m_nMaskStride + x] == GC_PR_BGD
				|| m_pDrawingMask[y * m_nMaskStride + x] == GC_PR_FGD
				|| m_pDrawingMask[y * m_nMaskStride + x] == GC_UKNW )
			{
				GetColor(x, y, data);
				float fromSource = m_backGMM.GetDWeight(data);
				float toSink = m_foreGMM.GetDWeight(data);
				NormalizeFBweights(fromSource, toSink);
				m_pMaxflow->SetTEdge(p, short((fromSource - toSink) * m_nodeModifier));
			}
			if( m_pDrawingMask[y * m_nMaskStride + x] == GC_BGD )	// hard constrain 
			{
				m_pMaxflow->SetTEdge(p, -SHRT_MAX);
			}
			else if( m_pDrawingMask[y * m_nMaskStride + x] == GC_FGD )
			{
				m_pMaxflow->SetTEdge(p, SHRT_MAX);
			}
		}
	}
	m_pMaxflow->ResetNEdges();
}

void CObjCut::EstimateSegmentation(int *label, int &diffNum, int &foreNum)
{
	unsigned int time = GetTickCount();

	//printf("[CObjCut::Cut] cutting the graph\n");
	m_pMaxflow->Maxflow();		
	//printf("flow: %ums\n", GetTickCount() - time);

	foreNum = diffNum = 0;
	GraphType::NodeId p = m_pMaxflow->GetNodeId(0, 0);
	for(int y = 0; y < m_nHeight; y++)
	{
		for(int x = 0; x < m_nWidth; x++, p++)
		{
			int indexMask = y * m_nMaskStride + x;
			int indexLabel = y * m_nWidth + x;
			if( m_pDrawingMask[indexMask] == GC_PR_BGD
			 || m_pDrawingMask[indexMask] == GC_PR_FGD
			 || m_pDrawingMask[indexMask] == GC_UKNW )
			{
				if(m_pMaxflow->GetSegmentation(p) == GC_FGD)
				{
					label[indexLabel] = 1;
					if(m_pDrawingMask[indexMask] != GC_PR_FGD)
						diffNum++;
					m_pDrawingMask[indexMask] = GC_PR_FGD;
					foreNum++;
				}
				else
				{
					label[indexLabel] = 0;
					if(m_pDrawingMask[indexMask] != GC_PR_BGD)
						diffNum++;
					m_pDrawingMask[indexMask] = GC_PR_BGD;
				}
			}
			else if( m_pDrawingMask[indexMask] == GC_BGD )
				label[indexLabel] = 0;
			else	// m_pDrawingMask[indexMask] == GC_FGD 
				label[indexLabel] = 1;
		}
	}
}

void CObjCut::testAssign(IplImage *img)
{
	IplImage *tmp = cvCloneImage(img);
	double color[3], prob[2];
	CvScalar scalar[6] = {cvScalar(255,0,0), cvScalar(0,255,0), cvScalar(0,0,255),
						  cvScalar(0,255,255), cvScalar(255,0,255), cvScalar(255,255,0)};

	srand( time(NULL) );
	for(int i=0; i<5000; i++)
	{
		int x = rand() % img->width;
		int y = rand() % img->height;
		GetColor(x, y, color);
		prob[0] = m_foreGMM(color);
		prob[1] = m_backGMM(color);
		if(prob[0] > prob[1])
			cvCircle(tmp, cvPoint(x, y), 0.5, scalar[0], 2, 8, 0);
		else
			cvCircle(tmp, cvPoint(x, y), 0.5, scalar[2], 2, 8, 0);
	}

	cvNamedWindow("watch", 1);
	cvShowImage("watch", tmp);
	cvWaitKey(0);
	cvDestroyWindow("watch");
	cvReleaseImage(&tmp);
}

int CObjCut::CutImage(IplImage *img, IplImage *mask, int nMaxIter, int *label, CBoundingBox box)
{
	unsigned int time = GetTickCount();
	int nBackground, nForeground, iter, diffNum, uknNum, foreNum;
	int *foreIndex, *backIndex;
	double *pfForeground, *pfBackground, beta, gamma, lambda;

	IplImage *mask_copy = cvCloneImage(mask);
	
	m_pImageData = (PBYTE)img->imageData;
	m_pDrawingMask = (PBYTE)mask->imageData;
	m_nWidth = img->width;
	m_nHeight = img->height;
	m_nImageStride = img->widthStep; 
	m_nMaskStride = mask->widthStep;
	m_edgeModifier = 1.f;
	m_nodeModifier = 2.f;
	m_MaxNodeModifier = 512.f;
	uknNum = (box.x2 - box.x1 + 1) * (box.y2 - box.y1 + 1);
	if(uknNum == 0)
		uknNum = m_nWidth * m_nHeight / 400;
	AllocateMemory();

	// train initial GMM models for foreground and background colors
	pfForeground = (double*) malloc(sizeof(double) * m_nWidth * m_nHeight * 3);
	pfBackground = (double*) malloc(sizeof(double) * m_nWidth * m_nHeight * 3);
	foreIndex = (int*)malloc(sizeof(int)*m_nWidth*m_nHeight);
	backIndex = (int*)malloc(sizeof(int)*m_nWidth*m_nHeight);

	PickColors(pfForeground, nForeground, pfBackground, nBackground, foreIndex, backIndex);		
	m_foreGMM.initialize(pfForeground, nForeground);	
	m_backGMM.initialize(pfBackground, nBackground);

	beta = CalcBeta();		
	gamma = 50;				// Relative importance term in definition of energy
	lambda = gamma * 9;
	CalcEdgeWeights(beta, gamma);		
	InitialGraph();

	//printf("before cut: %ums\n", GetTickCount() - time);
	for(iter = 0; iter < nMaxIter; iter++)
	{
		//testAssign(img);
		ConstructGraph(gamma, beta, lambda);
		EstimateSegmentation(label, diffNum, foreNum);

		// no FGD regions		
		if(float(foreNum) / uknNum < 0.01f && m_nodeModifier < m_MaxNodeModifier)
		{
			cvCopyImage(mask_copy, mask);
			m_nodeModifier *= 2;
			printf("rest nodeModifier to:  %f\n", m_nodeModifier);
			iter--;
		}

		// no much change
		if( float(diffNum) / uknNum < 1e-2 )	
			break;
		if( iter < nMaxIter - 1)
		{
			AssignGMMsComponents(box, pfForeground, nForeground, pfBackground, nBackground);
			UpdateGMMs(pfForeground, nForeground, pfBackground, nBackground);
		}
	}

	ReleaseMemory(); 

	free(pfForeground);
	free(pfBackground);
	free(foreIndex);
	free(backIndex);
	cvReleaseImage(&mask_copy);

	return 1;
}

void DownMask(IplImage *src, IplImage *dst)
{
	double scale  = 1.0 * src->width / dst->width;
	double dx, dy;
	int nx, ny;
	BYTE color;

	cvResize(src, dst, 0);
	for(int j = 0; j < src->height; j++)
		for(int i = 0; i < src->width; i++)
		{
			color = ((BYTE*)src->imageData)[j * src->widthStep + i];
			if(color == GC_BGD || color == GC_FGD)
			{
				dx = i / scale;
				dy = j / scale;
				nx = (int)dx;
				ny = (int)dy;
				nx = dx - nx > 0.5 ? nx + 1 : nx;
				ny = dy - ny > 0.5 ? ny + 1 : ny;
				nx = nx < dst->width ? nx : nx - 1;
				ny = ny < dst->height ? ny : ny - 1;
				
				((BYTE*)dst->imageData)[ny * dst->widthStep + nx] = color;
			}
		}
}

void CObjCut::DownSample(IplImage *img, IplImage *mask, int nMaxIter, int *labelDownImg, CBoundingBox box, double scale)
{
	IplImage *imgDown = cvCreateImage(cvSize(img->width/scale, img->height/scale), 8, 3);
	IplImage *maskDown = cvCreateImage(cvSize(img->width/scale, img->height/scale), 8, 1);


	box.x1 /= scale; box.x2 /= scale; box.y1 /= scale; box.y2 /= scale;
	box.width = box.x2 - box.x1; box.height = box.y2 - box.y1;
	cvResize(img, imgDown, CV_INTER_LINEAR);
	//cvResize(mask, maskDown, 0);
	DownMask(mask, maskDown);	// to be more precise
	CutImage(imgDown, maskDown, nMaxIter, labelDownImg, box);
	cvResize(maskDown, mask, CV_INTER_NN);
	cvReleaseImage(&imgDown);
	cvReleaseImage(&maskDown);
}

int CObjCut::Cut(IplImage *img, IplImage *mask, int nMaxIter, int *label, CBoundingBox box, int length, bool bUpSample)
{
	int *labelDownImg, time;
	double scale;
	int nPixels = img->width * img->height;
	int nMinImgPixels = length * length;

	if(nPixels <= nMinImgPixels)
	{
		CutImage(img, mask, nMaxIter, label, box);
		return 1;
	}
	else
	{	time = GetTickCount();
		CBandCut bandCut;
		scale = sqrt((1.0 * img->width * img->height) / nMinImgPixels);
		labelDownImg = (int*) malloc(sizeof(int) * (img->width/scale) * (img->height/scale));
		DownSample(img, mask, nMaxIter, labelDownImg, box, scale);
		//printf("time downsample: %ums\n", GetTickCount() - time);time = GetTickCount();
		if(bUpSample)
			bandCut.UpSample(img, mask, label, labelDownImg, scale);
		else
			bandCut.UpSampleTrivial(mask, label, labelDownImg, scale);
		//printf("time upsample: %ums\n", GetTickCount() - time);
		free(labelDownImg);
	}
	return 1;
}