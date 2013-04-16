package com.example.image.view;

import java.util.ArrayList;
import java.util.Vector;

import sun.swing.plaf.synth.Paint9Painter.PaintType;

import com.example.image.EditImageActivity;
import com.example.image.R;



import android.content.Context;
import android.graphics.AvoidXfermode.Mode;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import android.view.MotionEvent;


import com.example.image.util.EditImage;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class CropImageView extends ImageViewTouchBase {
    public ArrayList<HighlightView> mHighlightViews = new ArrayList<HighlightView>();
    public  HighlightView mMotionHighlightView = null;
   
    float mLastX, mLastY;
    int mMotionEdge;
    private EditImage mCropImage;
    public ImageMoveView mMoveView;
    
    //画布
    int preX;
    int preY;
    public Path path;
    int x;
    int y;
    public int imageW;
    public int imageH;
    
    public Paint paint = null;
    final int VIEW_WIDTH = 480;
    final int VIEW_HEIGHT = 800;


    RectF mRect1 = new RectF();
    
    public static final int DRAWABLE = 0x5;
    public static final int UNDRAWABLE = DRAWABLE + 1;
	private static final int NOT_SET = DRAWABLE + 2;
	private static final int IN_PROCESS = DRAWABLE + 3;
	private static final int SET = DRAWABLE + 4 ;
    private int drawState =UNDRAWABLE ;
    
    public org.opencv.core.Rect rect;
    public Mat image;
    public Mat mask;
    public Mat res;
    public Mat binMask;
   
    /*
     * 微调参数
     * */

	private int lblsState;
	private int rectState;
	public boolean isInitialized;
	Vector<Point> fgdPxls, bgdPxls, prFgdPxls, prBgdPxls;
	private int prLblsState;
    
    
    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //画笔设置
        paint = new Paint(Paint.DITHER_FLAG);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setDither(true);  
        //背景设置
        cacheBitmap = Bitmap.createBitmap(VIEW_WIDTH , VIEW_HEIGHT , Config.ARGB_8888);
        cacheCanvas = new Canvas();
        cacheCanvas.setBitmap(cacheBitmap);
        path = new Path();
        Log.i("cropimageview", "cropimageview中初始化");
      	isInitialized = false;
    	rectState = NOT_SET;
    	lblsState = NOT_SET;
    	prLblsState = NOT_SET;
        /**
         * 显示view大小
         */
//        int w = 0;
//        int h = 0;
//        int[] loc = {w,h};
//        this.getLocationInWindow(loc);
//        rect = new org.opencv.core.Rect();
//        Log.i("cropimageview", "cropimageview在窗口中的坐标"+loc[0]+":"+loc[1]);    
    }
    


    

	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (cacheBitmap != null) {
            for (HighlightView hv : mHighlightViews) {
                hv.mMatrix.set(getImageMatrix());
                hv.invalidate();
                if (hv.mIsFocused) {
                    centerBasedOnHighlightView(hv);
                }
            }
            
        }
    }
 
   

    // According to the event's position, change the focus to the first
    // hitting cropping rectangle.
    private void recomputeFocus(MotionEvent event) {
        for (int i = 0; i < mHighlightViews.size(); i++) {
            HighlightView hv = mHighlightViews.get(i);
            hv.setFocus(false);
            hv.invalidate();
        }

        for (int i = 0; i < mHighlightViews.size(); i++) {
            HighlightView hv = mHighlightViews.get(i);
            int edge = hv.getHit(event.getX(), event.getY());
            if (edge != HighlightView.GROW_NONE) {
                if (!hv.hasFocus()) {
                    hv.setFocus(true);
                    hv.invalidate();
                }
                break;
            }
        }
        invalidate();
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        super.setImageBitmap(bitmap);
        Drawable d = getDrawable();
        if (d != null) {
            d.setDither(true);
        }
        // 设置cacheCanvas将会绘制到内存中的mBitmapDisplayed上
      mBitmapDisplayed = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);
      cacheCanvas.drawBitmap(mBitmapDisplayed,0,0,null);
      imageW = bitmap.getWidth();
      imageH = bitmap.getHeight();
      int size =imageH*imageW;
      image = new Mat(imageH,imageW,CvType.CV_8UC3,new Scalar(0));
      int[] dstPixels = new int[size];  
      bitmap.getPixels(dstPixels , 0, imageW, 0, 0, imageW, imageH);
      double[] imgC3 = new double[3];
      for(int i=0;i<imageH;i++)
      {
    	  for(int j=0;j<imageW;j++)
    	  {
    		int ji= (i+1)*(j+1)-1;
    		//RGB值存储顺序
    		imgC3[0] = dstPixels[ji]&0X00FF0000;//R值
    		imgC3[1] = dstPixels[ji]&0X0000FF00;//G值
    		imgC3[2] = dstPixels[ji]&0X000000FF;//B值
    	    image.put(i, j, imgC3);
    	  }
    	    
      }
    
      mask = new Mat(imageH,imageW,CvType.CV_8UC1,new Scalar(0));
      binMask = new Mat(imageH,imageW,CvType.CV_8UC1,new Scalar(0));
     
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        EditImage cropImage = mCropImage;
        if (cropImage.mSaving) {
            return false;
        }
        //获取拖动事件的发生位置
         x =(int) event.getX();
         y = (int) event.getY();
        
        switch(mState) {
        case STATE_SUB_CROP:
			switch(event.getAction()) {
        	  case MotionEvent.ACTION_DOWN:
//        	      Log.i("cropimageview","");
        	      preX = x;
        	      preY = y;
        	      invalidate();
        	    
        	      if( rectState == NOT_SET )
      			{
      				rectState = IN_PROCESS;
      				rect = new org.opencv.core.Rect(preX,preY,x,y);
      			}
      			if ( rectState == SET )
      			{
      				lblsState = IN_PROCESS;
      			}
        	      break;
        	  case MotionEvent.ACTION_MOVE:
        	      Log.i("cropimageview","path.quadTo");
        	      mRect1.set(preX, preY, x, y);
//        	      path.addRect(mRect1, Path.Direction.CW);   
//        	      path.reset();
        	  	if( rectState == IN_PROCESS )
        		{
        			rect.x = Math.max(0, preX);
        			rect.y = Math.max(0,preY);
        			rect.width = Math.min(x-rect.x, image.cols()-rect.x);
        			rect.height = Math.min(y-rect.y, image.rows()-rect.y);
        			assert( bgdPxls.isEmpty() && fgdPxls.isEmpty() && prBgdPxls.isEmpty() && prFgdPxls.isEmpty() );
        			showImage();
        		}
        		else if( lblsState == IN_PROCESS )
        		{
        			setLblsInMask(new Point(x,y), false);
        			showImage();
        		}
        		else if( prLblsState == IN_PROCESS )
        		{
        			setLblsInMask(new Point(x,y), true);
        			showImage();
        		}
        	      invalidate();
        	      break;
        	  case MotionEvent.ACTION_UP:
        	      Log.i("cropimageview","path.reset");
        	      cacheCanvas.drawRect(mRect1, paint);  
        	  	if( rectState == IN_PROCESS )
        		{	
        	  		rect.x = Math.max(0, preX);
        			rect.y = Math.max(0,preY);
        			rect.width = Math.min(x-rect.x, image.cols()-rect.x);
        			rect.height = Math.min(y-rect.y, image.rows()-rect.y);        		
        			rectState = SET;
        			setRectInMask();
           			showImage();
        		}
        		if( lblsState == IN_PROCESS )
        		{
        			setLblsInMask(new Point(x,y), true);
        			lblsState = SET;
        			showImage();
        		}
//        	      path.reset();
        	      invalidate();
        	      
        	      break;
        	  }
        	        break;
        case STATE_HIGHLIGHT:
             switch (event.getAction()) { 
             case MotionEvent.ACTION_DOWN:
                 if (cropImage.mWaitingToPick) {
                     recomputeFocus(event);//setFocus
                 } else {
                     for (int i = 0; i < mHighlightViews.size(); i++) { // CR:
                         // iterator
                         // for; if
                         // not, then
                         // i++ =>
                         // ++i.
                         HighlightView hv = mHighlightViews.get(i);
                         int edge = hv.getHit(event.getX(), event.getY());
                         // 如果是按住了选中框，则变换模�?
                         if (edge != HighlightView.GROW_NONE) {
                             mMotionEdge = edge;
                             mMotionHighlightView = hv;
                             mLastX = event.getX();
                             mLastY = event.getY();
                             // CR: get rid of the extraneous parens below.
                             mMotionHighlightView.setMode((edge == HighlightView.MOVE) ? HighlightView.ModifyMode.Move
                                     : HighlightView.ModifyMode.Grow);
                            
                         }
                     }
                 }
                 break;
                    case MotionEvent.ACTION_MOVE:
                        if (cropImage.mWaitingToPick) {
                            recomputeFocus(event);
                        } else if (mMotionHighlightView != null) {
                            mMotionHighlightView.handleMotion(mMotionEdge, event.getX() - mLastX, event.getY() - mLastY);
                            mLastX = event.getX();
                            mLastY = event.getY();
                            
                            if (true) {
                                // This section of code is optional. It has some user
                                // benefit in that moving the crop rectangle against
                                // the edge of the screen causes scrolling but it means
                                // that the crop rectangle is no longer fixed under
                                // the user's finger.
                                ensureVisible(mMotionHighlightView);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (cropImage.mWaitingToPick) {
                            for (int i = 0; i < mHighlightViews.size(); i++) {
                                HighlightView hv = mHighlightViews.get(i);
                                if (hv.hasFocus()) {
                                    cropImage.mCrop = hv;
                                    for (int j = 0; j < mHighlightViews.size(); j++) {
                                        if (j == i) { // CR: if j != i do your shit; no need
                                            // for continue.
                                            continue;
                                        }
                                        mHighlightViews.get(j).setHidden(true);
                                    }
                                    centerBasedOnHighlightView(hv);
                                    cropImage.mWaitingToPick = false;
                                    return true;
                                }
                            }
                        } else if (mMotionHighlightView != null) {
                            centerBasedOnHighlightView(mMotionHighlightView);
                            mMotionHighlightView.setMode(HighlightView.ModifyMode.None);
                        }
                        mMotionHighlightView = null;
                        break;
             }
        }
        return true;
    }
                 
        private void setLblsInMask(Point p, boolean isPr) {
        	Vector<Point> bpxls, fpxls;
        	char bvalue, fvalue;
        	if( !isPr ) //
        	{
        		bpxls = bgdPxls;
        		fpxls = fgdPxls;
        		bvalue = Imgproc.GC_BGD;
        		fvalue = Imgproc.GC_FGD;
        	}
        	else//
        	{
        		bpxls = prBgdPxls;
        		fpxls = prFgdPxls;
        		bvalue = Imgproc.GC_PR_BGD;
        		fvalue = Imgproc.GC_PR_FGD;
        	}
        	
		
	}


		public void showImage() {
		
			res = new Mat(imageH,imageW,CvType.CV_8UC3,new Scalar(0));
			if( !isInitialized )
			{
				image.copyTo( res );
			}
			else
			{
				getBinMask();
				image.copyTo( res, mask );
			}
			
	}


		private void getBinMask() {
			
			double[] tmpM = new double[1];
			for(int i = 0;i<imageH;i++)
			{
				for(int j =0;j<imageW;j++)
				{
					tmpM = mask.get(i,j);
					tmpM[0]= (int)tmpM[0]&0X1;
					binMask.put(i,j,tmpM);
				}
				
			}
		}


		private void setRectInMask() {
			mask.setTo( new Scalar(Imgproc.GC_BGD) );
			if(rect.y >= 0)
			{
				Log.i("cropimageview", "rect.y >= 0");
			}
			if(rect.y <= (rect.y+rect.height))
			{
				Log.i("cropimageview", "rect.y <= (rect.y+rect.height)");
			}
			if((rect.y+rect.height) <= mask.cols())
			{
				Log.i("cropimageview", "(rect.y+rect.height) <= mask.cols()"+ (int)( rect.y+rect.height) +","+mask.cols());
			}
		
			getBinMask();
			binMask = new Mat(binMask.clone(),rect);
			Mat value = new Mat(imageH,imageW,CvType.CV_8UC3,new Scalar(Imgproc.GC_FGD));
			mask.setTo(value,binMask);
			
		}    
   






	// Pan the displayed image to make sure the cropping rectangle is visible.
    private void ensureVisible(HighlightView hv) {
        Rect r = hv.mDrawRect;

        int panDeltaX1 = Math.max(0, getLeft() - r.left);
        int panDeltaX2 = Math.min(0, getRight() - r.right);

        int panDeltaY1 = Math.max(0, getTop() - r.top);
        int panDeltaY2 = Math.min(0, getBottom() - r.bottom);

        int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
        int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

        if (panDeltaX != 0 || panDeltaY != 0) {
            panBy(panDeltaX, panDeltaY);
        }
    }

    /*
    矩形大小改变太多时，根据切割矩形改变view的中心和尺寸If the cropping rectangle's size changed significantly, change the
     view's center and scale according to the cropping rectangle.
     */
    private void centerBasedOnHighlightView(HighlightView hv) {
        Rect drawRect = hv.mDrawRect;

        float width = drawRect.width();
        float height = drawRect.height();

        float thisWidth = getWidth();
        float thisHeight = getHeight();

        float z1 = thisWidth / width * .6F;
        float z2 = thisHeight / height * .6F;

        float zoom = Math.min(z1, z2);
        zoom = zoom * this.getScale();
        zoom = Math.max(1F, zoom);

        if ((Math.abs(zoom - getScale()) / zoom) > .1) {
            float[] coordinates = new float[] { hv.mCropRect.centerX(), hv.mCropRect.centerY() };
            getImageMatrix().mapPoints(coordinates);
            zoomTo(zoom, coordinates[0], coordinates[1], 300F); // CR: 300.0f.
        }

        ensureVisible(hv);
    }

    
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint bmpPaint = new Paint();  
        canvas.drawPath(path, bmpPaint);
        canvas.drawBitmap(cacheBitmap, 0 , 0 ,bmpPaint);    //
     
        switch (mState)
        {
        case STATE_NONE:
        	break;
        case STATE_SUB_CROP:
            canvas.drawPath(path, paint);
            break;
        case STATE_HIGHLIGHT:
        	for (int i = 0; i < mHighlightViews.size(); i++) {
        		mHighlightViews.get(i).draw(canvas);
        	}
        	break;
        }
    }

    public void add(HighlightView hv) {
        mHighlightViews.add(hv);
        invalidate();
    }
    
  
    
    
    public void hideHighlightView()
    {
    	for (int i = 0, size = mHighlightViews.size(); i < size; i++)
    	{
    		mHighlightViews.get(i).setHidden(true);
    	}
    	invalidate();
    }
    
  
    
    public void setEditImage(EditImage cropImage)
    {
    	mCropImage = cropImage;
    }
    
    /**
     * 设置是剪切状态还是涂�?
     * @param doodle
     */
    public void setState(int state)
    {
    	mState = state;
    }
    
    public void setMoveView(ImageMoveView moveView)
    {
    	mMoveView = moveView;
    	invalidate();
    }
    
}
