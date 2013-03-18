package com.example.image.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class DrawView extends View
{
 
	float preX;
	float preY;
	float x;
	float y;

	public Path path;
	final int VIEW_WIDTH = 320;
	final int VIEW_HEIGHT = 480;
	Bitmap cacheBitmap = null;
	Canvas cacheCanvas = null;
    RectF mRect1 = new RectF();
   
    View contextView;
    public static final int MOTION_NONE = (1 << 0);
    public static final int MOTION_UP = (1 << 2);
    public int drawState = MOTION_NONE;
    private final Paint mPaint;
    public Matrix mMatrix;

	public DrawView(Context ct, View vw)
		{
	    super(ct);
        Log.d("drawview 监测", " DrawView  " );
    	    contextView = vw;
    	    cacheBitmap = Bitmap.createBitmap(VIEW_WIDTH
    	               , VIEW_HEIGHT , Config.ARGB_8888);
    	    cacheCanvas = new Canvas();
            path = new Path();
            mPaint = new Paint();
            cacheCanvas.setBitmap(cacheBitmap); 
            mPaint.setStrokeWidth(3F);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(Color.RED);
            mPaint.setAntiAlias(true); 
		}
	
	    public boolean mIsFocused;
	    boolean mHidden;

	    public boolean hasFocus() {
            Log.d("drawview 监测", " hasFocus  " );
	        return mIsFocused;
	    }

	    public void setFocus(boolean f) {
	        Log.d("drawview 监测", " setFocus  " );
	        mIsFocused = f;
	    }
	
	public void setHidden(boolean hidden) {
	    Log.d("drawview 监测", " setHidden  " );
        mHidden = hidden;
    }
	
        public float getPreX() {
            return this.preX;
        }
        
        public float getPreY() {
            return this.preY;
        }
       
        public float getX() {
            return this.x;
        }
        public float getY() {
            return this.y;
        }
	public void draw(Canvas canvas)
	{
		if(mHidden){
			return;
		}
	      canvas.save();
	    Log.d("drawview 监测", " draw  " );
		Paint bmpPaint = new Paint();
		canvas.drawBitmap(cacheBitmap , 0 , 0 , bmpPaint);    
		canvas.drawPath(path, bmpPaint);
		
	}
		
	public void onDraw(Canvas canvas)
    {
        if(mHidden){
            return;
        }
          canvas.save();
        Log.d("drawview 监测", " draw  " );
        Paint bmpPaint = new Paint();
        canvas.drawBitmap(cacheBitmap , 0 , 0 , bmpPaint);    
        canvas.drawPath(path, bmpPaint);
        
    }
	
	public boolean onTouchEvent(MotionEvent event) {
	    Log.d("drawview 监测", " onTouchEvent  " );
	    float x = event.getX();
	    float y = event.getY();
	    switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                preX = x;
                preY = y;               
                break;
            case MotionEvent.ACTION_MOVE:
                path.reset();
                mRect1.set(preX,preY,x,y);
                path.addRect(mRect1,Path.Direction.CW);
                break;
            case MotionEvent.ACTION_UP:
                cacheCanvas.drawPath(path, mPaint);  
                path.reset();
                break;
        }
        invalidate();

        return true;
	}
	public void handleDown(float x, float y){
	        Log.i("drawview监测","handleDown");
				path.moveTo(x, y);
				preX = x;
				preY = y;	
			    
	}
	
	public void handleMove(float x, float y){
	    Log.i("drawview监测","handleMove");
		path.reset();
		mRect1.set(preX,preY,x,y);
		path.addRect(mRect1,Path.Direction.CW);
	}
	
	public void handleUp()
	{
	    Log.i("drawview监测","handleUp");
		cacheCanvas.drawPath(path, mPaint);  
		path.reset();
		  Paint bmpPaint = new Paint();
		  cacheCanvas.drawBitmap(cacheBitmap , 0 , 0 , bmpPaint);    
		  cacheCanvas.drawPath(path, bmpPaint);
	}


    
}
