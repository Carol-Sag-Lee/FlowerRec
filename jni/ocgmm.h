/*
especially designed from object cut, not a fully functional gmm class
*/
#pragma once

#include <windows.h>
#include <stdio.h>
#include <math.h>
#include <limits>


/*
GMM - Gaussian Mixture Model
*/
class OCGMM
{
public:
	static const int componentsCount = 6;
	static const int nMaxSampleNum = 1800;
	static const int nMinSampleNum = 1000;

public:	
	double *coefs;
	double *mean;
	double *cov;

	double inverseCovs[componentsCount][3][3];
	double covDeterms[componentsCount];

	double sums[componentsCount][3];
	double prods[componentsCount][3][3];
	double eofLog[componentsCount];
	double det[componentsCount];
	int sampleCounts[componentsCount];
	int totalSampleCount;

public:
	OCGMM();
	~OCGMM();

	double operator()(double *color);
	double operator()(int ci, double *color);

	//initialize GMM by kmeans
	int initialize(double *data, int nSamples);
	int whichComponent(double *color);
	double GetDWeight(double *color);
	void initLearning();
	void addSample(int ci, double *color);
	void endLearning();

private:
	void calcInverseCovAndDeterm(int ci);
};