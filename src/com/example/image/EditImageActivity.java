package com.example.image;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.example.image.util.EditImage;
import com.example.image.util.ImageFrameAdder;
import com.example.image.util.ImageSpecific;
import com.example.image.view.CropImageView;
import com.example.image.R;
import com.example.image.util.ReverseAnimation;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.*;
import org.opencv.core.Scalar;


public class EditImageActivity extends Activity {

	public boolean mWaitingToPick; // Whether we are wait the user to pick a face.
    public boolean mSaving; // Whether the "save" button is already clicked.
    
    private Handler mHandler = null;
    private ProgressDialog mProgress;
    private ProgressDialog mProgressDialog;
	private TextView mShowHandleName;
	private Bitmap mBitmap;
	private Button coarseCutButton;
	private Button subtleCutButton;
	private byte[] mContent;
	/**
	 * 临时保存
	 */
	private Bitmap mTmpBmp;
	
	private CropImageView mImageView;
	private EditImage mEditImage;
	private ImageFrameAdder mImageFrame;
	private ImageSpecific mImageSpecific;
    
	
	/** 裁剪 */
	private final int FLAG_CROP =  0x1;
	private static final int EDIT_IMAGE_FROM_CAMERA = 0x4;
	
	/** 边缘检测*/
	private Mat img;
	private List<Mat> contours;
	private Mat hierarchy;
	/* Contour retrieval modes  */
	private static final  int     CV_RETR_EXTERNAL = 0x01;
	private static final  int     CV_RETR_LIST=1;
	private static final  int 	CV_RETR_CCOMP=2;
	private static final  int 	CV_RETR_TREE=3;
	/*Contour approximation methods*/
	private static final  int     CV_CHAIN_CODE=0;
	private static final  int	    CV_CHAIN_APPROX_NONE=1;
	private static final  int	    CV_CHAIN_APPROX_SIMPLE=2;
	private static final  int	    CV_CHAIN_APPROX_TC89_L1=3;
	private static final  int	    CV_CHAIN_APPROX_TC89_KCOS=4;
	private static final  int	    CV_LINK_RUNS=5;
	private Bitmap tmpContour;
	/**
	 * 反转动画
	 */
	private ReverseAnimation mReverseAnim;
	private int mImageViewWidth;
	private int mImageViewHeight;

	private View mSaveAll;
	private View mSaveStep;

	private final int STATE_CROP = 0x1;
	private int mState;
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
				coarseCutButton = (Button)findViewById(R.id.coarse_cut_button);
				subtleCutButton = (Button)findViewById(R.id.subtle_cut_button);
				
				coarseCutButton.setOnClickListener(new View.OnClickListener() {
					
				
					public void onClick(View v) {
						// TODO Auto-generated method stub
						crop();
						showSaveStep();
					}
				});
				
				
				
