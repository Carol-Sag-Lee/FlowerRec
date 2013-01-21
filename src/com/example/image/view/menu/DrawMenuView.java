package com.example.image.view.menu;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.PopupWindow;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.example.image.R;
import com.example.image.view.DrawView;
import com.example.image.view.ToneView;

public class DrawMenuView
{

	private PopupWindow mPopup;
	private DrawView mDrawView;
	private Context mContext;
	private boolean mIsShow;
	
	public DrawMenuView(Context context)
	{
		mContext = context;
	}
	
	public boolean show()
	{
		if (hide())
		{
			return false;
		}
		
		final Context context = mContext;
		mIsShow = true;
		
		mPopup = new PopupWindow(context);
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		
		mDrawView = new DrawView(context);
		View view = mDrawView.getParentView();
		view.setBackgroundResource(R.drawable.popup);
		view.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_DOWN)
				{
					hide();
				}
				return false;
			}
			
		});
		
		float density = metrics.density;
		mPopup.setWidth(metrics.widthPixels);
		mPopup.setHeight((int) (105 * density));
		mPopup.setContentView(view);
		mPopup.setFocusable(true);
		mPopup.setOutsideTouchable(true);
		mPopup.setTouchable(true);
		// 设置背景为null，就不会出现黑色背景，按返回键PopupWindow就会消失
		mPopup.setBackgroundDrawable(null);
		mPopup.showAtLocation(view, Gravity.BOTTOM | Gravity.CENTER, 0, 0);
		return true;
	}
	

	
	public boolean hide()
	{
		if (null != mPopup && mPopup.isShowing())
		{
			mIsShow = false;
			mPopup.dismiss();
			mPopup = null;
			return true;
		}
		return false;
	}
	
	/**
	 * 菜单是否是显示
	 * @return
	 */
	public boolean isShow()
	{
		return mIsShow;
	}
	
	public DrawView getDrawView()
	{
		return mDrawView;
	}
}
