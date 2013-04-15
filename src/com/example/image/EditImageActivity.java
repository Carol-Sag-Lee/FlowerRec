package com.example.image;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.example.image.util.EditImage;
import com.example.image.util.ImageFrameAdder;
import com.example.image.util.ImageSpecific;
import com.example.image.view.CropImageView;
import com.example.image.R;
import com.example.image.util.ReverseAnimation;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import com.example.image.view.menu.MenuView;
import com.example.image.view.menu.OnMenuClickListener;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;



@SuppressLint("NewApi")
public class EditImageActivity extends Activity {

	public boolean mWaitingToPick; // Whether we are wait the user to pick a face.
    public boolean mSaving; // Whether the "save" button is already clicked.
    
    private Handler mHandler = null;
    private ProgressDialog mProgress;
    private ProgressDialog mProgressDialog;
	private TextView mShowHandleName;
	private Bitmap mBitmap;
	private byte[] mContent;
	/**
	 * 临时保存
	 */
	private Bitmap mTmpBmp;
	
	private CropImageView mImageView;
	private EditImage mEditImage;
	private ImageFrameAdder mImageFrame;
	private ImageSpecific mImageSpecific;
	public static Handler subCropHandler = new Handler();
	/**
     * 一级菜单
     */
    private MenuView mMenuView;
    //细节修改
    private final int[] CROP_IMAGES = new int[] { R.drawable.ic_menu_crop, R.drawable.ic_menu_rotate_left };
    private final int[] CROP_TEXTS = new int[] { R.string.crop };
    
    private final int[] EDIT_IMAGES = new int[] { R.drawable.ic_menu_crop, R.drawable.ic_menu_rotate_left, R.drawable.ic_menu_mapmode, R.drawable.btn_rotate_horizontalrotate };
    private final int[] EDIT_TEXTS = new int[] { R.string.color, R.string.width, R.string.blur, R.string.emboss };
    
    private final int[] RETRIEVAL_IMAGES = new int[] { R.drawable.ic_menu_crop };
    private final int[] RETRIEVAL_TEXTS = new int[] { R.string.retrieval };
    


    
	
	private static final int EDIT_IMAGE_FROM_CAMERA = 0x4;
	

    /** 调色 */
    private final int FLAG_COLOR = 0x1;
    /** 调宽度 */
    private final int FLAG_WIDTH = FLAG_COLOR + 1;
    /** 调模糊度 */
    private final int FLAG_BLUR = FLAG_COLOR + 2;
    /** 浮雕效果 */
    private final int FLAG_EMBOSS = FLAG_COLOR + 3;
    /** 粗调 */
    private final int FLAG_CROP = FLAG_COLOR + 4;
    /** 微调 */
    private final int FLAG_SUB_CROP = FLAG_COLOR + 5;
    /** 搜索 */
    private final int FLAG_RETRIEVAL= FLAG_COLOR + 6;
  
    	private View mSaveAll;
	private View mSaveStep;
	private final int STATE_CROP = 0x1;
    private final int STATE_SUB_CROP = STATE_CROP <<1;
    private final int STATE_RETRIEVAL = STATE_CROP <<2;
    private final int STATE_NONE= STATE_CROP <<3;
    private final int STATE_REVERSE = STATE_CROP <<4;
    private int mState;

    public static String path;
    
	private Bitmap tmpContour;
	
