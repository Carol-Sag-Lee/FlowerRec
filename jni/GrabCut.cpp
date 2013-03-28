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


		int radius = 2;
		int thickness = -1;
		uchar rectState, lblsState, prLblsState;
		bool isInitialized;
		vector<Point> fgdPxls, bgdPxls, prFgdPxls, prBgdPxls;
		int iterCount;
		jint *cbuf;
		cbuf = env->GetIntArrayElements(buf, JNI_FALSE);



		if(cbuf == NULL)
		{
			__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","cbuf is empty");
			return 0;
		}
		//判断cbuf是否为零,结果cbuf全部为负数 有
//		for (jint i =0;i < width*height;i++)
//		{
//
//			if(cbuf[i]>0)
//			{
//				__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","cbuf is greater than 0");
//			}
//			else if(cbuf[i]<0)
//			{
//				__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","cbuf is less than 0");
//			}
//			else
//			{
//				__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","cbuf is 0");
//			}
//		}

		__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","cbuf is not empty");
		Mat image(height,width,CV_8UC3,(unsigned char*)cbuf);
		int size = width * height;
		/*
		 * 只要是大的数据就会不能显示 死机 不要想写出所有数据来
		 */
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
		if(image.empty())//empty()为true -- 元素数目为0或者data为NULL
		{
			__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","image is empty");
		}
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

		//判断binMask中data是否为零,只有大于0和等于0
//		for (jint i =0;i < width*height;i++)
//		{
//			cbuf[i] = binMask.data[i];
//			if(cbuf[i]>0)
//			{
//				__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","cbuf is greater than 0");
//			}
//			else if(cbuf[i]<0)
//			{
//				__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","cbuf is less than 0");
//			}
//			else
//			{
//				__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","cbuf is 0");
//			}
//		}
		__android_log_write(ANDROID_LOG_INFO,"GrabCut.CPP","before showImage");
		/*
		 * showImage方法部分
		 */
		//image.copyTo(res,binMask);
		bgdPxls.clear(); fgdPxls.clear();
		prBgdPxls.clear(); prFgdPxls.clear();
		__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","before return declaration ");
        if(size >10000)
        {
		__android_log_write(ANDROID_LOG_ERROR,"GrabCut.CPP","size大于10000");
        }

		env->SetIntArrayRegion(buf, 0, size, cbuf);
		__android_log_write(ANDROID_LOG_ERROR,"Tag","before return ");
		if(size == 131705)
		{
			__android_log_write(ANDROID_LOG_ERROR,"Tag","size is 131705 ");
		}
		return buf;
	}



