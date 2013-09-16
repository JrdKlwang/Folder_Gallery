package com.example.t_gallery;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore.Images.Media;
import android.view.WindowManager;

class Config {
	/* Application configuration */
	static public final int MAX_ALBUM = 1000;
	static public final int MAX_PICTURE_IN_ALBUM = 10000;

	static public final int THUMBNAILS_PER_LINE = 6;
	static public final int THUMBNAIL_WIDTH = 132;
	static public final int THUMBNAIL_HEIGHT = 132;
	static public final int THUMBNAIL_BOUND_WIDTH = 264;
	static public final int THUMBNAIL_BOUND_HEIGHT = 264;
	static public final int IMAGE_LIST_THUMBNAIL_PADDING = 10;
	static public final int FOLDER_THUMBNAIL_PADDING = 5;

	/* RAM cache */
	static public final int RAM_CACHE_SIZE_KB = (int) (Runtime.getRuntime().maxMemory() / 4096);

	/* Content query */
	static public final Uri MEDIA_URI = Media.EXTERNAL_CONTENT_URI;
	static public final String ALBUM_PROJECTION[] = { Media.BUCKET_ID,
			Media.BUCKET_DISPLAY_NAME };
	static public final String ALBUM_WHERE_CLAUSE = "1) GROUP BY (1";  /*This is a trick to use GROUP BY */
	static public final String IMAGE_PROJECTION[] = { Media._ID, Media.WIDTH, Media.HEIGHT, Media.DATA };
	static public final String IMAGE_WHERE_CLAUSE = Media.BUCKET_ID + " = ?";

	/* UI Effect */
	static public final int LIST_ANIM_DURATION = 400; /* count in ms */

	static public final int COLLAPSE_SHORTCUT_ANIM_DURATION = 500;
	static public final int COLLAPSE_SHORTCUT_STAY_DURATION = 2000;

	static public final float MAX_WIDTH_HEIGHT_RATIO = 4.0f;
	static public final int ALBUM_DESCRIPTION_HEIGHT = 120;
	static public final int ALBUM_PADDING = 50;
	static public final int CAMARA_ALBUM_HEIGHT = 700;
	static public final int COMMON_ALBUM_HEIGHT = 500;

	/* Request string */
	static public final int REQUEST_CODE = 0x717;
	static public final String DISPLAY_ITEM_INDEX = "display_item_index";
	static public final String ALBUM_INDEX = "album_index";
	static public final String IMAGE_PATH = "path";
	static public final String CLICK_ITEM_INFO = "click_item_info";
	static public final String ANIM_CONTROL = "anim_control";
	static public final String IMAGE_LIST = "image_list";
	static public final String CLICK_INDEX = "click_index";
	static public final String THUMBNAIL_ID = "thumbnail_id";
	
	//Shared Preference File
	static public final String PREFS_NAME = "gallery_prefs_file";
	static public final String SCROLL_IMAGE_SUM = "scroll_image_sum";
	static public final String IMAGE_LIST_ANIM_ACTIVE = "image_list_anim_active";
	
	/*Screen Info*/
	public int screenWidth = 0;
	public int screenHeight = 0;
	public int curent_orientation = 0;
	
	Config(Context context) {
    	Point outPoint = new Point();
    	WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
    	wm.getDefaultDisplay().getSize(outPoint);
    	
    	screenWidth = outPoint.x;
    	screenHeight = outPoint.y;
    	
    	curent_orientation = context.getResources().getConfiguration().orientation;
	}
}