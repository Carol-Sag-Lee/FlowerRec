
#include "stdafx.h"

#include "ObjCut.h"

bool CBandCut::IsValidPixel(int x, int y, int width, int height)
{
	return (x > 0 && x < width && y > 0 && y < height);
}

void CBandCut::FindOnEdgePixel(IplImage *mask)
{
	int width = mask->width;
	int height = mask->height;
	BYTE color, colorNbr;
	bool bOnEdge;

	m_boundaryEdgePos.clear();
	for(int y = 0; y < height; y++)
		for(int x = 0; x < width; x++)
		{
			bOnEdge = false;
			color = ((BYTE*)mask->imageData)[y * mask->widthStep + x];
			color = (color == GC_BGD || color == GC_PR_BGD) ? GC_BGD : GC_FGD;
			for(int yy = y - 1; yy <= y+1; yy++)
				for(int xx = x - 1; xx <= x+1; xx++)
				{
					if( !IsValidPixel(xx, yy, width, height) )
						continue;
					colorNbr = ((BYTE*)mask->imageData)[yy * mask->widthStep + xx];
					colorNbr = (colorNbr == GC_BGD || colorNbr == GC_PR_BGD) ? GC_BGD : GC_FGD;
					if(color != colorNbr)
						bOnEdge = true;
				}

				if(bOnEdge)
					m_boundaryEdgePos.push_back(y * width + x);
		}
}

void CBandCut::EncodeEdges(int startPos, bool *bOnEdge, bool *bVisited, int width, int height)
{
	queue<int> q;
	vector<int> tmpPos;
	int pos, x, y;

	bVisited[startPos] = true;
	tmpPos.reserve(5000);
	tmpPos.push_back(startPos);

	q.push(startPos);
	while(!q.empty())	// BFS edges
	{
		pos = q.front();
		q.pop();
		x = pos % width;
		y = pos / width;
		for(int i = x - 1; i <= x + 1; i++)
			for(int j = y - 1; j <= y + 1; j++)
			{
				if(!IsValidPixel(i, j, width, height))
					continue;
				int posNbr = j * width + i;
				if(bOnEdge[posNbr] && !bVisited[posNbr])
				{
					bVisited[posNbr] = true;
					tmpPos.push_back(posNbr);
					q.push(posNbr);
				}
			}
	}
	if(tmpPos.size() > 80)	// omit small edges
		m_bandEdgePos.push_back(tmpPos);
}

void CBandCut::FindContour(IplImage *mask, int formerWidth)
{
	int width = mask->width, height = mask->height;
	int widthDown = formerWidth, heightDown = int( 1.0 * formerWidth / width * height );
	int expandMargin = 2;
	double sizeFactor = 1.0 * width / widthDown;
	BYTE color, colorNbr;
	bool *bOnEdge = (bool*)malloc(sizeof(bool) * width * height);
	bool *bVisited = (bool*)malloc(sizeof(bool) * width * height);
	int *newPosOnEdgePixel = (int*)malloc(sizeof(bool) * width * height), nNewOnEdgePixel = 0;

	memset(bOnEdge, 0, sizeof(bool) * width * height);
	memset(bVisited, 0, sizeof(bool) * width * height);

	// find onEdgePixels by upSample
	for(UINT i = 0; i < m_boundaryEdgePos.size(); i++)	
	{
		int posDown = m_boundaryEdgePos[i];
		int xDown = posDown % widthDown;
		int yDown = posDown / widthDown;
		double xUp = xDown * sizeFactor;
		double yUp = yDown * sizeFactor;

		for(int y = yUp - sizeFactor - 1; y < yUp + sizeFactor + 1; y++)
			for(int x = xUp - sizeFactor - 1; x < xUp + sizeFactor + 1; x++)
			{
				if(!IsValidPixel(x, y, width, height))
					continue;

				color = ((BYTE*)mask->imageData)[y * mask->widthStep + x];
				for(int yy = y - 1; yy <= y + 1; yy++)
					for(int xx = x - 1; xx <= x + 1; xx++)
					{
						if(!IsValidPixel(xx, yy, width, height))
							continue;
						colorNbr = ((BYTE*)mask->imageData)[yy * mask->widthStep + xx];
						if(colorNbr != color)
						{
							if(!bOnEdge[y * width + x])
							{
								newPosOnEdgePixel[nNewOnEdgePixel] = y * width + x;
								nNewOnEdgePixel++;
							}
							bOnEdge[y * width + x] = true;
						}
					}
			}
	}

	for(int i = 0; i < nNewOnEdgePixel; i++)	// expand boundary
	{
		int pos = newPosOnEdgePixel[i];
		int x = pos % width;
		int y = pos / width;
		for(int xx = x - expandMargin; xx <= x + expandMargin; xx++)
			for(int yy = y - expandMargin; yy <= y + expandMargin; yy++)
			{
				if(!IsValidPixel(xx, yy, width, height))
					continue;
				bOnEdge[yy * width + xx] = true;
			}
	}

	m_bandEdgePos.clear();
	for(int i = 0; i < nNewOnEdgePixel; i++)
	{
		int pos = newPosOnEdgePixel[i];
		if(bOnEdge[pos] && !bVisited[pos])
			EncodeEdges(pos, bOnEdge, bVisited, width, height);
	}

	free(bOnEdge);
	free(bVisited);
	free(newPosOnEdgePixel);
}

