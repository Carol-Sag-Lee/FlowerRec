#include <GrabCut.h>
#include <stdio.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
//#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <android/log.h>





using namespace cv;

	JNIEXPORT jintArray JNICALL Java_com_example_image_util_GrabCut_grabCut(JNIEnv* env, jclass obj,jintArray buf,jfloat width,
			jfloat height,jfloat preX,jfloat preY, jfloat x, jfloat y)
	{
		__android_log_write(ANDROID_LOG_ERROR,"Tag","function is called sucessfully");
		const Scalar RED = Scalar(0,0,255);
		const Scalar PINK = Scalar(230,130,255);
		const Scalar BLUE = Scalar(255,0,0);
		const Scalar LIGHTBLUE = Scalar(255,255,160);
		const Scalar GREEN = Scalar(0,255,0);

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
			__android_log_write(ANDROID_LOG_ERROR,"Tag","cbuf == NULL");
			return 0;
		}

		Mat image(height,width,CV_8UC3,(unsigned char*)cbuf);
		int size = width * height;
		Mat bgdModel(13*5,1,CV_32FC1,NULL);
		Mat fgdModel(13*5,1,CV_32FC1,NULL);
		Rect rect = Rect(preX,preY,x,y);
		Mat mask(height,width,CV_8UC1);
		Mat binMask(height,width,CV_8UC1);
		Mat res;

		__android_log_write(ANDROID_LOG_ERROR,"Tag","before function ");
		grabCut(image,mask,rect,bgdModel, fgdModel, 1,GC_INIT_WITH_RECT);

		__android_log_write(ANDROID_LOG_ERROR,"Tag","function is called sucessfully");
		/*
		 * getBinMask方法
		 */
		if(mask.empty()||mask.type() != CV_8UC1)
			CV_Error( CV_StsBadArg, "comMask is empty or has incorrect type (not CV_8UC1)" );
		if(binMask.empty() || binMask.rows!=mask.rows || binMask.cols!=mask.cols )
			binMask.create(mask.size(),CV_8UC1);
		binMask = mask & 1;

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
		__android_log_write(ANDROID_LOG_ERROR,"Tag","before return declaration ");
		jintArray result = env->NewIntArray(size);
		jint* intRes;
		for (jint i =0;i < size;i++)
		{
			intRes[i] = (int)res.data[i];

		}
		env->SetIntArrayRegion(result,0,size,intRes);
		__android_log_write(ANDROID_LOG_ERROR,"Tag","before return ");
		return result;
	}



