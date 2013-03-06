package com.example.image.view;

import java.util.ArrayList;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.MenuInflater;
import android.widget.Toast;
import android.graphics.PorterDuff;
import android.graphics.Region;


import com.example.image.util.EditImage;
import com.example.image.util.GrabCut;

public class CropImageView extends ImageViewTouchBase {
    public ArrayList<HighlightView> mHighlightViews = new ArrayList<HighlightView>();
    HighlightView mMotionHighlightView = null;
    float mLastX, mLastY;
    int mMotionEdge;
    private EditImage mCropImage;
    public ImageMoveView mMoveView;
    
    //画布
    float preX;
    float preY;
    
    int scX;
    int scY;
    
    float x;
    float y;

    public Paint paint = null;
    final int VIEW_WIDTH = 480;
    final int VIEW_HEIGHT = 800;
    public Path path;

    RectF mRect1 = new RectF();
    
    public static final int DRAWABLE = 0x0;
    public static final int UNDRAWABLE = 0x1;
    private int drawState =UNDRAWABLE ;
    
    /*
     * 微调参数
     * */
    private EmbossMaskFilter emboss;
    private BlurMaskFilter blur;
    
    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        /*
         * drawView设置
         */

        //设置画笔的颜色
        paint = new Paint(Paint.DITHER_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3);
        //反锯齿
        paint.setAntiAlias(true);
        paint.setDither(true);  
        emboss = new EmbossMaskFilter(new float[]  { 1.5f , 1.5f , 1.5f }, 0.6f , 6, 4.2f);
        blur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
        path = new Path();
        cacheCanvas = new Canvas();
    }
    
    @Override
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

public void setDrawState(int t) {
    drawState = t;
}
   

    
    @Override
    protected void zoomTo(float scale, float centerX, float centerY) {
        super.zoomTo(scale, centerX, centerY);
        for (HighlightView hv : mHighlightViews) {
            hv.mMatrix.set(getImageMatrix());
            hv.invalidate();
        }
    }

    @Override
    protected void zoomIn() {
        super.zoomIn();
        for (HighlightView hv : mHighlightViews) {
            hv.mMatrix.set(getImageMatrix());
            hv.invalidate();
        }
    }

    @Override
    protected void zoomOut() {
        super.zoomOut();
        for (HighlightView hv : mHighlightViews) {
            hv.mMatrix.set(getImageMatrix());
            hv.invalidate();
        }
    }

    @Override
    protected void postTranslate(float deltaX, float deltaY) {
        super.postTranslate(deltaX, deltaY);
        for (int i = 0; i < mHighlightViews.size(); i++) {
            HighlightView hv = mHighlightViews.get(i);
            hv.mMatrix.postTranslate(deltaX, deltaY);
            hv.invalidate();
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
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        EditImage cropImage = mCropImage;
        if (cropImage.mSaving) {
            return false;
        }
        //获取拖动事件的发生位置
         x = event.getX();
         y = event.getY();
         scX = (int)x;
         scY = (int)y;
        
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN: // CR: inline case blocks.
        	switch (mState)
        	{
        	case STATE_SUB_CROP:
        	    if(drawState == DRAWABLE ) {
        	        path.moveTo(x, y);
        	        preX = x;
        	        preY = y;
        	    }
        	    invalidate();
        		break;
        	case STATE_HIGHLIGHT:
        		if (cropImage.mWaitingToPick) {
        			recomputeFocus(event);
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
        					break;
        				}
        			}
        		}
        		break;
        	}
            break;
       
        case MotionEvent.ACTION_MOVE:
        	switch (mState)
        	{
        	case STATE_NONE:
        		break;
        	case STATE_SUB_CROP:
        	    if(drawState == DRAWABLE) {
        	        path.reset();
        	       mRect1.set(preX, preY, x, y);
        	       path.addRect(mRect1, Path.Direction.CW);
        	       invalidate();
                    break;
        	    }
                break;
        	case STATE_HIGHLIGHT:
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
        	}
        	
            break;
            // CR: vertical space before case blocks.
        case MotionEvent.ACTION_UP:
            switch (mState)
            {
            case STATE_NONE:
                break;
            case STATE_SUB_CROP:
                if( drawState == DRAWABLE) {
                    cacheCanvas.drawPath(path, paint);
                    path.reset();
                    invalidate();
                    int w=mBitmapDisplayed.getWidth(),h=mBitmapDisplayed.getHeight();  
                    int[] pixels = new int[w*h];
                    mBitmapDisplayed.getPixels(pixels, 0, w, 0, 0, w, h);
                    Log.d("PIXELS IS EMPTY",""+(pixels==null));
                    int[] resultImg =  GrabCut.grabCut(pixels, w, h, (int)preX, (int)preY,(int) x, (int)y );
                    Log.d("RESULT PIXELS IS EMPTY",""+(resultImg==null));
                    Bitmap resultImgBit=Bitmap.createBitmap(w, h, Config.RGB_565);  
                    resultImgBit.setPixels(resultImg, 0, w, 0, 0, w, h);
                    mBitmapDisplayed = resultImgBit;
                   
                    break;
                }
                break;
            case STATE_HIGHLIGHT:
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
            
            break;
        }
        invalidate();
        return true;
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

    // If the cropping rectangle's size changed significantly, change the
    // view's center and scale according to the cropping rectangle.
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
        canvas.drawBitmap(mBitmapDisplayed, 0 , 0 ,paint);    //
        switch (mState)
        {
        case STATE_NONE:
        	break;
        case STATE_SUB_CROP:
            if(drawState == DRAWABLE) {
            // 沿着path绘制
            canvas.drawPath(path, paint);
            }
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