void CBandCut::CutEdge(IplImage *img, IplImage *mask)
{
	int nForeground, nBackground, width = mask->width, height = mask->height;
	double *pfForeground = (double*)malloc(sizeof(double) * img->width * img->height * 3);
	double *pfBackground = (double*)malloc(sizeof(double) * img->width * img->height * 3);
	int *pixelIndex = (int*)malloc(sizeof(int) * width * height);;


	m_boundaryEdgePos.clear();
	for(UINT idx = 0; idx < m_bandEdgePos.size(); idx++)
	{	
		PickEdgeColor(m_bandEdgePos[idx], img, mask, pfForeground, nForeground, pfBackground, nBackground);
		m_foreGMM.initialize(pfForeground, nForeground);	
		m_backGMM.initialize(pfBackground, nBackground);

		GetPixelIndex(pixelIndex, m_bandEdgePos[idx], width * height);
		ConstructGraph(m_bandEdgePos[idx], pixelIndex, mask, img);
		UpdateEdge(mask, m_bandEdgePos[idx], pixelIndex);
		delete m_pMaxflow;
	}

	free(pfForeground);
	free(pfBackground);
	free(pixelIndex);
}

void CBandCut::SetLabelByMask(int *label, IplImage *mask)
{
	int width = mask->width;
	int height = mask->height;
	BYTE *ptrColor,	color;
	
	for(int y = 0; y < height; y++)
	{
		ptrColor = (BYTE*)mask->imageData + y * mask->widthStep;
		for(int x = 0; x < width; x++)		
		{			
			if(x < 5 || y < 5 || x >= width - 5 || y >= height - 5)
			{
				label[y * width + x] = 0;
				ptrColor[x] = GC_BGD;
				continue;
			}
			color = ptrColor[x];
			if(color == GC_FGD || color == GC_PR_FGD)
				label[y * width + x] = 1;
			else
				label[y * width + x] = 0;
		}
	}
}

void CBandCut::UpdateMask(IplImage *mask, IplImage *tmpMask)
{
	int width = mask->width;
	int height = mask->height;
	BYTE color;

	for(int y = 0; y < height; y++)
		for(int x = 0; x < width; x++)
		{
			color = ((BYTE*)mask->imageData)[y * mask->widthStep + x];
			if(color != GC_BGD && color != GC_FGD)
				((BYTE*)mask->imageData)[y * mask->widthStep + x] = ((BYTE*)tmpMask->imageData)[y * tmpMask->widthStep + x];
		}
}

