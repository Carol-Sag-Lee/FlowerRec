package com.example.image;



import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

/**
 * Used for putting images in the view
 * @author pgautam
 *
 */
class ImageAdapter extends BaseAdapter {
    int mGalleryItemBackground;
	private Context mContext;
	private Cursor mCursor;
	private static final String TAG = "ImageAdapter";


    public ImageAdapter(Cursor cursor, Context c) {
        mContext = c;
        mCursor = cursor;
        // See res/values/attrs.xml for the  defined values here for styling
        TypedArray a = mContext.obtainStyledAttributes(R.styleable.Gallery1);
        mGalleryItemBackground = a.getResourceId(
                R.styleable.Gallery1_android_galleryItemBackground, 0);
        a.recycle();
		Log.i(TAG, "ImageAdapter count = " + getCount());

    }

    public int getCount() {
      return mCursor.getCount();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * Called repeatedly to render the View of each item in the gallery.
     */
    public View getView(int position, View convertView, ViewGroup parent) {
		Log.i(TAG, "Get view = " + position);
		ImageView i = new ImageView(mContext);
    	mCursor.requery();
    	  	
    	 if (convertView == null) {
    		mCursor.moveToPosition(position);
    		int id = mCursor.getInt(mCursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID));
    		Uri uri = Uri.withAppendedPath(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, ""+id);
 //   		Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ""+id);
    		Log.i(TAG, "Image Uri = " + uri.toString());
    		try {
    			i.setImageURI(uri);
    			i.setScaleType(ImageView.ScaleType.FIT_XY);
    			i.setLayoutParams(new Gallery.LayoutParams(136, 136));
    			i.setBackgroundResource(mGalleryItemBackground);
    		} catch (Exception e) {
    			Log.i(TAG, "Exception " + e.getStackTrace());
    		}
    	}
    	return i;
    }
  }