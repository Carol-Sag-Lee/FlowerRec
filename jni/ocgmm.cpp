
#include "stdafx.h"

#include "ocgmm.h"

OCGMM::OCGMM()
{		
	coefs = (double*) malloc(sizeof(double) * componentsCount);
	mean = (double*) malloc(sizeof(double) * componentsCount * 3);
	cov = (double*) malloc(sizeof(double) * componentsCount * 9);	
	memset(coefs, 0, sizeof(double) * componentsCount);
	memset(mean, 0, sizeof(double) * componentsCount * 3);	
	memset(cov, 0, sizeof(double) * componentsCount * 9);
}

OCGMM::~OCGMM()
{
	if(coefs)
	{
		free(coefs);
		coefs = NULL;
	}
	if(mean)
	{
		free(mean);
		mean = NULL;
	}
	if(cov)
	{
		free(cov);
		cov = NULL;
	}
}

double OCGMM::GetDWeight(double *color)
{
	double ret = DBL_MAX, *m, diff[3];

	for(int ci = 0; ci < componentsCount; ci++ )
	{
		if(coefs[ci]>0)
		{
			m = mean + 3 * ci;
			diff[0] = color[0] - m[0]; diff[1] = color[1] - m[1]; diff[2] = color[2] - m[2];
			double mult = diff[0] * (diff[0] * inverseCovs[ci][0][0] + diff[1]*inverseCovs[ci][1][0] + diff[2]*inverseCovs[ci][2][0])
				+ diff[1]*(diff[0]*inverseCovs[ci][0][1] + diff[1]*inverseCovs[ci][1][1] + diff[2]*inverseCovs[ci][2][1])
				+ diff[2]*(diff[0]*inverseCovs[ci][0][2] + diff[1]*inverseCovs[ci][1][2] + diff[2]*inverseCovs[ci][2][2]);
			double sum = eofLog[ci] + 0.5 * mult;
			if( sum < ret )
				ret = sum;
		}
	}
	return ret;
}

double OCGMM::operator()(double * color)
{
	double res = 0;

	for(int ci = 0; ci < componentsCount; ci++ )
	{
		res += coefs[ci] * (*this)(ci, color);
	}
	return res;
}

double OCGMM::operator()(int ci, double *color)
{
	double res = 0, diff[3], *m;

	if(coefs[ci] > 0)
	{
		//CV_Assert( covDeterms[ci] > std::numeric_limits<double>::epsilon() );						
		m = mean + 3 * ci;
		diff[0] = color[0] - m[0]; diff[1] = color[1] - m[1]; diff[2] = color[2] - m[2];
		double mult = diff[0] * (diff[0] * inverseCovs[ci][0][0] + diff[1]*inverseCovs[ci][1][0] + diff[2]*inverseCovs[ci][2][0])
			+ diff[1]*(diff[0]*inverseCovs[ci][0][1] + diff[1]*inverseCovs[ci][1][1] + diff[2]*inverseCovs[ci][2][1])
			+ diff[2]*(diff[0]*inverseCovs[ci][0][2] + diff[1]*inverseCovs[ci][1][2] + diff[2]*inverseCovs[ci][2][2]);
		res = 1.0 / sqrt(covDeterms[ci]) * exp(-0.5 * mult);
	}
	return res;
}

int OCGMM::initialize(double *data, int nSamples)
{
	int i, j, k, iter, *assign, *oldassign, changed;
	double diff[3], dist, minDist;

	assign = (int*) malloc(sizeof(int) * nSamples * 2);
	oldassign = assign + nSamples;
	memset(assign, 0, sizeof(int) * nSamples * 2);
	memset(mean, 0, sizeof(double) * componentsCount * 3);
	// random select initial central
	j = 0;
	for(int i = 0; i < nSamples; i++)
	{
		int idx = rand() % nSamples;
		bool diff = true;

		for(int k = 0; k < j; k++)
		{
			double delta = fabs(mean[k * 3 + 0] - data[idx * 3 + 0])
							+ fabs(mean[k * 3 + 1] - data[idx * 3 + 1])
							+ fabs(mean[k * 3 + 2] - data[idx * 3 + 2]);
			if(delta < 20){	// omit those pixel without much difference
				diff = false;
				break;
			}
		}
		if(diff)
		{
			memcpy(mean + j * 3, data + idx * 3, sizeof(double) * 3);
			j++;
		}
		if(j == componentsCount)
			break;
	}

	for(iter = 0; iter < 100; iter++)
	{
		//find new assignments
		for(i = 0, changed = 0; i < nSamples; i++)
		{
			for(j = 0, k = -1; j < componentsCount; j++)
			{
				diff[0] = data[i * 3 + 0] - mean[j * 3 + 0];
				diff[1] = data[i * 3 + 1] - mean[j * 3 + 1];
				diff[2] = data[i * 3 + 2] - mean[j * 3 + 2];
				dist = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
				if(k == -1 || dist < minDist)
				{
					k = j;
					minDist = dist;
				}
			}
			assign[i] = k;
			if(oldassign[i] != assign[i])
				changed++;
		}

		if(changed == 0) //is converged?
		{
			break;
		}
		memcpy(oldassign, assign, sizeof(int) * nSamples);

		//update means
		memset(coefs, 0, sizeof(double) * componentsCount);
		memset(mean, 0, sizeof(double) * componentsCount * 3);
		for(i = 0; i < nSamples; i++)
		{
			k = assign[i];
			coefs[k]++;
			mean[k * 3 + 0] += data[i * 3 + 0];
			mean[k * 3 + 1] += data[i * 3 + 1];
			mean[k * 3 + 2] += data[i * 3 + 2];
		}
		for(i = 0; i < componentsCount; i++)
		{
			mean[i * 3 + 0] /= coefs[i];
			mean[i * 3 + 1] /= coefs[i];
			mean[i * 3 + 2] /= coefs[i];
		}
	}

	initLearning();
	for(i = 0; i < nSamples; i++)
	{
		addSample(assign[i], data + i * 3);
	}
	endLearning();

	free(assign);
	return 1;
}

