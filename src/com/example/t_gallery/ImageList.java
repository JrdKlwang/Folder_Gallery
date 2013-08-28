package com.example.t_gallery;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;
import android.util.LruCache;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.AbsoluteLayout;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ImageList extends ListActivity {
	private ArrayList<String> mImageList;
	private LruCache<Long, Bitmap> mRamCache;
	public GalleryLayout mGalleryLayout;
	private ImageAnim mImageAnimObj;
	private ImageListAdapter mAdapter;
	
	private void fetchImageList(int album_index){
		
		Cursor galleryList = getContentResolver().query(
				Config.MEDIA_URI, 
				Config.ALBUM_PROJECTION, 
				Config.ALBUM_WHERE_CLAUSE, 
				null, null);
	
		mImageList = new ArrayList<String>();
		
		mGalleryLayout = new GalleryLayout(1080);
		
		galleryList.moveToPosition(album_index);
		
		String galleryId = galleryList.getString(galleryList.getColumnIndex(Media.BUCKET_ID));
		Cursor listCursor = getContentResolver().query(
				Config.MEDIA_URI, 
				Config.IMAGE_PROJECTION, 
				Config.IMAGE_WHERE_CLAUSE, 
				new String[]{galleryList.getString(galleryList.getColumnIndex(Media.BUCKET_ID))}, 
				null);
		
		listCursor.moveToFirst();
		
		int j = 0;
		
		while (false == listCursor.isAfterLast()){
			long id = listCursor.getLong(listCursor.getColumnIndex(Media._ID));
			int width = listCursor.getInt(listCursor.getColumnIndex(Media.WIDTH));
			int height = listCursor.getInt(listCursor.getColumnIndex(Media.HEIGHT));
	        String path = listCursor.getString(listCursor.getColumnIndex(Media.DATA));
			
			if (width != 0 && height != 0){
				float ratio = (float) height / (float) width;
				if (ratio > Config.MAX_WIDTH_HEIGHT_RATIO) {
					height = (int) (width * Config.MAX_WIDTH_HEIGHT_RATIO);
				}

				ratio = (float) width / (float) height;
				if (ratio > Config.MAX_WIDTH_HEIGHT_RATIO) {
					width = (int) (height * Config.MAX_WIDTH_HEIGHT_RATIO);
				}

				mGalleryLayout.addImage(id, width, height, j);
			}
			listCursor.moveToNext();
			
			j++;
			mImageList.add(path);
		}
		mGalleryLayout.addImageFinish();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_list);
		
		int album_index = getIntent().getIntExtra (Config.ALBUM_INDEX, 0);
		fetchImageList(album_index);						
		
		mRamCache = new LruCache<Long, Bitmap>(Config.RAM_CACHE_SIZE_KB){
			protected int sizeOf(Long key, Bitmap bmp){
				return (bmp.getByteCount()/1024);
			}
		};

		mAdapter = new ImageListAdapter(this);
		setListAdapter(mAdapter);
		
		mImageAnimObj = new ImageAnim();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_gallery_list, menu);
		return true;
	}
		
	private synchronized Bitmap getBitmapFromRamCache(long key){
		return mRamCache.get(key);	
	}
	
	private synchronized void addBitmapToRamCache(long key, Bitmap bmp){
		if (null == getBitmapFromRamCache(key)){
			mRamCache.put(key, bmp);
		}
	}
	
	private synchronized Bitmap clipSlimFlatImage(String path, int width, int height) {
		Bitmap bitmap = null;

		try {
			BitmapRegionDecoder mDecoder = BitmapRegionDecoder.newInstance(path, true);

			if (mDecoder != null) {
				Rect mRect = new Rect();

				float ratio = (float)height / (float)width;
				if(ratio > Config.MAX_WIDTH_HEIGHT_RATIO) {
					int realHeight = (int)(width*Config.MAX_WIDTH_HEIGHT_RATIO);
					int left = 0;
					int top = (height - realHeight) / 2;
					int right = width;
					int bottom = top + realHeight;

					mRect.set(left, top, right, bottom);
				}

				ratio = (float)width / (float)height;
				if(ratio > Config.MAX_WIDTH_HEIGHT_RATIO) {
					int realWidth = (int)(height*Config.MAX_WIDTH_HEIGHT_RATIO);
					int left = (width - realWidth) / 2;
					int top = 0;
					int right = left + realWidth;
					int bottom = height;

					mRect.set(left, top, right, bottom);
				}

				bitmap = mDecoder.decodeRegion(mRect, null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	private synchronized Bitmap adjustThumbmail(Bitmap thumb, int outWidth,
			int outHeight, int imagePosition) {
		
		if(thumb != null) {
			float ratio = (float)thumb.getHeight() / (float)thumb.getWidth();
	        String path = mImageList.get(imagePosition);

			//Crop the image a little if it is TOO slim or TOO flat
			if(ratio > Config.MAX_WIDTH_HEIGHT_RATIO || ratio < 1/Config.MAX_WIDTH_HEIGHT_RATIO) {
				Log.v("t-gallery", "crop image ratio: "+ratio);
				thumb.recycle();

				BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();

				bitmapFactoryOptions.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(path, bitmapFactoryOptions);
				
				Bitmap clipBitmap = clipSlimFlatImage(path, bitmapFactoryOptions.outWidth, bitmapFactoryOptions.outHeight);

				thumb = Bitmap.createScaledBitmap (clipBitmap, outWidth, outHeight, false);
				if(!clipBitmap.isRecycled()){
					clipBitmap.recycle();
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

	class ImageListAdapter extends BaseAdapter{
		private Context context;

		public ImageListAdapter(Context context) {
			super();
			this.context = context;
		}

		@Override
		public int getCount() {
			return mGalleryLayout.getLineNum();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			AbsoluteLayout line;			
			ViewHolder holder;
			/*Prepare the view (no content yet)*/
			if (convertView == null){
				line = new AbsoluteLayout(getApplicationContext());
				
	            holder = new ViewHolder();
	            
	            for (int i=0; i<Config.THUMBNAILS_PER_LINE; i++){
	            	holder.icons[i] = new ImageView(getApplicationContext());
	            	holder.icons[i].setBackgroundColor(0xFF000000);
	            	holder.icons[i].setAdjustViewBounds(true);
					holder.icons[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
					holder.icons[i].setPadding(Config.THUMBNAIL_PADDING, Config.THUMBNAIL_PADDING, Config.THUMBNAIL_PADDING, Config.THUMBNAIL_PADDING);
					
	    			line.addView(holder.icons[i], new AbsoluteLayout.LayoutParams(Config.THUMBNAIL_BOUND_WIDTH,Config.THUMBNAIL_BOUND_HEIGHT,0,0));
	            }
				
				line.setTag(R.id.data_holder, holder);
				line.setTag(R.id.recyclable, true);
			}
			else {
				line = (AbsoluteLayout)convertView;
				holder = (ViewHolder)(line.getTag(R.id.data_holder)); 
				
				for (int i=0; i<Config.THUMBNAILS_PER_LINE; i++){
					if (holder.task[i] != null){
						holder.task[i].cancel(true);
						holder.task[i] = null;
					}
				}
			}
			
			ImageLineGroup currentLine = mGalleryLayout.lines.get(position);
			
			/*Fill in the content*/
	        for (int i=0; i<Config.THUMBNAILS_PER_LINE; i++){

	        	if (i >= currentLine.getImageNum()){
	        		holder.icons[i].setVisibility(View.GONE);
	        	}
	        	else {
	        		Bitmap bmp = getBitmapFromRamCache(currentLine.getImage(i).id);
	        		ImageCell image = currentLine.getImage(i);
	      
	        		
	        		holder.icons[i].setLayoutParams(new AbsoluteLayout.LayoutParams(image.outWidth+2*Config.THUMBNAIL_PADDING, image.outHeight+2*Config.THUMBNAIL_PADDING, image.outX, image.outY));
	        		holder.icons[i].setVisibility(View.VISIBLE);
	        		
	        		if (bmp != null){
	        			holder.icons[i].setImageBitmap(bmp);
	        		}
	        		else {
	        			BitmapWorkerTask task = new BitmapWorkerTask(holder.icons[i]);
					    task.execute(image.id, (long)image.outWidth, (long)image.outHeight, (long)image.position);
					    holder.task[i] = task;
					    holder.icons[i].setScaleType(ImageView.ScaleType.FIT_XY);
					    holder.icons[i].setImageResource(R.drawable.grey);	
	        		}
	        		
					int viewId = image.position;

					holder.icons[i].setId(viewId);

					holder.icons[i].setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							entryImageDetail(v);
						}
					});
					
					mImageAnimObj.imageAnim(line, position, i, holder.icons[i]);
	        	}
			}

			return line;
		}
		
		class BitmapWorkerTask extends AsyncTask<Long, Void, Bitmap>{

			private final WeakReference<ImageView> iconReference;
			private long id;
			
			public BitmapWorkerTask(ImageView icon){
				iconReference = new WeakReference<ImageView>(icon);
			}
			
			@Override
			protected  Bitmap doInBackground(Long... params) {
			    BitmapFactory.Options options = new BitmapFactory.Options();
			    
			    id = params[0];
				Bitmap thumb = Thumbnails.getThumbnail(getContentResolver(), id, Thumbnails.MINI_KIND, options);
				
				//If handled, adjust will recycle thumb, and return the handled Bitmap
				thumb = adjustThumbmail(thumb, params[1].intValue(),
						  params[2].intValue(), params[3].intValue());

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
		
	    class ViewHolder{
			ImageView icons[] = new ImageView[Config.THUMBNAILS_PER_LINE];
			BitmapWorkerTask task[] = new BitmapWorkerTask[Config.THUMBNAILS_PER_LINE];
			boolean inTransient = false;
		}
	    
		
		public void entryImageDetail(View v) {

			//Store display state
			int aDisplayState[] = mImageAnimObj.getDisplayStatus();
			aDisplayState[0] = getListView().getFirstVisiblePosition();
			aDisplayState[1] = getListView().getLastVisiblePosition();
			
			int clickItemInfo[] = new int[4];
			v.getLocationOnScreen (clickItemInfo);
			clickItemInfo[2] = v.getWidth();
			clickItemInfo[3] = v.getHeight();
			

	        final Intent i = new Intent(ImageList.this, ImageDetail.class);
	        i.putStringArrayListExtra(ImageDetail.IMAGE_LIST, mImageList);
	        i.putExtra(ImageDetail.CLICK_INDEX, v.getId());
	        i.putExtra(ImageDetail.CLICK_ITEM_INFO, clickItemInfo);
	        
	        startActivityForResult(i, Config.REQUEST_CODE);
		}
	}
	
	class ImageAnim {
		
		private int mImageLocation[]; //[0]: ChildPosition, [1]:(itemIndex)Which one in line
		private int mDisplayState[]; //[0]:firstVisibleIndex, [1]:lastVisibleIndex
		
		static public final int TOP_DISPLAY = 1;
		static public final int NOT_CHANGE = 0;
		static public final int BOTTOM_DISPLAY = -1;
		
		ImageAnim() {
			mImageLocation = new int[2];
			for(int i = 0; i < mImageLocation.length; i++) {
				mImageLocation[i] = -1;
			}
			
			mDisplayState = new int[2];
			for(int i = 0; i < mDisplayState.length; i++) {
				mDisplayState[i] = -1;
			}
		}
		
		public int[] getImageLocation() {
			return mImageLocation;
		}
		
		public int[] getDisplayStatus() {
			return mDisplayState;
		}
		
		public void calculateChildPosition(int itemIndex) {
			
			mImageLocation[0] = -1;
			int totalLines = mGalleryLayout.getLineNum();
			
			int sum = 0;
			for(int i = 0; i < totalLines; i++) {
				sum += mGalleryLayout.lines.get(i).imageList.size();
				if(itemIndex < sum) {
					mImageLocation[0] = i;
					break;
				}
			}
			
			if (-1 != mImageLocation[0]) {
				mImageLocation[1] = itemIndex
						- (sum - mGalleryLayout.lines.get(mImageLocation[0]).imageList.size());
			}
		}
		
		public int getChangeState(int itemIndex) {
			
			int state = NOT_CHANGE;
			
			calculateChildPosition(itemIndex);
			
			if(mImageLocation[0] <= mDisplayState[0]) {
				state = TOP_DISPLAY;
			}
			else if(mImageLocation[0] >= mDisplayState[1]) {
				state = BOTTOM_DISPLAY;
			}
			return state;
		}
		
		public void imageAnim(AbsoluteLayout line, int childPosition, int itemIndex, ImageView view) {
			if (childPosition == mImageLocation[0] && itemIndex == mImageLocation[1]) {
				
	        	ImageLineGroup currentLine = mGalleryLayout.lines.get(childPosition);
	        	ImageCell image = currentLine.getImage(itemIndex);
	        	
	    		int outLayout[] = new int[2]; //Width, Height
	    		imageLayout(outLayout, image.inWidth, image.inHeight);
	        	
	    		//Alpha Anim
				AlphaAnimation alphaAnimation = new AlphaAnimation(0.2f, 0.6f);  
				alphaAnimation.setDuration(400);
				
	            //Scale Anim
	        	float toX = (float)image.outWidth/(float)outLayout[0];
	        	float toY = (float)image.outHeight/(float)outLayout[1];
				ScaleAnimation scaleAnimation = new ScaleAnimation(1 / toX, 1,
						1 / toY, 1, Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f);
				scaleAnimation.setDuration(400);

	            //Animation set  
	            AnimationSet set = new AnimationSet(true);
	            set.addAnimation(alphaAnimation); 
	            set.addAnimation(scaleAnimation); 

	            view.startAnimation(set);

	            line.bringChildToFront(view);
	            
				for(int i= 0; i < mImageLocation.length; i++) {
					mImageLocation[i] = -1;
				}
			}
		}
		
		public void imageLayout(int outLayout[], int iWidth, int iHeight) {
	    	float layoutWidth = 0.0f, layoutHeight = 0.0f;
	    	
	    	Point outPoint = new Point();
	    	getWindowManager().getDefaultDisplay().getSize(outPoint);
	    	
	    	int screenWidth = outPoint.x;
	    	int screenHeight = outPoint.y;
	    	
	    	
			float yRatio = (float)iHeight / (float)iWidth;
			
			if (yRatio >= 1.0f) {
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == Config.REQUEST_CODE
				&& resultCode == Config.REQUEST_CODE) {

			int aImageLocation[] = mImageAnimObj.getImageLocation();
			int itemIndex = data.getIntExtra(Config.DISPLAY_ITEM_INDEX, 0);
			
			int changeState = mImageAnimObj.getChangeState(itemIndex);
			
			mAdapter.notifyDataSetChanged();


			if (mImageAnimObj.TOP_DISPLAY == changeState) {
				
				setSelection(aImageLocation[0]);
			} else if (mImageAnimObj.BOTTOM_DISPLAY == changeState) {
				
				ImageLineGroup currentLine = mGalleryLayout.lines.get(aImageLocation[0]);
				int y = getListView().getHeight() - currentLine.height;
				getListView().setSelectionFromTop(aImageLocation[0], y);
			}
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
}