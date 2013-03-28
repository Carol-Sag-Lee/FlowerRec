package com.example.image.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


import com.example.image.R;
import com.example.image.view.CropImageView;
import com.example.image.view.HighlightView;

public class EditImage
{
	public boolean mWaitingToPick; // Whether we are wait the user to pick a face.
	public boolean mWaitingToPickDraw;
    public boolean mSaving; // Whether the "save" button is already clicked.
    public HighlightView mCrop;
    
	private Context mContext;
	private Handler mHandler = new Handler();
	private CropImageView mImageView;

	private Bitmap mBitmap;
	
	private float preX;
    private float preY;
    private float x;
   private float y;
	    
	
	public EditImage(Context context, CropImageView imageView, Bitmap bm)
	{
		mContext = context;
		mImageView = imageView;
		mBitmap = bm;
	}
	
	/**
	 * 图片裁剪
	 */
	public void crop(Bitmap bm)
	{
	    Log.i("editimage 监测","crop");
		mBitmap = bm;
		startCutProcess();
	}
	

    public void subCrop(float preX,float preY,float x,float y) {
        if (((Activity)mContext).isFinishing()) {
            Log.i("editimage","(Activity)mContext).isFinishing()");
            return;
        }
       this.preX = preX;
       this.preY  = preY;
       this.x = x;
       this.y = y;
      startSubCutProcess();
    }
    
	

    /**
	 * 图片旋转
	 * @param degree
	 */
	public Bitmap rotate(Bitmap bmp, float degree)
    {
    	Matrix matrix = new Matrix();
		matrix.postRotate(degree);
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		Bitmap bm = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
		return bm;
    }
	
	/**
	 * 图片反转
	 * @param bm
	 * @param flag
	 * @return
	 */
	public Bitmap reverse(Bitmap bmp, int flag)
	{
		float[] floats = null;
		switch (flag)
		{
		case 0: // 水平反转
			floats = new float[] { -1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f };
			break;
		case 1: // 垂直反转
			floats = new float[] { 1f, 0f, 0f, 0f, -1f, 0f, 0f, 0f, 1f };
			break;
		}
		if (floats != null)
		{
			Matrix matrix = new Matrix();
			matrix.setValues(floats);
			Bitmap bm = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
			return bm;
		}
		return null;
	}
    
