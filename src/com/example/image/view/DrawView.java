package com.example.image.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DrawView extends View
{
	float preX;
	float preY;
	private Path path;
	public Paint paint = null;
	final int VIEW_WIDTH = 320;
	final int VIEW_HEIGHT = 480;
	Bitmap cacheBitmap = null;
	Canvas cacheCanvas = null;
	
	private TextView mColor; //颜色
	private TextView mWidth; //宽度
	private TextView mBlur; //模糊
    private TextView mEmboss; //浮雕
    
    private LinearLayout mParent;
	public DrawView(Context context)
	{
	    super(null, null, 1);
	   
		cacheBitmap = Bitmap.createBitmap(VIEW_WIDTH
			, VIEW_HEIGHT , Config.ARGB_8888);
		cacheCanvas = new Canvas();
		path = new Path();
	
		cacheCanvas.setBitmap(cacheBitmap);
	
		paint = new Paint(Paint.DITHER_FLAG);
		paint.setColor(Color.RED);

		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(1);
		
		paint.setAntiAlias(true);
		paint.setDither(true);	
		
		mParent = new LinearLayout(context);
        mParent.setOrientation(LinearLayout.VERTICAL);
        mParent.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mParent.addView(mColor);
        mParent.addView(mBlur);
        mParent.addView(mEmboss);
    }
	
	public View getParentView()
    {
        return mParent;
    }

	public boolean onTouchEvent(MotionEvent event)
	{
	
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
				path.quadTo(preX , preY , x, y);
				preX = x;
				preY = y;
				break;
			case MotionEvent.ACTION_UP:
				cacheCanvas.drawPath(path, paint);    
				path.reset();
				break;
		}
		invalidate();
	
		return true;
	}	

	public void onDraw(Canvas canvas)
	{
		Paint bmpPaint = new Paint();

		canvas.drawBitmap(cacheBitmap , 0 , 0 , bmpPaint);    
	
		canvas.drawPath(path, paint);
	}
}
