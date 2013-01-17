package com.example.image.util;

import java.util.ArrayList;
public class OCGMM {

    public 
        static final int componentsCount = 6;
        static final int nMaxSampleNum = 1800;
        static final int nMinSampleNum = 1000;
        private static final double FLT_MAX = 3.402823466e+38F ;
        ArrayList<Double> coefs = new ArrayList<Double>();
        ArrayList<Double> mean = new ArrayList<Double>();
        ArrayList<Double> cov = new ArrayList<Double>();

        double[][][] inverseCovs= new double[componentsCount][3][3];
        double[] covDeterms= new double[componentsCount];

        double[][] sums= new double[componentsCount][3];
        double[][][] prods = new double[componentsCount][3][3];
        double[] eofLog = new double[componentsCount];
        double[] det = new double[componentsCount];
        int[] sampleCounts = new int[componentsCount];
        int totalSampleCount;
        
        
        
        double  bracket(ArrayList<Double>color) {
            double res = 0;

            for(int ci = 0; ci < componentsCount; ci++ )
            {
                res += coefs.get(ci) * bracket(ci, color);
            }
            return res;
        }
        
        double bracket(int ci, ArrayList<Double> color) {
            double res = 0;
            double[] diff = new double[3];
            ArrayList<Double> m = new ArrayList<Double>();

            if(coefs.get(ci) > 0)
            {
                //CV_Assert( covDeterms[ci] > std::numeric_limits<double>::epsilon() );                     
                m = (ArrayList<Double>) mean.subList(3*ci, mean.size());
                diff[0] = color.get(0) - m.get(0); diff[1] = color.get(1) - m.get(1); diff[2] = color.get(2) - m.get(2);
                double mult = diff[0] * (diff[0] * inverseCovs[ci][0][0] + diff[1]*inverseCovs[ci][1][0] + diff[2]*inverseCovs[ci][2][0])
                    + diff[1]*(diff[0]*inverseCovs[ci][0][1] + diff[1]*inverseCovs[ci][1][1] + diff[2]*inverseCovs[ci][2][1])
                    + diff[2]*(diff[0]*inverseCovs[ci][0][2] + diff[1]*inverseCovs[ci][1][2] + diff[2]*inverseCovs[ci][2][2]);
                res = 1.0 / Math.sqrt(covDeterms[ci]) * Math.exp(-0.5 * mult);
            }
            return res;
        }

        int initialize(ArrayList<Double> data, int nSamples) {
            int i, j, k, iter,  changed;
            ArrayList<Integer> assign =new ArrayList<Integer>() ,oldassign = new ArrayList<Integer>();
            double dist, minDist = 0;
            double[] diff =new double[3];

            oldassign = (ArrayList<Integer>) assign.subList(nSamples, assign.size()) ;
            assign.clear();
            mean.clear();
            // random select initial central
            j = 0;
            for(int ii = 0; ii < nSamples; ii++)
            {
                int idx = (int) (Math.random() % nSamples);
                boolean diff_bool= true;

                for(int kk = 0; kk < j; kk++)
                {
                    double delta = Math.abs(mean.get(kk * 3 + 0) - data.get(idx * 3 + 0))
                                    + Math.abs(mean.get(kk * 3 + 1) - data.get(idx * 3 + 1))
                                    + Math.abs(mean.get(kk * 3 + 2) - data.get(idx * 3 + 2));
                    if(delta < 20){ // omit those pixel without much difference
                        diff_bool = false;
                        break;
                    }
                }
                if(diff_bool)
                {
                    for(int index=0;index<=2; index++)
                    {
                        mean.set(index+j*3, data.get(index+idx*3)) ;
                    }
                   // memcpy(mean + j * 3, data + idx * 3, sizeof(double) * 3);
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
                        diff[0] = data.get(i * 3 + 0) - mean.get(j * 3 + 0);
                        diff[1] = data.get(i * 3 + 1) - mean.get(j * 3 + 1);
                        diff[2] = data.get(i * 3 + 2) - mean.get(j * 3 + 2);
                        dist = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
                        if(k == -1 || dist < minDist)
                        {
                            k = j;
                            minDist = dist;
                        }
                    }
                    assign.set(i, k);
                    if(oldassign.get(i) != assign.get(i))
                        changed++;
                }

                if(changed == 0) //is converged?
                {
                    break;
                }
                /*--memcpy(oldassign, assign, sizeof(int) * nSamples);--*/
                for( int iii=0;iii<=nSamples;iii++)
                {
                    oldassign.set(iii, assign.get(iii));
                }
                //update means
             /* --memset(coefs, 0, sizeof(double) * componentsCount);
                memset(mean, 0, sizeof(double) * componentsCount * 3);--*/
                coefs.clear();
                mean.clear();
                for(i = 0; i < nSamples; i++)
                {
                    k = assign.get(i);
//                    coefs[k]++;
//                    mean[k * 3 + 0] += data[i * 3 + 0];
//                    mean[k * 3 + 1] += data[i * 3 + 1];
//                    mean[k * 3 + 2] += data[i * 3 + 2];
                    coefs.set(k,coefs.get(k)+1);
                }
                
/*-                    mean[i * 3 + 0] /= coefs[i];
----                   mean[i * 3 + 1] /= coefs[i];
----                   mean[i * 3 + 2] /= coefs[i];
-*/ 
                for(i = 0; i < componentsCount; i++)
                {

                    Double tmp1 = mean.get(i * 3 + 0)/coefs.get(i);
                    mean.set(i * 3 + 0, tmp1);
                    Double tmp2 = mean.get(i * 3 + 1)/coefs.get(i);
                    mean.set(i * 3 + 1, tmp2);
                    Double tmp3 = mean.get(i * 3 + 2)/coefs.get(i);
                    mean.set(i * 3 + 2, tmp3);
                    
                }
            }