	/**
	 * 图片缩放
	 * @param bm
	 * @param w
	 * @param h
	 * @return
	 */
	public Bitmap resize(Bitmap bm, float scale)
	{
		Bitmap BitmapOrg = bm;
		
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		// if you want to rotate the Bitmap
		// matrix.postRotate(45);
		
		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, BitmapOrg.getWidth(), BitmapOrg.getHeight(), matrix, true);
		return resizedBitmap;
	}
	/**
	 * 图片缩放
	 * @param bm
	 * @param w
	 * @param h
	 * @return
	 */
    public Bitmap resize(Bitmap bm, int w, int h)
    {
    	Bitmap BitmapOrg = bm;

		int width = BitmapOrg.getWidth();
		int height = BitmapOrg.getHeight();
		int newWidth = w;
		int newHeight = h;

		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		// if you want to rotate the Bitmap
		// matrix.postRotate(45);

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);
		return resizedBitmap;
    }
	
	private void startCutProcess() {
        if (((Activity)mContext).isFinishing()) {
            return;
        }

        showProgressDialog(mContext.getResources().getString(R.string.running_cut_process), new Runnable() {
            public void run() {
                final CountDownLatch latch = new CountDownLatch(1);
                final Bitmap b = mBitmap;
                mHandler.post(new Runnable() {//post一个runnable对象
                    public void run() {
                        if (b != mBitmap && b != null) {
                            Log.i("editimage", "startCutProcess showProgressDialog mHandler run");
          //                  mImageView.setImageBitmap(b);
                            mBitmap.recycle();
                            mBitmap = b;
                        }
                        if (mImageView.getScale() == 1.0f) {
                            mImageView.center(true, true);
                        }
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Log.i("editimage", "startCutProcess showProgressDialog before mRunCutProcess run");
                mRunCutProcess.run();
            }
        }, mHandler);
    }

private void startSubCutProcess() {
    if (((Activity)mContext).isFinishing()) {
        return;
    }
    
    showProgressDialog(mContext.getResources().getString(R.string.running_cut_process), new Runnable() {
        public void run() {
                    final CountDownLatch latch = new CountDownLatch(1);
                    final Bitmap b = mBitmap;
                    mHandler.post(new Runnable() {
                        public void run() {
                            Log.i("editimage","startSubCutProcess中showProgressDialog的mHandler运行run");
                            if (b != mBitmap && b != null) {
                                mImageView.setImageBitmap(b);
                                mBitmap.recycle();
                                mBitmap = b;
                            }
                            if (mImageView.getScale() == 1.0f) {
                                mImageView.center(true, true);
                            }
                            latch.countDown();
                        }
                    });
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    mRunSubCutProcess.run();
                }
            },mHandler);
}


	/**
	 * 裁剪并保存
	 * @return
	 */
	public Bitmap cropAndSave(Bitmap bm)
	{
		final Bitmap bmp = onSaveClicked(bm);
		mImageView.setState(CropImageView.STATE_NONE);
		mImageView.mHighlightViews.clear();
		return bmp;
	}
	
	/**
	 * 取消裁剪
	 */
	public void cropCancel()
	{
		mImageView.setState(CropImageView.STATE_NONE);
		mImageView.invalidate();
	}
	
    private Bitmap onSaveClicked(Bitmap bm) {
        // CR: TODO!
        // TODO this code needs to change to use the decode/crop/encode single
        // step api so that we don't require that the whole (possibly large)
        // bitmap doesn't have to be read into memory
        if (mSaving)
            return bm;

        if (mCrop == null) {
            return bm;
        }

        mSaving = true;

        Rect r = mCrop.getCropRect();

        int width = r.width(); // CR: final == happy panda!
        int height = r.height();

        // If we are circle cropping, we want alpha channel, which is the
        // third param here.
        Bitmap croppedImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        {
            Canvas canvas = new Canvas(croppedImage);
            Rect dstRect = new Rect(0, 0, width, height);
            canvas.drawBitmap(bm, r, dstRect, null);
        }
        return croppedImage;
    }

    public String saveToLocal(Bitmap bm)
    {
    	String path = "/sdcard/mm.jpg";
    	try
		{
			FileOutputStream fos = new FileOutputStream(path);
			bm.compress(CompressFormat.JPEG, 75, fos);
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		} catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
		
		return path;
    }

    private void showProgressDialog(String msg, Runnable job, Handler handler)
    {
    	final ProgressDialog progress = ProgressDialog.show(mContext, null, msg);
    	new Thread(new BackgroundJob(progress, job, handler)).start();
    }
    
    //mRunCutProcess开始

    Runnable mRunCutProcess = new Runnable() {
        float mScale = 1F;
        Matrix mImageMatrix;
        FaceDetector.Face[] mFaces = new FaceDetector.Face[3];
        int mNumFaces;

        // For each face, we create a HightlightView for it.
        private void handleFace(FaceDetector.Face f) {
            PointF midPoint = new PointF();

            int r = ((int) (f.eyesDistance() * mScale)) * 2;
            f.getMidPoint(midPoint);
            midPoint.x *= mScale;
            midPoint.y *= mScale;

            int midX = (int) midPoint.x;
            int midY = (int) midPoint.y;
            Log.i("EditImage 监测","mRunCutProcess运行 before HighlightView");
            HighlightView hv = new HighlightView(mImageView);

            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();

            Rect imageRect = new Rect(0, 0, width, height);

            RectF faceRect = new RectF(midX, midY, midX, midY);
            //to make rect narrower  for insert parameter is positive
            faceRect.inset(-r, -r);
            if (faceRect.left < 0) {
                faceRect.inset(-faceRect.left, -faceRect.left);
            }

            if (faceRect.top < 0) {
                faceRect.inset(-faceRect.top, -faceRect.top);
            }

            if (faceRect.right > imageRect.right) {
                faceRect.inset(faceRect.right - imageRect.right, faceRect.right - imageRect.right);
            }

            if (faceRect.bottom > imageRect.bottom) {
                faceRect.inset(faceRect.bottom - imageRect.bottom, faceRect.bottom - imageRect.bottom);
            }

            hv.setup(mImageMatrix, imageRect, faceRect, false, false);

            mImageView.add(hv);
        }

        // Create a default HightlightView if we found no face in the picture.
        private void makeDefault() {
            Log.i("EditImage 监测", "makeDefault 初始化highlightView的地方");//初始化HighLightView的地方
            HighlightView hv = new HighlightView(mImageView);
            int width = mBitmap.getWidth();
            int height = mBitmap.getHeight();

            Rect imageRect = new Rect(0, 0, width, height);

            // CR: sentences!
            // make the default size about 4/5 of the width or height
            int cropWidth = Math.min(width, height) * 4 / 5;
            int cropHeight = cropWidth;
            //cropImage origin point
            int x = (width - cropWidth) / 2;
            int y = (height - cropHeight) / 2;

            RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
            Log.i("EditImage 监测", "makeDefault hightlightview setup的地方");
            hv.setup(mImageMatrix, imageRect, cropRect, false, false);
            mImageView.add(hv);
        }

        // Scale the image down for faster face detection.
        private Bitmap prepareBitmap() {
            if (mBitmap == null) {
                return null;
            }

            // 256 pixels wide is enough.
            if (mBitmap.getWidth() > 256) {
                mScale = 256.0F / mBitmap.getWidth(); // CR: F => f (or change
                                                      // all f to F).
            }
            Matrix matrix = new Matrix();
            matrix.setScale(mScale, mScale);
            Bitmap faceBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
            return faceBitmap;
        }

        public void run() {
            mImageMatrix = mImageView.getImageMatrix();
            Bitmap faceBitmap = prepareBitmap();

            mScale = 1.0F / mScale;
            if (faceBitmap != null) {
                FaceDetector detector = new FaceDetector(faceBitmap.getWidth(), faceBitmap.getHeight(), mFaces.length);
                mNumFaces = detector.findFaces(faceBitmap, mFaces);
            }

            if (faceBitmap != null && faceBitmap != mBitmap) {
                faceBitmap.recycle();
            }

            mHandler.post(new Runnable() {
                public void run() {
                    mWaitingToPick = mNumFaces > 1;
                    if (mNumFaces > 0) {
                        for (int i = 0; i < mNumFaces; i++) {
                            handleFace(mFaces[i]);
                        }
                    } else {
                        makeDefault();//没有人脸监测框的时候会出现这个DEFAULT的框
                    }
                    mImageView.invalidate();
                    if (mImageView.mHighlightViews.size() == 1) {
                        Log.i("EditImage","mHighlightViews初始化");
                        mCrop = mImageView.mHighlightViews.get(0);
                        mCrop.setFocus(true);
                    }

                    if (mNumFaces > 1) {
                        // CR: no need for the variable t. just do
                        // Toast.makeText(...).show().
                        Toast t = Toast.makeText(mContext, R.string.multiface_crop_help, Toast.LENGTH_SHORT);
                        t.show();
                    }
                }
            });
        }
    };
    
    //mRunCutProcess结束

    Runnable mRunSubCutProcess = new Runnable() {
        Matrix mImageMatrix;
       
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                public void run() {   
                    int w =mBitmap.getWidth();
                    int h = mBitmap.getHeight(); 
                    int size = w*h;
                    mImageMatrix = mImageView.getImageMatrix();
                    int[] pixels = new int[size];
                    mBitmap.getPixels(pixels, 0, w, 0, 0, w, h);
                    for(int i =0;i<size;i++)
                    {
                        pixels[i] = pixels[i]&0x00ffffff;
                    }
                    int[] tmpPixels =  GrabCut.grabCut(pixels, w, h,preX, preY, x,y ); //返回值全部为空
                    for(int i =0;i<size;i++)
                    {
                        tmpPixels[i] = tmpPixels[i]|0xff000000;
                    }
                    /*
                   * 查看返回值结果
                   */
               
                    for(int i =0; i<size;i++) {
                        if(tmpPixels[i] !=0)
                        Log.i("editimage","tmp["+i+"]"+tmpPixels[i]) ;
                    }
                    Log.i("editimage","w*h"+w*h+"tmpPixels长度"+tmpPixels.length);
                    
                    final Bitmap b = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
                    b.setPixels(tmpPixels, 0, w, 0, 0, w, h);//immutable的图片不能setPixels
                    
                    if(b != mBitmap && b != null) {
                        Log.i("editimage", "b不为空");
                        mImageView.setImageBitmap(b);
                        mBitmap.recycle();
                        mBitmap = b;
                    }
           
                    Log.i("editimage","mRunSubCutProcess中已经返回结果resultImgBit");
                    mImageView.invalidate();
             } });
        
        }
    
    };
    
	
	class BackgroundJob implements Runnable//结束背景任务，并关闭progressDialog
    {
    	private ProgressDialog mProgress;
    	private Runnable mJob;
    	private Handler mHandler;
    	public BackgroundJob(ProgressDialog progress, Runnable job, Handler handler)
    	{
    		mProgress = progress;
    		mJob = job;
    		mHandler = handler;
    	}    
    	
    	public void run()
    	{
    		try 
    		{
    			mJob.run();
    		}
    		finally
    		{
    			mHandler.post(new Runnable()//将一个消息加入消息队列
    			{
    				public void run()
    				{
    					if (mProgress != null && mProgress.isShowing())
    					{
    						mProgress.dismiss();
    						mProgress = null;
    					}
    				}
    			});
    		}
    	}
    }




  

}