int OCGMM::whichComponent(double *color)
{
	int k = 0;
	double max = 0;

	for(int ci = 0; ci < componentsCount; ci++)
	{
		double p = (*this)(ci, color);
		if(p > max)
		{
			k = ci;
			max = p;
		}
	}
	return k;
}

void OCGMM::initLearning()
{
	for(int ci = 0; ci < componentsCount; ci++)
	{
		sums[ci][0] = sums[ci][1] = sums[ci][2] = 0;
		prods[ci][0][0] = prods[ci][0][1] = prods[ci][0][2] = 0;
		prods[ci][1][0] = prods[ci][1][1] = prods[ci][1][2] = 0;
		prods[ci][2][0] = prods[ci][2][1] = prods[ci][2][2] = 0;
		sampleCounts[ci] = 0;
	}
	totalSampleCount = 0;
}

void OCGMM::addSample(int ci, double *color)
{
	sums[ci][0] += color[0]; sums[ci][1] += color[1]; sums[ci][2] += color[2];
	prods[ci][0][0] += color[0]*color[0]; prods[ci][0][1] += color[0]*color[1]; prods[ci][0][2] += color[0]*color[2];
	prods[ci][1][0] += color[1]*color[0]; prods[ci][1][1] += color[1]*color[1]; prods[ci][1][2] += color[1]*color[2];
	prods[ci][2][0] += color[2]*color[0]; prods[ci][2][1] += color[2]*color[1]; prods[ci][2][2] += color[2]*color[2];
	sampleCounts[ci]++;
	totalSampleCount++;
}

void OCGMM::endLearning()
{
	const double variance = 4.0f; //Bayesian Regularization:
	for(int ci = 0; ci < componentsCount; ci++)
	{
		int n = sampleCounts[ci];
		if(n == 0)
		{
			coefs[ci] = 0;
			eofLog[ci] = FLT_MAX;
		}
		else
		{
			coefs[ci] = (double) n / totalSampleCount;

			double *m = mean + 3 * ci;
			m[0] = sums[ci][0]/n; m[1] = sums[ci][1]/n; m[2] = sums[ci][2]/n;

			double *c = cov + 9*ci;	// Cov(x,y) = E( (x-E(X)) * (y-E(y)) ) = E(XY) - E(X)*E(Y)
			c[0] = prods[ci][0][0]/n - m[0]*m[0]; c[1] = prods[ci][0][1]/n - m[0]*m[1]; c[2] = prods[ci][0][2]/n - m[0]*m[2];
			c[3] = prods[ci][1][0]/n - m[1]*m[0]; c[4] = prods[ci][1][1]/n - m[1]*m[1]; c[5] = prods[ci][1][2]/n - m[1]*m[2];
			c[6] = prods[ci][2][0]/n - m[2]*m[0]; c[7] = prods[ci][2][1]/n - m[2]*m[1]; c[8] = prods[ci][2][2]/n - m[2]*m[2];

			double dtrm = c[0]*(c[4]*c[8]-c[5]*c[7]) - c[1]*(c[3]*c[8]-c[5]*c[6]) + c[2]*(c[3]*c[7]-c[4]*c[6]);
			if(dtrm < std::numeric_limits<double>::epsilon())
			{
				// Adds the white noise to avoid singular covariance matrix.
				c[0] += variance;
				c[4] += variance;
				c[8] += variance;
			}

			calcInverseCovAndDeterm(ci);
			eofLog[ci] = -log(coefs[ci]) + 0.5 * log(det[ci]);
		}
	}
}

void OCGMM::calcInverseCovAndDeterm(int ci)
{
	if(coefs[ci] > 0)
	{
		double *c = cov + 9*ci;
		double dtrm = covDeterms[ci] = c[0]*(c[4]*c[8]-c[5]*c[7]) - 
			c[1]*(c[3]*c[8]-c[5]*c[6]) + c[2]*(c[3]*c[7]-c[4]*c[6]);
		det[ci] = dtrm;
		//CV_Assert( dtrm > std::numeric_limits<double>::epsilon() );
		inverseCovs[ci][0][0] =  (c[4]*c[8] - c[5]*c[7]) / dtrm;
		inverseCovs[ci][1][0] = -(c[3]*c[8] - c[5]*c[6]) / dtrm;
		inverseCovs[ci][2][0] =  (c[3]*c[7] - c[4]*c[6]) / dtrm;
		inverseCovs[ci][0][1] = -(c[1]*c[8] - c[2]*c[7]) / dtrm;
		inverseCovs[ci][1][1] =  (c[0]*c[8] - c[2]*c[6]) / dtrm;
		inverseCovs[ci][2][1] = -(c[0]*c[7] - c[1]*c[6]) / dtrm;
		inverseCovs[ci][0][2] =  (c[1]*c[5] - c[2]*c[4]) / dtrm;
		inverseCovs[ci][1][2] = -(c[0]*c[5] - c[2]*c[3]) / dtrm;
		inverseCovs[ci][2][2] =  (c[0]*c[4] - c[1]*c[3]) / dtrm;
	}
}