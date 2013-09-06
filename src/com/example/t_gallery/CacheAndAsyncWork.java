package com.example.t_gallery;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

public class CacheAndAsyncWork {
	private static LruCache<Long, Bitmap> mRamCache = null;
	
	public CacheAndAsyncWork() {
		if (mRamCache == null) {
			mRamCache = new LruCache<Long, Bitmap>(Config.RAM_CACHE_SIZE_KB) {
				protected int sizeOf(Long key, Bitmap bmp) {
					return (bmp.getByteCount() / 1024);
				}
			};
		}
	}
	
	public synchronized Bitmap getBitmapFromRamCache(long key){
		return mRamCache.get(key);	
	}
	
	public synchronized void addBitmapToRamCache(long key, Bitmap bmp){
		if (null == getBitmapFromRamCache(key)){
			mRamCache.put(key, bmp);
		}
	}
	
	private synchronized Bitmap clipSlimFlatImage(String path, int iWidth, int iHeight, int dWidth, int dHeight) {
		Bitmap bitmap = null;

		try {
			BitmapRegionDecoder mDecoder = BitmapRegionDecoder.newInstance(path, true);

			if (mDecoder != null) {
				Rect mRect = new Rect();

				float ratio = (float)dHeight / (float)dWidth;
				if(ratio > 1.0f) {
					int realHeight = (int)(iWidth*ratio);
					int left = 0;
					int top = (iHeight - realHeight) / 2;
					int right = iWidth;
					int bottom = top + realHeight;

					mRect.set(left, top, right, bottom);
				}
				else {
					int realWidth = (int)(iHeight/ratio);
					int left = (iWidth - realWidth) / 2;
					int top = 0;
					int right = left + realWidth;
					int bottom = iHeight;

					mRect.set(left, top, right, bottom);
				}

				bitmap = mDecoder.decodeRegion(mRect, null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	private synchronized Bitmap adjustThumbmail(Bitmap thumb, Long params[], ArrayList<String> imageList) {
		
		int outWidth = params[1].intValue();
		int outHeight = params[2].intValue();
		int imagePosition = params[3].intValue();
		int crop = params[4].intValue();
		
		if(thumb != null) {
			float ratio = (float)thumb.getHeight() / (float)thumb.getWidth();
	        String path = imageList.get(imagePosition);

			//Crop the image a little if it is TOO slim or TOO flat
			if(1 == crop) {
				BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();

				bitmapFactoryOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(path, bitmapFactoryOptions);
				
				Bitmap clipBitmap = clipSlimFlatImage(path,
						bitmapFactoryOptions.outWidth,
						bitmapFactoryOptions.outHeight, outWidth, outHeight);

				Log.v("t-gallery", "crop image width: " + bitmapFactoryOptions.outWidth + "  , height: " + bitmapFactoryOptions.outHeight);
				if (clipBitmap != null) {
					thumb.recycle();
					
					thumb = Bitmap.createScaledBitmap(clipBitmap, outWidth, outHeight, false);
							
					if (!clipBitmap.isRecycled()) {
						clipBitmap.recycle();
					}
				}
				return thumb;
			}

			//Load original image instead of thumbnail if we enlarge it too-much
			ratio = (float)thumb.getWidth() / (float) outWidth;
			if (ratio <= 0.5f) {

				thumb.recycle();

				BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();

				bitmapFactoryOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(path, bitmapFactoryOptions);
				
				bitmapFactoryOptions.inJustDecodeBounds = false;
				bitmapFactoryOptions.inSampleSize = bitmapFactoryOptions.outWidth / outWidth;
				thumb = BitmapFactory.decodeFile(path, bitmapFactoryOptions);

				Log.v("t-gallery", "load original ratio: "+ratio + "   ,inSampleSize: "+bitmapFactoryOptions.inSampleSize);
				return thumb;
			}
		}
		else {
			//Have this case? Need to check
		}
		return thumb;
	}
	
	class BitmapWorkerTask extends AsyncTask<Long, Void, Bitmap>{

		private final WeakReference<ImageView> iconReference;
		private long id;
		private ContentResolver contentResolver;
		private ArrayList<String> imageList;
		
		public BitmapWorkerTask(ImageView icon, ContentResolver contentResolver, ArrayList<String> imageList){
			iconReference = new WeakReference<ImageView>(icon);
			this.contentResolver = contentResolver;
			this.imageList = imageList;
		}
		
		@Override
		protected  Bitmap doInBackground(Long... params) {
		    BitmapFactory.Options options = new BitmapFactory.Options();
		    
		    id = params[0];
			Bitmap thumb = Thumbnails.getThumbnail(contentResolver, id, Thumbnails.MINI_KIND, options);
			
			//If handled, adjust will recycle thumb, and return the handled Bitmap
			if(imageList != null) {
				thumb = adjustThumbmail(thumb, params, imageList);
			}

			return thumb;
		}
		
		public void onPostExecute(Bitmap bitmap){
			
			if (iconReference != null && bitmap != null){
				final ImageView iconView = iconReference.get();
				
				if (iconView != null){
					addBitmapToRamCache(id, bitmap);
					iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
					iconView.setImageBitmap(bitmap);
				}
			}
		}
     }
}