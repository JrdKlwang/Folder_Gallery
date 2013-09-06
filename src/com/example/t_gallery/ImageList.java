package com.example.t_gallery;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsoluteLayout;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ImageList extends ListActivity {
	private ArrayList<String> mImageList;
	public GalleryLayout mGalleryLayout;
	private ImageAnim mImageAnimObj;
	private ImageListAdapter mAdapter;
	private CacheAndAsyncWork mCacheAndAsyncWork;
	private Config mConfig;
	private GroupImageAnim mGroupImageAnim;
	
	private void fetchImageList(int album_index){
		
		Cursor galleryList = getContentResolver().query(
				Config.MEDIA_URI, 
				Config.ALBUM_PROJECTION, 
				Config.ALBUM_WHERE_CLAUSE, 
				null, null);
	
		mImageList = new ArrayList<String>();
		
		mGalleryLayout = new GalleryLayout(1080, Config.IMAGE_LIST_THUMBNAIL_PADDING);
		
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
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_image_list);
		
		int album_index = getIntent().getIntExtra (Config.ALBUM_INDEX, 0);
		fetchImageList(album_index);						
		
		mAdapter = new ImageListAdapter(this);
		setListAdapter(mAdapter);
		
		mImageAnimObj = new ImageAnim(this);
		
		mCacheAndAsyncWork = new CacheAndAsyncWork();
		
		mConfig = new Config(this);
	
		mGroupImageAnim = new GroupImageAnim(getIntent().getIntArrayExtra(Config.CLICK_ITEM_INFO));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_gallery_list, menu);
		return true;
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
				line = new AbsoluteLayout(context);
				
	            holder = new ViewHolder();
	            
	            for (int i=0; i<Config.THUMBNAILS_PER_LINE; i++){
	            	holder.icons[i] = new ImageView(getApplicationContext());
	            	holder.icons[i].setBackgroundColor(0xFF000000);
	            	holder.icons[i].setAdjustViewBounds(true);
					holder.icons[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
					holder.icons[i].setPadding(
							Config.IMAGE_LIST_THUMBNAIL_PADDING,
							Config.IMAGE_LIST_THUMBNAIL_PADDING,
							Config.IMAGE_LIST_THUMBNAIL_PADDING,
							Config.IMAGE_LIST_THUMBNAIL_PADDING);
					
	    			line.addView(holder.icons[i], new AbsoluteLayout.LayoutParams(Config.THUMBNAIL_BOUND_WIDTH,Config.THUMBNAIL_BOUND_HEIGHT,0,0));
	            }
				
				line.setTag(R.id.data_holder, holder);
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
	        		Bitmap bmp = mCacheAndAsyncWork.getBitmapFromRamCache(currentLine.getImage(i).id);
	        		ImageCell image = currentLine.getImage(i);
	      
	        		
					holder.icons[i].setLayoutParams(new AbsoluteLayout.LayoutParams(
									image.outWidth + 2*Config.IMAGE_LIST_THUMBNAIL_PADDING,
									image.outHeight + 2*Config.IMAGE_LIST_THUMBNAIL_PADDING,
									image.outX, image.outY));
	        		holder.icons[i].setVisibility(View.VISIBLE);
	        		
	        		if (bmp != null){
	        			holder.icons[i].setImageBitmap(bmp);
	        		}
	        		else {
						CacheAndAsyncWork.BitmapWorkerTask task = mCacheAndAsyncWork.new BitmapWorkerTask(
								holder.icons[i], context.getContentResolver(), mImageList);
					    task.execute(image.id, (long)image.outWidth, (long)image.outHeight, (long)image.position, (long)image.crop);
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
					
					mImageAnimObj.anim(line, position, i, holder.icons[i]);
	        	}
			}
	        
			mGroupImageAnim.layoutAnim(line, position);
			return line;
		}
		
	    class ViewHolder{
			ImageView icons[] = new ImageView[Config.THUMBNAILS_PER_LINE];
			CacheAndAsyncWork.BitmapWorkerTask task[] = new CacheAndAsyncWork.BitmapWorkerTask[Config.THUMBNAILS_PER_LINE];
			boolean inTransient = false;
		}
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
        i.putStringArrayListExtra(Config.IMAGE_LIST, mImageList);
        i.putExtra(Config.CLICK_INDEX, v.getId());
        i.putExtra(Config.CLICK_ITEM_INFO, clickItemInfo);
        
        startActivityForResult(i, Config.REQUEST_CODE);
	}
	
	class GroupImageAnim {
		private boolean stopAnim = false;
		private int start_location[];
		
		GroupImageAnim(int location[]) {

			start_location = location;
		}
		
		private void layoutAnim(AbsoluteLayout line, int childPosition) {
			
			if (stopAnim == false) {
				
				int contentTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
				int lineHeight = 0;

				for (int i = 0; i < childPosition; i++) {
					lineHeight += mGalleryLayout.lines.get(i).getHeight();
				}

				ImageLineGroup currentLine = mGalleryLayout.lines.get(childPosition);
				
				if ((childPosition == (mGalleryLayout.getLineNum() - 1))
						|| ((lineHeight + currentLine.getHeight()) > (mConfig.screenHeight - contentTop))) {
					stopAnim = true;
				}
				
				float startX = (float)start_location[0];
				float startY = (float)start_location[1] + (float) lineHeight / 2.0f;

				float endX = (float) mConfig.screenWidth / 4.0f;
				float endY = (float) lineHeight + (float) currentLine.getHeight() / 4.0f;

				float fromXDelta = startX - endX;
				float fromYDelta = startY - endY;
				TranslateAnimation translateAnimation = new TranslateAnimation(2 * fromXDelta, 0, 2 * fromYDelta, 0);
				translateAnimation.setDuration(300);

				// Scale Anim
				ScaleAnimation scaleAnimation = new ScaleAnimation(0.5f,
						1.0f, 0.5f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f);
				scaleAnimation.setDuration(300);

				// Animation set
				AnimationSet set = new AnimationSet(true);
				set.addAnimation(translateAnimation);
				set.addAnimation(scaleAnimation);

				line.startAnimation(set);
			}
		}
	}
	
	class ImageAnim {
		
		private int mImageLocation[]; //[0]: ChildPosition, [1]:(itemIndex)Which one in line
		private int mDisplayState[]; //[0]:firstVisibleIndex, [1]:lastVisibleIndex
		
		static public final int TOP_DISPLAY = 1;
		static public final int NOT_CHANGE = 0;
		static public final int BOTTOM_DISPLAY = -1;
		
		private Context context;
		
		ImageAnim(Context context) {
			
			this.context = context;
			
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
		
		public void anim(AbsoluteLayout line, int childPosition, int itemIndex, ImageView view) {
			if (childPosition == mImageLocation[0] && itemIndex == mImageLocation[1]) {
				
	        	ImageLineGroup currentLine = mGalleryLayout.lines.get(childPosition);
	        	ImageCell image = currentLine.getImage(itemIndex);
	        	
	    		int outLayout[] = new int[2]; //Width, Height
	    		
	    		ImageDetail detailObj = new ImageDetail();
	    		ImageDetail.ImageDetailFragment fragmentObj = detailObj.new ImageDetailFragment();
	    		fragmentObj.imageLayout(context, outLayout, image.inWidth, image.inHeight);
	        	
	    		//Alpha Anim
				AlphaAnimation alphaAnimation = new AlphaAnimation(0.2f, 0.6f);  
				alphaAnimation.setDuration(300);
				
	            //Scale Anim
	        	float toX = (float)image.outWidth/(float)outLayout[0];
	        	float toY = (float)image.outHeight/(float)outLayout[1];
				ScaleAnimation scaleAnimation = new ScaleAnimation(1 / toX, 1,
						1 / toY, 1, Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f);
				scaleAnimation.setDuration(300);

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