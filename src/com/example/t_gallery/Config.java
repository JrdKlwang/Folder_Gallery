package com.example.t_gallery;

import android.net.Uri;
import android.provider.MediaStore.Images.Media;

class Config {
	/* Application configuration */
	static public final int MAX_ALBUM = 1000;
	static public final int MAX_PICTURE_IN_ALBUM = 10000;

	static public final int THUMBNAILS_PER_LINE = 5;
	static public final int THUMBNAIL_WIDTH = 132;
	static public final int THUMBNAIL_HEIGHT = 132;
	static public final int THUMBNAIL_BOUND_WIDTH = 264;
	static public final int THUMBNAIL_BOUND_HEIGHT = 264;
	static public final int THUMBNAIL_PADDING = 10;

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
	static public final int ALBUM_DESCRIPTION_HEIGHT = 130;
	static public final int DESCRIPTION_TEXT_HEIGHT = 60;

	/* Request string */
	static public final int REQUEST_CODE = 0x717;
	static public final String DISPLAY_ITEM_INDEX = "display_item_index";
	static public final String ALBUM_INDEX = "album_index";
}