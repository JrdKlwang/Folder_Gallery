package com.example.t_gallery;

import android.content.Context;
import android.content.SharedPreferences;

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
}