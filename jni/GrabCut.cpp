#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <GrabCut.h>
#include <opencv2/opencv.hpp>
using namespace cv;

	JNIEXPORT jintArray JNICALL Java_com_example_image_util_GrabCut_grabCut(JNIEnv* env, jclass obj,jintArray buf,jfloat width,
			jfloat height,jint preX,jint preY, jint x, jint y)
	{


		jint *cbuf;
		cbuf = env->GetIntArrayElements(buf, false);

		if(cbuf == NULL)

		{
		return 0;
		}

		Mat image(height,width,CV_8UC3,(unsigned char*)cbuf);
		int size = width * height;
		Mat bgdModel(13*5,1,CV_32FC1,NULL);
		Mat fgdModel(13*5,1,CV_32FC1,NULL);
		Rect rect = Rect(preX,preY,x,y);
		Mat mask(height,width,CV_8UC1);
		grabCut(image,mask,rect,bgdModel, fgdModel, 1,0);


		jintArray result = env->NewIntArray(size);
		env->SetIntArrayRegion(result, 0, size, cbuf);
		env->ReleaseIntArrayElements(buf, cbuf, 0);
		return result;
	}