void CBandCut::UpSample(IplImage *img, IplImage *mask, int *label, int *labelDownImg, double scale)
{
	int nDownImgWidth = int(img->width/scale);
	int nDownImgHeight = int(img->height/scale);
	int tmpWidth, tmpHeight;
	int nScales, i;
	
	IplImage *tmpImg, *tmpMask, *formerMask;

	formerMask = cvCreateImage(cvSize(nDownImgWidth, nDownImgHeight), mask->depth, mask->nChannels);
	cvResize(mask, formerMask, CV_INTER_NN);
	m_boundaryEdgePos.reserve(5000);
	FindOnEdgePixel(formerMask);

	nScales = 1;
	i = 2;
	while(scale / i > 1)
	{
		nScales++;
		i *= 2;
	}
	for(i = 0; i < nScales; i++)
	{
		tmpWidth = int( nDownImgWidth * pow(2.0, i+1) );
		tmpHeight = int( nDownImgHeight * pow(2.0, i+1) );
		if(i == nScales - 1)
		{
			tmpWidth = img->width;
			tmpHeight = img->height;
		}
		tmpImg = cvCreateImage(cvSize(tmpWidth, tmpHeight), img->depth, img->nChannels);
		tmpMask = cvCreateImage(cvSize(tmpWidth, tmpHeight), mask->depth, mask->nChannels);
		cvResize(img, tmpImg, CV_INTER_NN);
		cvResize(formerMask, tmpMask, CV_INTER_NN);

		FindContour(tmpMask, formerMask->width);
		CutEdge(tmpImg, tmpMask);
		cvReleaseImage(&tmpImg);
		cvReleaseImage(&formerMask);
		formerMask = tmpMask;
	}
	UpdateMask(mask, tmpMask);
	SetLabelByMask(label, mask);
	cvReleaseImage(&formerMask);
}

void CBandCut::UpSampleTrivial(IplImage *mask, int *label, int *labelDownImg, double scale)
{
	int nDownImgWidth = int(mask->width/scale);
	int nDownImgHeight = int(mask->height/scale);
	double dx, dy;
	int nx, ny;
	BYTE color;

	for(int j = 0; j < mask->height; j++)
		for(int i = 0; i < mask->width; i++)
		{
			dx = i / scale;
			dy = j / scale;
			nx = (int)dx;
			ny = (int)dy;
			nx = dx - nx > 0.5 ? nx + 1 : nx;
			ny = dx - ny > 0.5 ? ny + 1 : ny;
			nx = nx < nDownImgWidth ? nx : nDownImgWidth - 1;
			ny = ny < nDownImgHeight ? ny : nDownImgHeight - 1;
			label[j * mask->width + i] = labelDownImg[ny * nDownImgWidth + nx];
			color = label[j * mask->width + i] > 0 ? GC_FGD : GC_BGD;			
			((BYTE*)mask->imageData)[j * mask->widthStep + i] = color;
		}
}

void CBandCut::PickEdgeColor(vector<int> &vtxPos, IplImage *img, IplImage *mask,
	double *pfForeground, int &nForeground, double *pfBackground, int &nBackground)
{
	int width = img->width;
	int height = img->height;
	int x, y, posMask;
	BYTE color;

	nForeground = nBackground = 0;
	for(UINT i = 0; i < vtxPos.size(); i++)
	{
		x = vtxPos[i] % width;
		y = vtxPos[i] / width;
		posMask = y * mask->widthStep + x;
		color = ((BYTE*)mask->imageData)[posMask];
		if( color == GC_BGD || color == GC_PR_BGD )
		{
			GetEdgeColor(x, y, pfBackground + nBackground * 3, img);
			nBackground++;	
		}
		else if(color == GC_FGD || color == GC_PR_FGD)
		{
			GetEdgeColor(x, y, pfForeground + nForeground * 3, img);
			nForeground++;
		}
	}

	if(OCGMM::nMaxSampleNum < nForeground)
	{
		for(int i=0; i<OCGMM::nMaxSampleNum; i++)
		{
			double tmp[3];
			int idx = (int) (1.0 * rand() / RAND_MAX * nForeground);
			memcpy(tmp, pfForeground + idx * 3, sizeof(double)*3);
			memcpy(pfForeground + idx*3, pfForeground + i*3, sizeof(double)*3);
			memcpy(pfForeground + i*3, tmp, sizeof(double)*3);
		}
	}

	if(OCGMM::nMaxSampleNum < nBackground)
	{
		for(int i=0; i<OCGMM::nMaxSampleNum; i++)
		{
			double tmp[3];
			int idx = (int) (1.0 * rand() / RAND_MAX * nBackground);
			memcpy(tmp, pfBackground + idx * 3, sizeof(double)*3);
			memcpy(pfBackground + idx*3, pfBackground + i*3, sizeof(double)*3);
			memcpy(pfBackground + i*3, tmp, sizeof(double)*3);
		}
	}

	nForeground = min(nForeground, OCGMM::nMaxSampleNum);
	nBackground = min(nBackground, OCGMM::nMaxSampleNum);
}

