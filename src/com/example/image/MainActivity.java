package com.example.image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;  
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
	final static int CAMERA_RESULT = 0; 
	/**
	 *从相册中选择图片-数据结构
	 * */
	private Button album_button;
	
	private static final int FLAG_CHOOSE = 0x10;
	private static final int FLAG_HANDLEBACK = 0x11;
	private ImageView mImageView;
	String FileName;
	private Bitmap bm;// 需要旋转的图片资源Bitmap
	private float scaleW = 1;// 横向缩放系数，1表示不变
	private float scaleH = 1;// 纵向缩放系数，1表示不变
	private float curDegrees = 90;// 当前旋转度数
	/**
	 *相机拍照-数据结构
	 * */
	String TAG = "carol";
	private static MediaActionReceiver actionReceiver;
	private static final int SCAN_MEDIA_START = 1;
	private static final int SCAN_MEDIA_FINISH = 2;
	private static final int SCAN_MEDIA_FILE = 3;
	private static final int SCAN_MEDIA_FILE_FINISH_INT = 4;
	private static final int SCAN_MEDIA_FILE_FAIL_INT = 5;
    ProgressDialog delLoadingDialog = null;
    private static final String SCAN_MEDIA_FILE_FINISH = "ACTION_MEDIA_SCANNER_SCAN_FILE_FINISH";
    private static final String SCAN_MEDIA_FILE_FAIL = "ACTION_MEDIA_SCANNER_SCAN_FILE_FAIL";
    private static final int PIC_REQUEST_CODE_WITH_DATA = 1;    // 标识获取图片数据
    private static final int PIC_REQUEST_CODE_SELECT_CAMERA = 2;    // 标识请求照相功能的activity
	private Button camera_button;
	
    /* 拍照的照片存储位置和存储文件 */
	private static final File PHOTO_DIR = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
    private File mCurrentPhotoFile;// 照相机拍照得到的图片

	    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
             
    	/**
    	 *界面初始化
    	 * */
        album_button  = (Button) findViewById(R.id.album_button);
        camera_button = (Button) findViewById(R.id.camera_button);
        actionReceiver = new MediaActionReceiver();
        
        
    	/**
    	 *组建初始化
    	 * */
        
        
        //相册
        album_button.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				   testBitmap();
				   Intent intent = new Intent();
				   intent.setAction(Intent.ACTION_PICK);
				   intent.setType("image/*");
				   startActivityForResult(intent, FLAG_CHOOSE);  
			}
		});
        //相机
        camera_button.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				String status = Environment.getExternalStorageState();
				if (status.equals(Environment.MEDIA_MOUNTED)) {// 判断是否有SD卡
					// TODO Auto-generated method stub
					    PHOTO_DIR.mkdirs();// 创建照片的存储目录
					    Date date = new Date(System.currentTimeMillis());
				        SimpleDateFormat dateFormat = new SimpleDateFormat(
				                "'IMG'_yyyyMMdd_HHmmss");
				        
						FileName = dateFormat.format(date) + ".jpg";
						mCurrentPhotoFile = new File(PHOTO_DIR, FileName);
					    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
						intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCurrentPhotoFile));
						Log.d(TAG,"begin take picture");
						startActivityForResult(intent, PIC_REQUEST_CODE_SELECT_CAMERA);
			
				} else {
					Toast.makeText(MainActivity.this, "没有SD卡", Toast.LENGTH_LONG).show();
				}
				
			}
		});
    }
	
    
  	/**
	 *从相册中选择图片
	 * */

	private void testBitmap()
	{
		int width = 4;
		int height = 5;
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		for (int i = 0; i < height; i++)
		{
			for (int k = 0; k < width; k++)
			{
				if (k % 2 == 0)
				{
					bmp.setPixel(k, i, Color.RED);
				}
				else
				{
					bmp.setPixel(k, i, Color.BLACK);
				}
			}
		}
		
		int[] pixels = new int[width * height];
		bmp.getPixels(pixels, 0, width, 0, 0, width, height);
		Log.d(TAG, Arrays.toString(pixels));
		for (int i = 0; i < width; i++)
		{
			for (int k = 0; k < height; k++)
			{
				Log.d(TAG, pixels[i * height + k]+"");
			}
		}
	}
	

    
	//回调函数
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		
		if (resultCode == RESULT_OK )
		{
			switch (requestCode)
			{
			//相册
			case FLAG_CHOOSE:
			    Log.d(TAG,"FLAG_CHOOSE is"+FLAG_CHOOSE);
				Uri uri = data.getData();
				Log.d(TAG, "uri="+uri+", authority="+uri.getAuthority());
				if (!TextUtils.isEmpty(uri.getAuthority()))
				{
					Cursor cursor = getContentResolver().query(uri, new String[]{ MediaStore.Images.Media.DATA }, null, null, null);
					if (null == cursor)
					{
						Toast.makeText(this, R.string.no_found, Toast.LENGTH_SHORT).show();
						return;
					}
					cursor.moveToFirst();
					String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
					Log.d(TAG, "path="+path);
					Intent intent = new Intent(MainActivity.this, EditImageActivity.class);
					intent.putExtra("path", path);
					startActivity(intent);
					cursor.close();
				}
				else
				{
					Log.d(TAG, "path="+uri.getPath());
					Intent intent = new Intent(this, EditImageActivity.class);
					intent.putExtra("path", uri.getPath());
					Log.d(TAG,"IMAGE_PATH"+ uri.getPath());
					startActivity(intent);
				}
				break;
					
			case FLAG_HANDLEBACK:
				Log.d(TAG,"FLAG_HANDLEBACK is"+FLAG_HANDLEBACK);
				String imagePath = data.getStringExtra("path");
				Log.d(TAG,"may"+ imagePath);
				mImageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
				break;
		
       
            // 照相机程序返回的,再次调用图片剪辑程序去修剪图片
            case PIC_REQUEST_CODE_SELECT_CAMERA: 
            	     Log.d(TAG,"return from catch camera");
                     Uri fileUri = Uri.fromFile(mCurrentPhotoFile);
                     Uri dirUri = Uri.parse("file://" + Environment.getExternalStorageDirectory()+ "/DCIM/Camera");

                     try {
                        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
                         intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
                         intentFilter.addDataScheme("file");       
                         intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                         Log.d(TAG,"register Receiver");
                         registerReceiver(actionReceiver,intentFilter);
                    } catch (RuntimeException e) {
                    	  Log.d(TAG,"register Receiver caught error");
                    }
                    Log.d(TAG,"begin send scan file broadcast");
                     //sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, dirUri));  
                     sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, fileUri));
                break;
            
			}
		}
		
		else {
			Log.d(TAG, "resultCode != RESULT_OK ");
		}
	
	}
	
	
    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(actionReceiver);
        } catch (Exception e) {
            Log.e(TAG, "actionReceiver not registed");
        }
        super.onDestroy();
    }
    
	/**
	 * 定义receiver接收其他线程的广播
	 * 
	 */
	public class MediaActionReceiver extends BroadcastReceiver {

	    @Override
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action)) {
	            mHandler.sendEmptyMessage(SCAN_MEDIA_START);
	        }
	        if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
	            mHandler.sendEmptyMessage(SCAN_MEDIA_FINISH);
	        }
	        if (Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action)) {
	        	Log.d(TAG,"begin send scan file message");
	            mHandler.sendEmptyMessage(SCAN_MEDIA_FILE);
	        }
	    }
	}
	
	  public Handler mHandler = new Handler() {
	        public void handleMessage(Message msg) {
	            super.handleMessage(msg);
	                            switch (msg.what) {
	                            case SCAN_MEDIA_START:
	                                Log.d(TAG, "sccan media started");
	                                delLoadingDialog = onCreateDialogByResId(R.string.loading);
	                                delLoadingDialog.show();
	                                break;
	                            case SCAN_MEDIA_FINISH:
	                                Log.d(TAG, "sccan media finish");
	                                galleryPhoto();
	                                
	                                break;
	                                
	                            case SCAN_MEDIA_FILE:
	                                Log.d(TAG, "sccan file");
	                                delLoadingDialog = onCreateDialogByResId(R.string.loading);
	                                delLoadingDialog.show();
	                                ScanMediaThread sthread = new ScanMediaThread(MainActivity.this,40,300);
	                                sthread.run();
	                                break;
	                                
	                            case SCAN_MEDIA_FILE_FINISH_INT:
	                                Log.d(TAG, "sccan file finish");
	                                galleryPhoto();
	                                break;
	                                
	                            case SCAN_MEDIA_FILE_FAIL_INT:
	                                Log.d(TAG, "sccan file fail");
	                                if (delLoadingDialog!=null && delLoadingDialog.isShowing()) {
	                                    delLoadingDialog.dismiss();                        
	                                }
	                                
	                                try {
	                                    unregisterReceiver(actionReceiver);
	                                } catch (Exception e) {
	                                    Log.d(TAG, "actionReceiver not registed");
	                                }
	                                
	                                Toast.makeText(MainActivity.this, "no take photo",
	                                        Toast.LENGTH_LONG).show();
	                                break;

	                            
	        }
	    }
	    };
	    
	    /**
	     * 根据资源ID获得ProgressDialog对象
	     * 
	     * @param resId
	     * @return
	     */
	    protected ProgressDialog onCreateDialogByResId(int resId) {
	        ProgressDialog dialog = new ProgressDialog(this);
	        dialog.setMessage(getResources().getText(resId));
	        dialog.setIndeterminate(true);
	        dialog.setCancelable(true);
	        return dialog;
	    }
	    
	    
	  public class ScanMediaThread extends Thread{
	        private int scanCount=5;
	        private int interval=50;
	        private Context cx = null;
	        public ScanMediaThread(Context context,int count,int i){
	            scanCount = count;
	            interval =  i;
	            cx = context;
	            this.setName(System.currentTimeMillis() + "_ScanMediaThread");
        }
        
        @Override
        public void run() {
           
            if (this.cx == null)
                return;
            try {
            	 Log.d(TAG,"begin run scan thread");
                int j = 0;
                long id = 0;
                Uri imgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;                        
                ContentResolver cr = MainActivity.this.getContentResolver();

                Cursor cursor;
                for (j = 0; j < this.scanCount; j++) {
                    Thread.sleep(this.interval);
                    cursor = cr.query(imgUri, null,MediaStore.Images.Media.DISPLAY_NAME + "='"+mCurrentPhotoFile.getName()+"'", null, null);
                    Log.d(TAG,"scan thread "+ j);
                    if(cursor!=null&& cursor.getCount()>0){
                        Log.d(TAG,"send finish " + SCAN_MEDIA_FILE_FINISH);
                        //cx.sendBroadcast(new Intent(SCAN_MEDIA_FILE_FINISH));
                        mHandler.sendEmptyMessage(SCAN_MEDIA_FILE_FINISH_INT);
                        break;
                    }
                }
                if (j==this.scanCount) {
                    Log.d(TAG,"send fail ");
                    //cx.sendBroadcast(new Intent(SCAN_MEDIA_FILE_FAIL));
                    mHandler.sendEmptyMessage(SCAN_MEDIA_FILE_FAIL_INT);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        
    }
	    
	  private void galleryPhoto(){
	        try {
	            long id = 0;
	            Uri imgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;                        
	            ContentResolver cr = MainActivity.this.getContentResolver();

	            Cursor cursor = cr.query(imgUri, null,MediaStore.Images.Media.DISPLAY_NAME + "='"+mCurrentPhotoFile.getName()+"'", null, null);
	            
	            if(cursor!=null&& cursor.getCount()>0){
	                cursor.moveToLast();
	                id=cursor.getLong(0);

	                /*
	                 * update by lvxuejun on 20110322
	                 * 2.1系统对 gallery 这个调用进行了修改，uri不让传file:///了，只能传图库中的图片，
	                 * 比如此类uri：content://media/external/images/media/3
	                 * 所以需要把FIle的Uri 转化成图库的Uri
	                */
	                Uri uri = ContentUris.withAppendedId(imgUri,id);//Uri.fromFile(mCurrentPhotoFile);

	                // 启动gallery去剪辑这个照片
	                Log.d(TAG,"camera_uri" + uri.getEncodedPath());
	                Intent intent =getEditImageIntent(uri);// new Intent( MainActivity.this, EditImageActivity.class);
	                Log.d(TAG,"begin start edit image");
	                startActivity(intent);
	                
	                
	            }

	            if (delLoadingDialog!=null && delLoadingDialog.isShowing()) {
	                delLoadingDialog.dismiss();                        
	            }
	            
	            try {
	                unregisterReceiver(actionReceiver);
	            } catch (Exception e) {
	                Log.d(TAG, "actionReceiver not registed");
	            }
	        } catch (Exception ee) {
	            // TODO Auto-generated catch block
	            Log.d(TAG, "",ee);
	            
	        }
	    }
	private Intent getEditImageIntent(Uri uri) {
		// TODO Auto-generated method stub
		 Intent intent = new Intent(MainActivity.this, EditImageActivity.class);
	        //intent.setDataAndType(photoUri, "image/*");
	        intent.setData(uri);
	    
	        return intent;
	}
}