				//获取图片
				Intent intent = getIntent();
				String path = intent.getStringExtra("path");
				Uri uri = intent.getData();
				Log.d("may", "MainActivity--->path="+path);
				Log.d("may", "MainActivity--->uri="+uri);
				if (null == path&&uri != null)
			    	{	
					    ContentResolver resolver = getContentResolver();
						mImageView = (CropImageView) findViewById(R.id.crop_image);
						try {
							mContent = readStream(resolver.openInputStream(Uri.parse(uri.toString())));
						} catch (Exception e) {
							System.out.println(e.getMessage());
						} 
						BitmapFactory.Options options = new BitmapFactory.Options();
		                options.inSampleSize = 2;
						// 将字节数组转换为ImageView可调用的Bitmap对象
		                mBitmap = getPicFromBytes(mContent, null);
		                if(mBitmap != null){
							mTmpBmp = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
							mImageView = (CropImageView) findViewById(R.id.crop_image);
							mImageView.setImageBitmap(mBitmap);
					        mImageView.setImageBitmapResetBase(mBitmap, true);
					        
					        mEditImage = new EditImage(this, mImageView, mBitmap);
					        mImageFrame = new ImageFrameAdder(this, mImageView, mBitmap);
					        mImageView.setEditImage(mEditImage);
					        mImageSpecific = new ImageSpecific(this);
						}
					}
					else if(null != path&&uri == null){
								BitmapFactory.Options options = new BitmapFactory.Options();
				                options.inSampleSize = 2;
				                mBitmap = BitmapFactory.decodeFile(path, options);
								if(mBitmap != null){
									mTmpBmp = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
									mImageView = (CropImageView) findViewById(R.id.crop_image);
									mImageView.setImageBitmap(mBitmap);
							        mImageView.setImageBitmapResetBase(mBitmap, true);
							        
							        mEditImage = new EditImage(this, mImageView, mBitmap);
							        mImageFrame = new ImageFrameAdder(this, mImageView, mBitmap);
							        mImageView.setEditImage(mEditImage);
							        mImageSpecific = new ImageSpecific(this);
								}
					}
					else if(null != path&&uri != null)
					{
						         Toast.makeText(this, "NND,path和uri都不空", Toast.LENGTH_SHORT).show();
						         finish();
				    }
					else{
								Toast.makeText(this, "NND,path和uri都空", Toast.LENGTH_SHORT).show();
							    finish();
					}
			}
	
	
	private void showSaveStep()
	{
		mSaveStep.setVisibility(View.VISIBLE);
		mSaveAll.setVisibility(View.GONE);
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
			finish();
			return;
		    //取消操作
		case R.id.cancel:
			setResult(RESULT_CANCELED);
            finish();
			return;
			//不理解
		case R.id.save_step:
			if (mState == STATE_CROP)
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
			//不理解
		case R.id.cancel_step:
			if (mState == STATE_CROP)
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
	        //边缘检测方法
			findContour();
			showContour();
			break;
			//搜索
		case R.id.retrieval:
			retrieval(mBitmap);
			break;	
		}
	}
	
     private void retrieval(Bitmap m) {
	        
		
	}


	private void showContour() {
		mImageView.setImageBitmap(tmpContour);
		
	}


	/*边缘检测*/
	private void findContour() {
		img = new Mat();
		img = Utils.bitmapToMat( mBitmap);
		if(img == null )
		{
			System.out.print("IMG IS NULL");
		}
		System.out.print("IMG IS NOT NULL");
		contours = new ArrayList<Mat>();
		if(contours == null )
		{
			System.out.print("CONTOURS ARE NULL");
			}
	
		int mode = CV_RETR_EXTERNAL;
		int method = CV_CHAIN_APPROX_SIMPLE;
		Imgproc.findContours(img,contours,hierarchy,mode,method);      //        int findContours(Mat image,
																							                //        java.util.List<MatOfPoint> contours,
																											//        Mat hierarchy,
																											//        int mode,
																											//        int method
		int idx = 0;
		for(; idx >= 0; idx = contours.size())
		{
			Scalar color = new Scalar(255,255,255);
		    Imgproc.drawContours(img, contours, idx, color, 4);	     //  drawContours(Mat image,
																						            //    java.util.List<MatOfPoint> contours,
																						            //    int contourIdx,
																						            //    Scalar color,
																						            //    int thickness)																			
		}

		Utils.matToBitmap(img, tmpContour);
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
	
	private void showSaveAll()
	{
		mSaveStep.setVisibility(View.GONE);
		mSaveAll.setVisibility(View.VISIBLE);
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
	 * 裁剪
	 */
	private void crop()
	{
		// 进入裁剪状态
		prepare(STATE_CROP, CropImageView.STATE_HIGHLIGHT, false);
		mShowHandleName.setText(R.string.crop);
		mEditImage.crop(mTmpBmp);
		reset();
	}
	

	private void resetToOriginal()
	{
		mTmpBmp = mBitmap;
		mImageView.setImageBitmap(mBitmap);
		// 已经保存图片
		mEditImage.mSaving = true;
		// 清空裁剪操作
		mImageView.mHighlightViews.clear();
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