void CBandCut::GetEdgeColor(int x, int y, double *color, IplImage *img)
{
	color[0] = ((BYTE*)img->imageData)[y * img->widthStep + x * 3 + 0];
	color[1] = ((BYTE*)img->imageData)[y * img->widthStep + x * 3 + 1];
	color[2] = ((BYTE*)img->imageData)[y * img->widthStep + x * 3 + 2];
}

double CBandCut::CalcEdgeBeta(vector<int> &vtxPos, IplImage *img)
{
	double beta = 0, color1[3], color2[3], diff[3];
	int width = img->width;
	int height = img->height;
	int posVtx, x, y;

	for(UINT i = 0; i < vtxPos.size(); i++)
	{
		posVtx = vtxPos[i];
		x = posVtx % width;
		y = posVtx / width;
		GetEdgeColor(x, y, color1, img);
		if(x > 0)				// left
		{            
			GetEdgeColor(x - 1, y, color2, img);
			diff[0] = color1[0] - color2[0]; diff[1] = color1[1] - color2[1]; diff[2] = color1[2] - color2[2];
			beta += diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
		}
		if(y > 0 && x > 0)		// upleft
		{    
			GetEdgeColor(x - 1, y - 1, color2, img);
			diff[0] = color1[0] - color2[0]; diff[1] = color1[1] - color2[1]; diff[2] = color1[2] - color2[2];
			beta += diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
		}
		if(y > 0)				// up
		{              
			GetEdgeColor(x, y - 1, color2, img);
			diff[0] = color1[0] - color2[0]; diff[1] = color1[1] - color2[1]; diff[2] = color1[2] - color2[2];
			beta += diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
		}
		if(y > 0 && x < width - 1) // upright
		{                
			GetEdgeColor(x + 1, y - 1, color2, img);
			diff[0] = color1[0] - color2[0]; diff[1] = color1[1] - color2[1]; diff[2] = color1[2] - color2[2];
			beta += diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
		}
	}
	beta = (beta == 0) ? 0 : 1 / (2 * beta / (4 * vtxPos.size()) );
	return beta;
}

void CBandCut::GetPixelIndex(int *pixelIndex, vector<int> &vtxPos, int size)
{
	int posVtx;

	for(int i = 0; i < size; i++)
		pixelIndex[i] = -1;
	for(UINT i = 0; i < vtxPos.size(); i++)
	{
		posVtx = vtxPos[i];
		pixelIndex[posVtx] = i;
	}
}

