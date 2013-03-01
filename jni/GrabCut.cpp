#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

class GrabCut{
private:
	const Mat* image;
	Mat mask;
	Mat bgdModel, fgdModel;
	Rect rect;
	Mat bgdModel, fgdModel;
public:
	static GrabCut(const Mat& _image,Rect rect)
	{
		*this.image = _image;
		grabCut(*image,mask,rect,bgdModel, fgdModel, 1, GC_INIT_WITH_RECT);
	}

}
