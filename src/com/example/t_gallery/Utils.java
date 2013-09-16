package com.example.t_gallery;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.view.WindowManager;


public class Utils {
	
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
	
    public static void imageLayout(Context context, int outLayout[], int iWidth, int iHeight) {
    	float layoutWidth = 0.0f, layoutHeight = 0.0f;
    	
    	Point outPoint = new Point();
    	WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
    	
    	wm.getDefaultDisplay().getSize(outPoint);
    	
    	int screenWidth = outPoint.x;
    	int screenHeight = outPoint.y;
    	
		float yRatio = (float)iHeight / (float)iWidth;
		
		if (yRatio > 1.0f) {
			float ratio = (float)screenHeight / (float)iHeight;
			if (ratio > 4.0f) {
				layoutHeight = iHeight*4;
			} else {
				layoutHeight = screenHeight;
			}
			layoutWidth = layoutHeight / yRatio;
		} else {
			layoutWidth = screenWidth;
			layoutHeight = layoutWidth * yRatio;
		}
		
		outLayout[0] = (int)layoutWidth;
		outLayout[1] = (int)layoutHeight;
    }
}