void CBandCut::ConstructGraph(vector<int> &vtxPos, int *pixelIndex, IplImage *mask, IplImage *img)
{
	int x, y, posVtx, posMask, posNbr, width, height, edgeCount, nodeModifier = 8;
	double data[3], dataNbr[3], diff[3], edgeScaling[4];
	double fromSource, toSink, edge, norm, beta;
	BYTE colorMask;

	width = mask->width;
	height = mask->height;
	edgeCount = 8 * vtxPos.size();	

	m_pMaxflow = new Graph<double, double, double>(vtxPos.size(), edgeCount);
	m_pMaxflow->add_node(vtxPos.size());

	edgeScaling[0] = edgeScaling[2] = 1;
	edgeScaling[1] = edgeScaling[3] = 1 / sqrt(2.0f);

	beta = CalcEdgeBeta(vtxPos, img);
	for(UINT i = 0; i < vtxPos.size(); i++)
	{
		posVtx = vtxPos[i];
		x = posVtx % width;
		y = posVtx / width;
		posMask = y * mask->widthStep + x;
		colorMask = ((BYTE*)mask->imageData)[posMask];

		GetEdgeColor(x, y, data, img);
		if( colorMask == GC_PR_BGD || colorMask == GC_PR_FGD || colorMask == GC_UKNW )
		{
			fromSource = m_backGMM.GetDWeight(data) * nodeModifier;
			toSink = m_foreGMM.GetDWeight(data) * nodeModifier;
		}
		if( colorMask == GC_BGD )	// hard constrain 
		{
			fromSource = 0;
			toSink = FLT_MAX;
		}
		else if( colorMask == GC_FGD )
		{
			fromSource = FLT_MAX;
			toSink = 0;
		}

		// set hard-constrain on edge boundary
		for(int xx = x - 1; xx <= x + 1; xx++)	
			for(int yy = y - 1; yy <= y + 1; yy++)
			{
				if(!IsValidPixel(xx, yy, width, height) || pixelIndex[yy * width + xx] >= 0)
					continue;
				if(colorMask == GC_FGD || colorMask == GC_PR_FGD)
				{
					fromSource = FLT_MAX;
					toSink = 0;
				}
				else
				{
					fromSource = 0;
					toSink = FLT_MAX;
				}					
			}
			m_pMaxflow->add_tweights(pixelIndex[posVtx], fromSource, toSink);

			// set n-weights
			if(x > 0)
			{
				posNbr =  y * width + x - 1;
				GetEdgeColor(x - 1, y, dataNbr, img);
				diff[0] = data[0] - dataNbr[0]; diff[1] = data[1] - dataNbr[1]; diff[2] = data[2] - dataNbr[2];
				norm = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
				edge = edgeScaling[0] * 120.f / (beta * norm + .05f);
				if(pixelIndex[posNbr] >= 0)
					m_pMaxflow->add_edge(pixelIndex[posVtx], pixelIndex[posNbr], edge, edge);
			}
			if(x > 0 && y > 0)
			{           
				posNbr = (y - 1) * width + x - 1;
				GetEdgeColor(x - 1, y - 1, dataNbr, img);
				diff[0] = data[0] - dataNbr[0]; diff[1] = data[1] - dataNbr[1]; diff[2] = data[2] - dataNbr[2];
				norm = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
				edge = edgeScaling[1] * 120.f / (beta * norm + .05f);
				if(pixelIndex[posNbr] >= 0)
					m_pMaxflow->add_edge(pixelIndex[posVtx], pixelIndex[posNbr], edge, edge);
			}
			if(y > 0)
			{               
				posNbr = (y - 1) * width + x;
				GetEdgeColor(x, y - 1, dataNbr, img);
				diff[0] = data[0] - dataNbr[0]; diff[1] = data[1] - dataNbr[1]; diff[2] = data[2] - dataNbr[2];
				norm = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
				edge = edgeScaling[2] * 120.f / (beta * norm + .05f);
				if(pixelIndex[posNbr] >= 0)
					m_pMaxflow->add_edge(pixelIndex[posVtx], pixelIndex[posNbr], edge, edge);					
			}
			if(x < width - 1 && y > 0)
			{ 
				posNbr = (y - 1) * width + x + 1;
				GetEdgeColor(x + 1, y - 1, dataNbr, img);
				diff[0] = data[0] - dataNbr[0]; diff[1] = data[1] - dataNbr[1]; diff[2] = data[2] - dataNbr[2];
				norm = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
				edge = edgeScaling[3] * 120.f / (beta * norm + .05f);
				if(pixelIndex[posNbr] >= 0)
					m_pMaxflow->add_edge(pixelIndex[posVtx], pixelIndex[posNbr], edge, edge);
			}	
	}
}

void CBandCut::UpdateEdge(IplImage *mask, vector<int> &vtxPos, int *pixelIndex)
{
	int width = mask->width;
	int height = mask->height;
	int x, y, pos, posNbr;
	BYTE color;

	m_pMaxflow->maxflow();

	for(UINT i = 0; i < vtxPos.size(); i++)
	{
		pos = vtxPos[i];
		x = pos % width;
		y = pos / width;

		// update those pixels on the edge
		bool onEdge = false;
		for(int xx = x - 1; xx <= x + 1; xx++)
			for(int yy = y - 1; yy <= y + 1; yy++)
			{
				posNbr = yy * width + xx;
				if(!IsValidPixel(xx, yy, width, height) || pixelIndex[posNbr] < 0)
					continue;
				if(m_pMaxflow->what_segment(pixelIndex[pos]) != m_pMaxflow->what_segment(pixelIndex[posNbr]))
					onEdge = true;
			}
			if(onEdge)
				m_boundaryEdgePos.push_back(pos);

			// update mask
			color = ((BYTE*)mask->imageData)[y*mask->widthStep+x];
			if(color == GC_BGD || color == GC_FGD)
				continue;
			if(m_pMaxflow->what_segment(pixelIndex[pos]) == Graph<double, double, double>::SOURCE)
				((BYTE*)mask->imageData)[y * mask->widthStep + x] = GC_PR_FGD;
			else
				((BYTE*)mask->imageData)[y * mask->widthStep + x] = GC_PR_BGD;
	}
}