	/**
	 * 反转动画
	 */
	private ReverseAnimation mReverseAnim;
	private int mImageViewWidth;
	private int mImageViewHeight;



	

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	  
		mHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				closeProgress();
				reset();
			}
		};
		        // 全屏显示
				requestWindowFeature(Window.FEATURE_NO_TITLE);
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				setContentView(R.layout.image_main);
				
				mSaveAll = findViewById(R.id.save_all);
				mSaveStep = findViewById(R.id.save_step);
				mShowHandleName = (TextView) findViewById(R.id.handle_name);
				
				//获取图片
				Intent intent = getIntent();
				path = intent.getStringExtra("path");
				if(path == null){
                    Toast.makeText(this, "NND,path和uri都空", Toast.LENGTH_SHORT).show();
                    finish();
                 }
                    mBitmap = BitmapFactory.decodeFile(path);
                    if(mBitmap != null){
                        mTmpBmp = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        mImageView = (CropImageView) findViewById(R.id.crop_image);
                        mImageView.setImageBitmap(mBitmap);
                        mEditImage = new EditImage(this, mImageView, mBitmap);
                        mImageFrame = new ImageFrameAdder(this, mImageView, mBitmap);
                        mImageView.setEditImage(mEditImage);
                        mImageSpecific = new ImageSpecific(this);
                    }
				
				
				
	}
	
	// -----------------------------------菜单事件------------------------------------
	public void onClick(View v)
	{
		int flag = -1;
		switch (v.getId())
		{
		//保存操作结果
		case R.id.save:
//			onSaveClicked();
			String path = saveBitmap(mBitmap);
			if (mProgressDialog != null)
			{
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			Intent data = new Intent();
			data.putExtra("path", path);
			setResult(RESULT_OK, data);
			
			return;
		    //取消操作
		case R.id.cancel:
			setResult(RESULT_CANCELED);
            finish();
			return;
			
		case R.id.save_step:
		      if(mState == STATE_CROP || mState == STATE_SUB_CROP)
	            {
		              mTmpBmp = mEditImage.cropAndSave(mTmpBmp);
	            }
		     
			mBitmap = mTmpBmp;
			showSaveAll();
			reset();
			
			mEditImage.mSaving = true;
			mImageViewWidth = mImageView.getWidth();
			mImageViewHeight = mImageView.getHeight();
			return;
			//取消操作
		case R.id.cancel_step:
		    if (mState == STATE_CROP || mState == STATE_SUB_CROP)
            {
		        mEditImage.cropCancel();
            }
			showSaveAll();
			resetToOriginal();
			return;
			//粗调
		case R.id.coarse_cut_button:
			flag = FLAG_CROP;
			break;
			//微调
		case R.id.subtle_cut_button:
		    flag = FLAG_SUB_CROP;
		   showSaveStep();
			break;
		case R.id.retrieval:
		    flag = FLAG_RETRIEVAL;
			retrieval(mBitmap);
			break;	
		}
		initMenu(flag);
	}
	
	

    private void initMenu(int flag)
    {
        if (null == mMenuView)
        {
            mMenuView = new MenuView(this);
            mMenuView.setBackgroundResource(R.drawable.popup);
            mMenuView.setTextSize(16);
            
            switch (flag)
            {
            case FLAG_CROP: //粗调按钮
                mMenuView.hide();
                crop();
                showSaveStep();
                break;
            case FLAG_SUB_CROP: //微调按钮
                mMenuView.hide();
                showSaveStep();
                prepare(STATE_SUB_CROP, CropImageView.STATE_SUB_CROP,true);
                mShowHandleName.setText(R.string.sub_crop);
               
                break;
            case FLAG_RETRIEVAL://检索
                break;
            }
        }

        mMenuView.show();
    }
    
    /**
     * 菜单消失处理
     */
    private void dimissMenu()
    {
        mMenuView.dismiss();
        mMenuView = null;
    }
    private void retrieval(Bitmap m) {
	}


    

    
    private void showSaveStep()
    {
        mSaveStep.setVisibility(View.VISIBLE);
        mSaveAll.setVisibility(View.GONE);
    }
    
    private void showSaveAll()
    {
        mSaveStep.setVisibility(View.GONE);
        mSaveAll.setVisibility(View.VISIBLE);
    }
    

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        switch (keyCode)
        {
        case KeyEvent.KEYCODE_BACK:
            if (mMenuView != null && mMenuView.isShow() )
            {
                mMenuView.hide();
            } else
            {
                if (mSaveAll.getVisibility() == View.GONE)
                {
                    showSaveAll();
                }
                else
                {
                    finish();
                }
            }
            break;
        case KeyEvent.KEYCODE_MENU:
            break;
        
        }
        
        return super.onKeyDown(keyCode, event);
    }

	
	// ----------------------------------------------------功能----------------------------------------------------

	/**
	 * 进行操作前的准备
	 * @param state 当前准备进入的操作状态
	 * @param imageViewState ImageView要进入的状态
	 * @param hideHighlight 是否隐藏裁剪框
	 */
	private void prepare(int state, int imageViewState, boolean hideHighlight)
	{
		resetToOriginal();
		mEditImage.mSaving = false;
		if (null != mReverseAnim)
		{
			mReverseAnim.cancel();
			mReverseAnim = null;
		}
		
		if (hideHighlight)
		{
			mImageView.hideHighlightView();
		}
		mState = state;
		mImageView.setState(imageViewState);
		mImageView.invalidate();
	}
	   
	
	
	
	/**
	 * 粗调
	 */
	private void crop()
	{
		// 进入裁剪状态
		prepare(STATE_CROP, CropImageView.STATE_HIGHLIGHT, false);
		mShowHandleName.setText(R.string.crop);
		mEditImage.crop(mTmpBmp);
		reset();
	}

    /**
     *微调
     */
  

	

	private void resetToOriginal()
	{
	    Log.i("editimageactivity","resetToOriginal");
		mTmpBmp = mBitmap;
		mImageView.setImageBitmap(mBitmap);
		// 已经保存图片
		mEditImage.mSaving = true;
		// 清空裁剪操作
		mImageView.mHighlightViews.clear();
	}
	
	
	
	   /**
     * 关闭进度条
     */
    private void closeProgress()
    {
        if (null != mProgress)
        {
            mProgress.dismiss();
            mProgress = null;
        }
    }
    
    
    /**
     * 重新设置一下图片
     */
    private void reset()
    {
        mImageView.setImageBitmap(mTmpBmp);
        mImageView.invalidate();
    }
    
    
    /**
     * 保存图片到本地
     * @param bm
     */
    private String saveBitmap(Bitmap bm)
    {
        mProgressDialog = ProgressDialog.show(this, null, getResources().getString(R.string.save_bitmap));
        mProgressDialog.show();
        return mEditImage.saveToLocal(bm);
    }

    //---------------------------------------------------readstream---------------------------------------
	public static byte[] readStream ( InputStream inStream ) throws Exception
	{
		byte[] buffer = new byte[1024];
		int len = -1;
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		while ((len = inStream.read(buffer)) != -1)
		{
			outStream.write(buffer, 0, len);
		}
		byte[] data = outStream.toByteArray();
		outStream.close();
		inStream.close();
		return data;
	}
	
	public static Bitmap getPicFromBytes ( byte[] bytes , BitmapFactory.Options opts )
	{
		if (bytes != null)
			if (opts != null)
				return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
			else
				return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		return null;
	}

  
}
