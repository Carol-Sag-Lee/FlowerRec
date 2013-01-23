package com.example.image.view;

import java.util.ArrayList;

import com.example.image.EditImageActivity;
import com.example.image.R;
import com.example.image.view.DrawView;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.MenuInflater;
import com.example.image.util.EditImage;

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
    private Path path;
    public Paint paint = null;
    final int VIEW_WIDTH = 320;
    final int VIEW_HEIGHT = 480;
    // 定义一个内存中的图片，该图片将作为缓冲区
    Bitmap cacheBitmap = null;
    // 定义cacheBitmap上的Canvas对象
    Canvas cacheCanvas = null;
    
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
     // 创建一个与该View相同大小的缓存区
        cacheBitmap = Bitmap.createBitmap(VIEW_WIDTH
            , VIEW_HEIGHT , Config.ARGB_8888);
        cacheCanvas = new Canvas();
        path = new Path();
        // 设置cacheCanvas将会绘制到内存中的cacheBitmap上
        cacheCanvas.setBitmap(cacheBitmap);
        //设置画笔的颜色
        paint = new Paint(Paint.DITHER_FLAG);
        paint.setColor(Color.RED);
        //设置画笔风格
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        //反锯齿
        paint.setAntiAlias(true);
        paint.setDither(true);  
        emboss = new EmbossMaskFilter(new float[]  { 1.5f , 1.5f , 1.5f }, 0.6f , 6, 4.2f);
        blur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
    
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mBitmapDisplayed.getBitmap() != null) {
            for (HighlightView hv : mHighlightViews) {
                hv.mMatrix.set(getImageMatrix());
                hv.invalidate();
                if (hv.mIsFocused) {
                    centerBasedOnHighlightView(hv);
                }
            }
        }
    }


   
   
/*
 * 设置颜色，宽度，模糊，浮雕
 */
    public void setColor(int i)
    {
        switch(i) {
        case 0:
            paint.setColor(Color.RED);
            break;
        case 1:
            paint.setColor(Color.GREEN);
            break;
        case 2:
            paint.setColor(Color.BLUE);
            break;
        }
    }
    
    public void setWidth(int i)
    {
        switch(i) {
        case 0:
            paint.setStrokeWidth(1);
        case 1:
            paint.setStrokeWidth(3);
        case 2:
            paint.setStrokeWidth(5);
        }
    }
    
    public void setBlurMask() {
       paint.setMaskFilter(blur);
    }
    public void setEmbossMask() {
        paint.setMaskFilter(emboss);
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
    public boolean onTouchEvent(MotionEvent event) {
        EditImage cropImage = mCropImage;
        if (cropImage.mSaving) {
            return false;
        }
        //获取拖动事件的发生位置
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN: // CR: inline case blocks.
        	switch (mState)
        	{
        	case STATE_SUB_CROP:
        	      path.moveTo(x, y);
                  preX = x;
                  preY = y;       
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
        // CR: vertical space before case blocks.
        case MotionEvent.ACTION_UP:
        	switch (mState)
        	{
        	case STATE_NONE:
        		break;
        	case STATE_SUB_CROP:
        	    cacheCanvas.drawPath(path, paint);     
                path.reset();
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
        case MotionEvent.ACTION_MOVE:
        	switch (mState)
        	{
        	case STATE_NONE:
        		break;
        	case STATE_SUB_CROP:
        	    path.quadTo(preX , preY , x, y);
                preX = x;
                preY = y;
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
        }

        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            center(true, true);
            break;
        case MotionEvent.ACTION_MOVE:
            // if we're not zoomed then there's no point in even allowing
            // the user to move the image around. This call to center puts
            // it back to the normalized location (with false meaning don't
            // animate).
            if (getScale() == 1F) {
                center(true, true);
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
        switch (mState)
        {
        case STATE_NONE:
        	break;
        case STATE_SUB_CROP:
            Paint bmpPaint = new Paint();
            // 将cacheBitmap绘制到该View组件上
            canvas.drawBitmap(cacheBitmap , 0 , 0 , bmpPaint);    //
            // 沿着path绘制
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