            initLearning();
            for(i = 0; i < nSamples; i++)
            {
                addSample(assign.get(i), (ArrayList<Double>) data.subList(i * 3, data.size()-1) );
            }
            endLearning();

            assign.clear();
            return 1;
        }

            void endLearning() {

                final double variance = 4.0f; //Bayesian Regularization:
                for(int ci = 0; ci < componentsCount; ci++)
                {
                    int n = sampleCounts[ci];
                    if(n == 0)
                    {
                        coefs.set(ci, 0.0);
                        eofLog[ci] = FLT_MAX;
                    }
                    else
                    {
                      //  coefs[ci] = (double) n / totalSampleCount;
                        coefs.set(ci, (double) n / totalSampleCount);
                    //    double *m = mean + 3 * ci;
                    //    m[0] = sums[ci][0]/n; m[1] = sums[ci][1]/n; m[2] = sums[ci][2]/n;
                        double[] m = new double[3];
                        m[0] = sums[ci][0]/n; m[1] = sums[ci][1]/n; m[2] = sums[ci][2]/n;
                    //   double *c = cov + 9*ci; // Cov(x,y) = E( (x-E(X)) * (y-E(y)) ) = E(XY) - E(X)*E(Y)
                        double[] c = new double[9];
                        c[0] = prods[ci][0][0]/n - m[0]*m[0]; c[1] = prods[ci][0][1]/n - m[0]*m[1]; c[2] = prods[ci][0][2]/n - m[0]*m[2];
                        c[3] = prods[ci][1][0]/n - m[1]*m[0]; c[4] = prods[ci][1][1]/n - m[1]*m[1]; c[5] = prods[ci][1][2]/n - m[1]*m[2];
                        c[6] = prods[ci][2][0]/n - m[2]*m[0]; c[7] = prods[ci][2][1]/n - m[2]*m[1]; c[8] = prods[ci][2][2]/n - m[2]*m[2];

                        double dtrm = c[0]*(c[4]*c[8]-c[5]*c[7]) - c[1]*(c[3]*c[8]-c[5]*c[6]) + c[2]*(c[3]*c[7]-c[4]*c[6]);
                        if(dtrm < 2.22045e-016)//std::numeric_limits<double>::epsilon()
                        {
                            // Adds the white noise to avoid singular covariance matrix.
                            c[0] += variance;
                            c[4] += variance;
                            c[8] += variance;
                        }

                        calcInverseCovAndDeterm(ci);
                        eofLog[ci] = -Math.log(coefs.get(ci)) + 0.5 * Math.log(det[ci]);
                    }
                }
            
        }

        void initLearning() {
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
        
        void addSample(int ci, ArrayList<Double> color )
        {
            sums[ci][0] += color.get(0); sums[ci][1] += color.get(1); sums[ci][2] += color.get(2);
            prods[ci][0][0] += color.get(0)*color.get(0); prods[ci][0][1] += color.get(0)*color.get(1); prods[ci][0][2] += color.get(0)*color.get(2);
            prods[ci][1][0] += color.get(1)*color.get(0); prods[ci][1][1] += color.get(1)*color.get(1); prods[ci][1][2] += color.get(1)*color.get(2);
            prods[ci][2][0] += color.get(2)*color.get(0); prods[ci][2][1] += color.get(2)*color.get(1); prods[ci][2][2] += color.get(2)*color.get(2);
            sampleCounts[ci]++;
            totalSampleCount++;
        }
        
        void calcInverseCovAndDeterm(int ci)
        {
            if(coefs.get(ci) > 0)
            {
                //double *c = cov + 9*ci;
                double[] c = new double[9];
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
        
        double GetDWeight(double *color)
}
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
}
