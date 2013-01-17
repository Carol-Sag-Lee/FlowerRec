package com.example.image;
/*
 * Copyright (C) 2009 The Humanitarian FOSS Project
 *
 * Licensed under the GPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * 
 * A simple demo illustrating how to use some of Android's built-in Camera and
 * MediaStore functionality.  This App let's the user choose to capture either 
 * a full-size photo or small photo.  It starts the Camera activity, captures
 * the returned image,  displays its thumbnail in the main view, and displays 
 * a Gallery of thumbnails of all the images on the phone. When you click on the
 * Gallery thumbnail, it starts the Android photo view Activity.
 * 
 * This code goes with the following Tutorial: 
 * 
 *   http://2009.hfoss.org/Tutorial:Camera_and_Gallery_Demo#The_Activity_Class
 *
 */
public class CameraGalleryDemo extends Activity implements OnItemClickListener{
	private static final String TAG = "CameraGalleryDemo";
	private static final int CAMERA_ACTIVITY = 0;

	private Cursor mCursor;
	private Gallery mGallery;
	private Intent mIntent;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.album);
		mGallery = (Gallery)findViewById(R.id.picturesTaken);
 		displayGallery();
	}
	
	

	/**
	 * Reloads the Gallery to prevent crashes in case the user
	 *  has deleted an image in a sub-Activity.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		displayGallery();
	}	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.album, menu);
		return true;
	}

	/**
	 * Standard Intent action that can be sent to have the camera application 
	 * capture an image and return it.
	 * The caller may pass an extra EXTRA_OUTPUT to control where this 
	 * image will be written. If the EXTRA_OUTPUT is not present, then 
	 * a small sized image is returned as a Bitmap object in the extra field. 
	 * This is useful for applications that only need a small image. If the 
	 * EXTRA_OUTPUT is present, then the full-sized image 
	 * will be written to the Uri value of EXTRA_OUTPUT. (non-Javadoc)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		mIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);	
		switch(item.getItemId()) {
		case R.id.big_picture:
			mIntent.putExtra(
					MediaStore.EXTRA_OUTPUT, 
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()
					); 
			startActivityForResult(mIntent, CAMERA_ACTIVITY);
			break;
		case R.id.small_picture:
			startActivityForResult(mIntent, CAMERA_ACTIVITY);
			break;
		}
		return true;	
	}

	/**
	 * Retrieves the returned image from the Intent, inserts it into the MediaStore, which
	 *  automatically saves a thumbnail. Then assigns the thumbnail to the ImageView.
	 *  @param requestCode is the sub-activity code
	 *  @param resultCode specifies whether the activity was cancelled or not
	 *  @param intent is the data packet passed back from the sub-activity
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		Log.i(TAG, "Result code = " + resultCode);

		if (resultCode == RESULT_CANCELED) {
			showToast(this,"Activity cancelled");
			return;
		}
		switch (requestCode) {


		case CAMERA_ACTIVITY: 
			Bundle b = intent.getExtras();
			Bitmap bm = (Bitmap) b.get("data");
		

//			Bundle b = mIntent.getExtras();
//			Bundle b = intent.getExtras();
			//if (b != null && b.containsKey(MediaStore.EXTRA_OUTPUT)) { // large image?
			if (b.containsKey(MediaStore.EXTRA_OUTPUT)) { // large image?
				Log.i(TAG, "This is a large image");
				showToast(this,"Large image");
				// Should have to do nothing for big images -- should already saved in MediaStore ... but
				MediaStore.Images.Media.insertImage(getContentResolver(), bm, null, null);
			} else {
				Log.i(TAG, "This is a small image");
				showToast(this,"Small image");
				MediaStore.Images.Media.insertImage(getContentResolver(), bm, null, null);
			}
			break;
		}
		displayGallery();

	}


	/**
	 * Queries for images for this Find and shows them in a Gallery at the bottom of the View.
	 * managedQuery() is a Wrapper around query(android.net.Uri, String[], String, String[], String)  
	 * that gives the resulting Cursor to call startManagingCursor(Cursor) so that the 
	 * activity will manage its lifecycle for you. It parameters are:
	 *	uri 	The URI of the content provider to query.
	 *	projection 	List of columns to return.
	 *	selection 	SQL WHERE clause.
	 *	selectionArgs 	The arguments to selection, if any ?s are pesent
	 *	sortOrder 	SQL ORDER BY clause.
	 *
	 * Here we all the MINI_KIND thumbnails.
	 */
	private void displayGallery() {
		Uri uri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI; // Where images are stored
		displaySdCard();
		String[] projection = {
				MediaStore.Images.ImageColumns._ID,  // The columns we want
				MediaStore.Images.Thumbnails.IMAGE_ID,  
				MediaStore.Images.Thumbnails.KIND };
		String selection = MediaStore.Images.Thumbnails.KIND + "="  + // Select only mini's
		MediaStore.Images.Thumbnails.MINI_KIND;
		mCursor = this.managedQuery(uri, projection, selection, null, null);	
		if (mCursor != null) { 
			mCursor.moveToFirst();
			ImageAdapter adapter = new ImageAdapter(mCursor, this);
			Log.i(TAG, "displayGallery(), adapter = " + adapter.getCount());
			mGallery.setAdapter(adapter);
			mGallery.setOnItemClickListener(this);
		} else 
			showToast(this, "Gallery is empty.");
	}

	/**
	 *  Called when the user clicks on a thumbnail in the Gallery. It retrieves the
	 *  associated image and starts an ACTION_VIEW activity, which brings up a slide show.
	 *  @param arg0 is the Adapter used by the Gallery, the calling object
	 *  @param arg1 is the thumbnail's View
	 *  @param position is the thumbnail's position in the Gallery
	 *  @param rowId is the Adapter's RowId
	 */
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long rowId) {
		Log.i(TAG,"onImageClick position= " + position +  " rowId= " 
				+ rowId + " nCursor=" + mCursor.getCount());
		try {
			mCursor.moveToPosition(position);
			long id = mCursor.getLong(mCursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.IMAGE_ID));
			//create the Uri for the Image 
			Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id+"");
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(uri);
			startActivity(intent);
		} catch (CursorIndexOutOfBoundsException e) {
			Log.i(TAG, "CursorIndexOutOfBoundsException " + e.getStackTrace());
		}
	}
	
	/**
	 * Utility method for displaying a Toast.
	 * @param mContext
	 * @param text
	 */
	private void showToast(Context mContext, String text) {
		Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Debugging method. Call this to display the columns and values for images and
	 * thumbnails in the MediaStore.
	 */
	private void displaySdCard() {
		Uri uri = MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI; // Where images are stored
		Cursor c = this.managedQuery(uri, null, null, null, null);
		Log.i(TAG, "DISPLAYING THUMBNAILS  = " + c.getCount());
		c.moveToFirst();
		for (int k = 0; k < c.getCount(); k++) {
			Log.i(TAG, "ID = " + c.getString(c.getColumnIndexOrThrow("_id")));
			for (String column : c.getColumnNames()) {
				Log.i(TAG, column + "=" +  c.getString(c.getColumnIndexOrThrow(column)));
			}
			c.moveToNext();
		}

		uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI; // Where images are stored
		c = this.managedQuery(uri, null, null, null, null);

		Log.i(TAG, "DISPLAYING IMAGES  = " + c.getCount());
		c.moveToFirst();
		for (int k = 0; k < c.getCount(); k++) {
			Log.i(TAG, "ID = " + c.getString(c.getColumnIndexOrThrow("_id")));
			for (String column : c.getColumnNames()) {
				Log.i(TAG, column + "=" +  c.getString(c.getColumnIndexOrThrow(column)));
			}
			
			
			
			
			
			
			c.moveToNext();
		}
	}
}
