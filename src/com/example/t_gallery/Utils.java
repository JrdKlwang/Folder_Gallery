package com.example.t_gallery;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;

public class Utils {
	private static String TAG = "New-Gallery Utils";
	
	public static void putPrefAnimFlag(Context context, boolean flag) {
        // All objects are from android.context.Context  
        SharedPreferences settings = context.getSharedPreferences(Config.PREFS_NAME, context.MODE_PRIVATE);  
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(Config.IMAGE_LIST_ANIM_ACTIVE, flag);
        editor.commit();
	}
	
	public static boolean getPrefAnimFlag(Context context) {
		SharedPreferences settings = context.getSharedPreferences(Config.PREFS_NAME, context.MODE_PRIVATE);
		return settings.getBoolean(Config.IMAGE_LIST_ANIM_ACTIVE, false);
	}
	
	public static void putPrefScrollImageSum(Context context, int sum) {
		
        SharedPreferences settings = context.getSharedPreferences(Config.PREFS_NAME, context.MODE_PRIVATE);  
        SharedPreferences.Editor editor = settings.edit();
		editor.putInt(Config.SCROLL_IMAGE_SUM, sum);
		editor.commit();
	}
	
	public static int getPrefScrollImageSum(Context context) {
		
		SharedPreferences settings = context.getSharedPreferences(Config.PREFS_NAME, context.MODE_PRIVATE);
		return settings.getInt(Config.SCROLL_IMAGE_SUM, 0);
	}
	
	public static Bitmap makeDst(Bitmap bmp, int canvasWidth, int canvasHeight,
			int dispalyWidth, int displayHeight, int x, int y) {

		Bitmap output = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        int imageWidth = bmp.getWidth();
        int imageHeight = bmp.getHeight();
        
		Utils.LogV(TAG, "Bitmap width: " + imageWidth
				+ "  , Bitmap height: " + imageHeight
				+ "  ,Display width: " + dispalyWidth + "  ,Display height: " + displayHeight);
        
        Rect sRect = null;
        Bitmap scale_bitmap = null;
		if (imageWidth > dispalyWidth && imageHeight > displayHeight) {
			scale_bitmap = bmp;
		}
		else if(imageWidth > dispalyWidth) {
			float xRatio = (float)imageWidth / (float)imageHeight;
			float scaleHeight = displayHeight;
			float scaleWidth =  xRatio*scaleHeight;

			scale_bitmap = Bitmap.createScaledBitmap(bmp, (int)scaleWidth, (int)scaleHeight, false);
		}
		else if(imageHeight > displayHeight) {
			float yRatio = (float)imageHeight / (float)imageWidth;
			float scaleWidth = dispalyWidth;
			float scaleHeight =  yRatio*scaleWidth;

			scale_bitmap = Bitmap.createScaledBitmap(bmp, (int)scaleWidth, (int)scaleHeight, false);
		}
		else {
			if(imageHeight > imageWidth) {
				float yRatio = (float)imageHeight / (float)imageWidth;
				float scaleWidth = dispalyWidth;
				float scaleHeight =  yRatio*scaleWidth;
				
				scale_bitmap = Bitmap.createScaledBitmap(bmp, (int)scaleWidth, (int)scaleHeight, false);
			}
			else {
				float xRatio = (float)imageWidth / (float)imageHeight;
				float scaleHeight = displayHeight;
				float scaleWidth =  xRatio*scaleHeight;
				
				scale_bitmap = Bitmap.createScaledBitmap(bmp, (int)scaleWidth, (int)scaleHeight, false);
			}
		}
		
		sRect = new Rect((scale_bitmap.getWidth()- dispalyWidth) / 2,
				(scale_bitmap.getHeight() - displayHeight) / 2,
				(scale_bitmap.getWidth() + dispalyWidth) / 2,
				(scale_bitmap.getHeight() + displayHeight) / 2);
		
		Rect dRect = new Rect(0, 0, dispalyWidth, displayHeight);
		canvas.translate(x, y);
		canvas.drawBitmap(scale_bitmap, sRect, dRect, paint);
		
		return output;
	}
    
    public static Bitmap layerImages(ArrayList<Bitmap> image_list, int bigImagewidth, int bigImageheight) {

    	int wholeWidth = bigImagewidth + (image_list.size()-1) * Config.ALBUM_OVERLIE_IMAGE_WIDTH;
		Bitmap output = Bitmap.createBitmap(wholeWidth, bigImageheight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		Paint paint = new Paint();
    	
		for (int i = 0; i < image_list.size(); i++) {
			int alpha = 255 - i*50;
			
	        // draw the src/dst example into our offscreen bitmap
	        int sc = canvas.saveLayer(0, 0, wholeWidth, bigImageheight, null,
	                                  Canvas.MATRIX_SAVE_FLAG |
	                                  Canvas.CLIP_SAVE_FLAG |
	                                  Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
	                                  Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
	                                  Canvas.CLIP_TO_LAYER_SAVE_FLAG);
			paint.setAlpha(alpha);
			
			if(i == 0) {
				Bitmap bmp = makeDst(image_list.get(i), wholeWidth, bigImageheight, bigImagewidth, bigImagewidth, 0, 0);
                canvas.drawBitmap(bmp, 0, 0, paint);
			}
			else {
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
				
				Bitmap bmp = makeDst(image_list.get(i),
						wholeWidth, bigImageheight,
						Config.ALBUM_OVERLIE_IMAGE_WIDTH, bigImageheight - 2*i*Config.ALBUM_OVERLIE_DELETE_WIDTH,
						bigImagewidth + (i - 1)* Config.ALBUM_OVERLIE_IMAGE_WIDTH, i*Config.ALBUM_OVERLIE_DELETE_WIDTH);
				
                canvas.drawBitmap(bmp, 0, 0, paint);
			}
	        paint.setXfermode(null);
	        canvas.restoreToCount(sc);
		}

		return output;
    } 
	
	public static void LogV(String category, String content) {
		if (Config.LOG_ENABLE) {
			Log.v(category, content);
		}
	}
}