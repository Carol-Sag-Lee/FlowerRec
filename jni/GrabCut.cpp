#include <GrabCut.h>
#include <stdio.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <android/log.h>




using namespace std;
using namespace cv;

	JNIEXPORT jintArray JNICALL Java_com_example_image_util_GrabCut_grabCut(JNIEnv* env, jclass obj,jintArray buf,jfloat width,
			jfloat height,jfloat preX,jfloat preY, jfloat x, jfloat y)
	{
		__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","function is called sucessfully");
		const Scalar RED = Scalar(0,0,255);
		const Scalar PINK = Scalar(230,130,255);
		const Scalar BLUE = Scalar(255,0,0);
		const Scalar LIGHTBLUE = Scalar(255,255,160);
		const Scalar GREEN = Scalar(0,255,0);
		enum{
			GC_BGD    = 0,  //!< background
			GC_FGD    = 1,  //!< foreground
			GC_PR_BGD = 2,  //!< most probably background
			GC_PR_FGD = 3   //!< most probably foreground
		};

		int radius = 2;
		int thickness = -1;
		uchar rectState, lblsState, prLblsState;
		bool isInitialized;
		vector<Point> fgdPxls, bgdPxls, prFgdPxls, prBgdPxls;
		int iterCount;
		jint *cbuf;
		cbuf = env->GetIntArrayElements(buf, false);

		if(cbuf == NULL)
		{
			return 0;
		}
		Mat image(height,width,CV_8UC3,(unsigned char*)cbuf);
		int size = width * height;
		Mat bgdModel;
		Mat fgdModel;
		Rect rect = Rect(preX,preY,x,y);//rect = Rect( Point(rect.x, rect.y), Point(x,y) );设置rect的c++函数
		Mat mask;
		Mat binMask;
		Mat res;
		mask.create(height,width,CV_8UC1);
		mask.setTo(GC_BGD);
		rect.x = std::max((jfloat)0,preX);
		rect.y = std::max((jfloat)0,preY);
		rect.width = std::min(rect.width,image.cols-rect.x);
		rect.height = std::min (rect.height, image.cols-rect.y);
		(mask(rect)).setTo(Scalar(GC_PR_FGD));
		grabCut(image,mask,rect,bgdModel, fgdModel, 1,GC_INIT_WITH_RECT);
		/*
		 * getBinMask方法
		 */
		if(mask.empty()||mask.type() != CV_8UC1)
		{
			__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","mask is empty");
		}
		if(binMask.empty() || binMask.rows!=mask.rows || binMask.cols!=mask.cols ){
			binMask.create(height,width,CV_8UC1);

		}
		binMask = mask & 1;
		__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","before showImage");
		/*
		 * showImage方法部分
		 */
		image.copyTo(res,binMask);
		vector<Point>::const_iterator it;
				for( it = bgdPxls.begin(); it != bgdPxls.end(); ++it )
					circle( res, *it, radius, BLUE, thickness );//circle(image,point,radius,color,linetype)
				for( it = fgdPxls.begin(); it != fgdPxls.end(); ++it )
					circle( res, *it, radius, RED, thickness );
				for( it = prBgdPxls.begin(); it != prBgdPxls.end(); ++it )
					circle( res, *it, radius, LIGHTBLUE, thickness );
				for( it = prFgdPxls.begin(); it != prFgdPxls.end(); ++it )
					circle( res, *it, radius, PINK, thickness );
				rectangle( res, Point( rect.x, rect.y ), Point(rect.x + rect.width, rect.y + rect.height ), GREEN, 2);//rec(image,point1,point2,color,linetype)
		bgdPxls.clear(); fgdPxls.clear();
		prBgdPxls.clear(); prFgdPxls.clear();
		__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","before return declaration ");
        if(size >10000)
        {
		__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","size大于10000");
        }
		for (jint i =0;i < size;i++)
		{
			cbuf[i] = binMask.data[i];
		}
		env->SetIntArrayRegion(buf, 0, size, cbuf);
		__android_log_write(ANDROID_LOG_ERROR,"Tag","before return ");
		return buf;
	}



