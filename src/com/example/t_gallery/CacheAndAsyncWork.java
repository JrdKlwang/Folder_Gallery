package com.example.t_gallery;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

public class CacheAndAsyncWork {
	private static String TAG = "New-Gallery CacheAndAsyncWork";
	private static LruCache<Long, Bitmap> mRamCache = null;
	
	public CacheAndAsyncWork() {
		if (mRamCache == null) {
			Log.v("t-galler", "create cache");
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
	
	class BitmapWorkerTask extends AsyncTask<Long, Void, Bitmap>{

		private final WeakReference<ImageView> iconReference;
		private long id;
		private ContentResolver contentResolver;
		private ArrayList<String> imageList;
		
		public BitmapWorkerTask(ImageView icon, ContentResolver contentResolver, ArrayList<String> imageList){
			Utils.LogV(TAG, "Create BitmapWorkerTask");
			iconReference = new WeakReference<ImageView>(icon);
			this.contentResolver = contentResolver;
			this.imageList = imageList;
		}
		
		@Override
		protected  Bitmap doInBackground(Long... params) {
		    id = params[0];
		    Bitmap thumb = null;
		    
		    if(params[1] == 0) {//Folder list
		    	ArrayList<Bitmap> image_list = new ArrayList<Bitmap>();
			    BitmapFactory.Options options = new BitmapFactory.Options();
			    
				for(int i = 0; i < Config.IMAGE_NUM_PRE_ALBUM; i++) {
					Bitmap temp = null;
					if(params[i + 2] != 0) {
						temp = Thumbnails.getThumbnail(contentResolver, params[i + 2], Thumbnails.MINI_KIND, options);
					}
					image_list.add(temp);
				}
				thumb = Utils.layerImages(image_list, params[6].intValue(), params[6].intValue());
		    }
		    else if(params[1] == 1) { // Camere Folder Image List
		    	
			    BitmapFactory.Options options = new BitmapFactory.Options();
		    	thumb = Thumbnails.getThumbnail(contentResolver, id, Thumbnails.MINI_KIND, options);
		    }
		    else if (params[1] == 2) { //Other Folder Image List
			    BitmapFactory.Options options = new BitmapFactory.Options();
				thumb = Thumbnails.getThumbnail(contentResolver, id, Thumbnails.MINI_KIND, options);
				
				//Load original image instead of thumbnail if we enlarge it too-much
				int outWidth = params[2].intValue();
				int imagePosition = params[3].intValue();
				String path = imageList.get(imagePosition);
				float ratio = (float)thumb.getWidth() / (float) outWidth;
					
				if (ratio <= 0.5f) {
					thumb.recycle();
					options.inJustDecodeBounds = true;
					BitmapFactory.decodeFile(path, options);
						
					options.inSampleSize = options.outWidth / outWidth;	
					options.inJustDecodeBounds = false;
					thumb = BitmapFactory.decodeFile(path, options);
					Utils.LogV(TAG, "load original ratio: " + ratio + "   ,inSampleSize: " + options.inSampleSize);
				}
		    }

			return thumb;
		}
		
		public void onPostExecute(Bitmap bitmap){
			
			if (iconReference != null && bitmap != null){
				final ImageView iconView = iconReference.get();
				
				if (iconView != null && iconView.isShown()){
					iconView.setImageBitmap(bitmap);
				}
				
				addBitmapToRamCache(id, bitmap);
			}
		}
     }

	class BitmapPathWorkerTask extends AsyncTask<String, Void, Bitmap> {

		private final WeakReference<ImageView> iconReference;
		private String path;
		private int scaleSize;

		public BitmapPathWorkerTask(ImageView icon, int scaleSize) {
			iconReference = new WeakReference<ImageView>(icon);
			this.scaleSize = scaleSize;
		}

		@Override
		protected Bitmap doInBackground(String... params) {

			Bitmap bitmap = null;
			BitmapFactory.Options options = new BitmapFactory.Options();

			path = params[0];

			options.inJustDecodeBounds = false;
			options.inSampleSize = scaleSize;
			bitmap = BitmapFactory.decodeFile(path, options);

			return bitmap;
		}

		public void onPostExecute(Bitmap bitmap) {

			if (iconReference != null && bitmap != null) {
				final ImageView iconView = iconReference.get();

				if (iconView != null) {
					iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
					iconView.setImageBitmap(bitmap);
				}
			}
		}
	}
}