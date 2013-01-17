package com.example.image.util;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * 图片处理
 * 
 * @author maylian.mei
 * 
 */
public class ImageUtil
{
	private static final String tag = "ImageUtil";

	/**
	 * 缩放图片
	 * @param bitmap
	 * @param w
	 * @param h
	 * @return
	 */
	public static Drawable resizeImage(Bitmap bitmap, int w, int h)
	{

		// load the origial Bitmap
		Bitmap BitmapOrg = bitmap;

		int width = BitmapOrg.getWidth();
		int height = BitmapOrg.getHeight();
		int newWidth = w;
		int newHeight = h;

		Log.v(tag, String.valueOf(width));
		Log.v(tag, String.valueOf(height));

		Log.v(tag, String.valueOf(newWidth));
		Log.v(tag, String.valueOf(newHeight));

		// calculate the scale
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();
		// resize the Bitmap
		matrix.postScale(scaleWidth, scaleHeight);
		// if you want to rotate the Bitmap
		// matrix.postRotate(45);

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
				height, matrix, true);

		// make a Drawable from Bitmap to allow to set the Bitmap
		// to the ImageView, ImageButton or what ever
		return new BitmapDrawable(resizedBitmap);

	}
	
	public static Drawable cropImage(Bitmap bitmap, int w, int h)
	{
		
		return null;
	}
	
}
