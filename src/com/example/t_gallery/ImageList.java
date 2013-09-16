package com.example.t_gallery;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Menu;
import android.view.View;
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
import android.widget.ListView;

public class ImageList extends Activity {
	private ArrayList<String> mImageList = null;
	private ArrayList<Long> mImageIds = null;
	public GalleryLayout mGalleryLayout = null;
	private ImageAnim mImageAnimObj = null;
	private ImageListAdapter mAdapter = null;
	private CacheAndAsyncWork mCacheAndAsyncWork = null;
	private Config mConfig = null;
	private GroupImageAnim mGroupImageAnim = null;
	
	private HorizontalListView  mHorizontalListView;
	private ListView  mVerticalListView;
	
	private void fetchImageList(int album_index){
		
		Cursor galleryList = getContentResolver().query(
				Config.MEDIA_URI, 
				Config.ALBUM_PROJECTION, 
				Config.ALBUM_WHERE_CLAUSE, 
				null, null);
	
		mImageList = new ArrayList<String>();
		mImageIds = new ArrayList<Long>();
		
		if (mConfig.curent_orientation == Configuration.ORIENTATION_LANDSCAPE) {
	    	//int contentTop = this.getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
			
			mGalleryLayout = new GalleryLayout(0, mConfig.screenHeight - 100, Config.IMAGE_LIST_THUMBNAIL_PADDING, 0);
		} 
		else {
			mGalleryLayout = new GalleryLayout(mConfig.screenWidth, 0,  Config.IMAGE_LIST_THUMBNAIL_PADDING, 1);
		}
		
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
				
				j++;
				mImageList.add(path);
				mImageIds.add(id);
			}
			listCursor.moveToNext();
		}
		mGalleryLayout.addImageFinish();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mConfig = new Config(this);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mAdapter = new ImageListAdapter(this);
		
		if (mConfig.curent_orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setContentView(R.layout.horizontal_list_view);
			mHorizontalListView = (HorizontalListView) this.findViewById(R.id.horizontalListView);
			mHorizontalListView.setAdapter(mAdapter);
		}
		else {
	        setContentView(R.layout.vertical_list_view);
	        mVerticalListView = (ListView) this.findViewById(R.id.verticalListView);
	        mVerticalListView.setAdapter(mAdapter);
		}
		
		int album_index = getIntent().getIntExtra (Config.ALBUM_INDEX, 0);
		
		fetchImageList(album_index);						
		mImageAnimObj = new ImageAnim(this);
		mCacheAndAsyncWork = new CacheAndAsyncWork();
		int selectedPosition = restorePrevState(); //Must after fetchImageList
		
		if (Utils.getPrefAnimFlag(this) == true) {
			mGroupImageAnim = new GroupImageAnim(getIntent().getIntArrayExtra(
					Config.CLICK_ITEM_INFO), selectedPosition);
		}
	}
	
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	Log.v("t-gallery", "image list destroy");
		
		int scrollImageSum = 0;
		int line = 0;
		if (mConfig.curent_orientation == Configuration.ORIENTATION_LANDSCAPE) {
			
			int currentX = mHorizontalListView.getCurrentX();
			int i = 0;
			for(i = 0; i < mGalleryLayout.getLineNum(); i++) {
				currentX -= mGalleryLayout.lines.get(i).getWidth();
				if(currentX < 0) {
					break;
				}
			}
			
			line = i;
			
		} else {
			if(mVerticalListView.getLastVisiblePosition() == (mGalleryLayout.getLineNum()-1)) {
				line = mGalleryLayout.getLineNum();
			}
			else {
				line = mVerticalListView.getFirstVisiblePosition();
			}
		}

		for(int i = 0; i < line; i++) {
			scrollImageSum += mGalleryLayout.lines.get(i).imageList.size();
		}
		
		Utils.putPrefScrollImageSum(this, scrollImageSum);
		Utils.putPrefAnimFlag(this, false);
	}  
	
	private int restorePrevState() {
		
		// Restore preferences
		int scrollImageSum = Utils.getPrefScrollImageSum(this);
		int ret = 0;
		
		if (scrollImageSum > 0) {
			
			int i = 0;

			for (i = 0; i < mGalleryLayout.getLineNum(); i++) {
				scrollImageSum -= mGalleryLayout.lines.get(i).imageList.size();
				if (scrollImageSum == 0) {
					i++;
					break;
				} else if (scrollImageSum < 0) {
					break;
				}
			}

			if (i >= mGalleryLayout.getLineNum()) {
				i = mGalleryLayout.getLineNum() - 1;
			}
			
			ret = i;
			
			if (mConfig.curent_orientation == Configuration.ORIENTATION_LANDSCAPE) {

				int shiftX = 0;
				
				for(int j = 0; j < ret; j++) {
					shiftX += mGalleryLayout.lines.get(i).getWidth();
				}
				
				mHorizontalListView.scrollTo(shiftX);
			} else {
				
				mVerticalListView.setSelection(ret);
			}
		}
		return ret;
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
			int count = 0;
			
			if(mGalleryLayout != null) {
				count = mGalleryLayout.getLineNum();
			}
			
			return count;
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
	            	holder.icons[i] = new GestureImageView(context, 2);
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
	        		
					holder.icons[i].setId(image.position);
					mImageAnimObj.anim(line, position, i, holder.icons[i]);
	        	}
			}
	        
			if (mGroupImageAnim != null) {
				mGroupImageAnim.layoutAnim(line, position);
			}
			return line;
		}
		
	    class ViewHolder{
	    	GestureImageView icons[] = new GestureImageView[Config.THUMBNAILS_PER_LINE];
			CacheAndAsyncWork.BitmapWorkerTask task[] = new CacheAndAsyncWork.BitmapWorkerTask[Config.THUMBNAILS_PER_LINE];
			boolean inTransient = false;
		}
	}
	
	public void entryImageDetail(View v) {
		 
		//Store display state
		int aDisplayState[] = mImageAnimObj.getDisplayStatus();
		
		if (mConfig.curent_orientation == Configuration.ORIENTATION_LANDSCAPE) {
			aDisplayState[0] = mHorizontalListView.getFirstVisiblePosition();
			aDisplayState[1] = mHorizontalListView.getLastVisiblePosition();
		}
		else {
			aDisplayState[0] = mVerticalListView.getFirstVisiblePosition();
			aDisplayState[1] = mVerticalListView.getLastVisiblePosition();
		}
		
		int clickItemInfo[] = new int[4];
		v.getLocationOnScreen (clickItemInfo);
		clickItemInfo[2] = v.getWidth();
		clickItemInfo[3] = v.getHeight();
		
        final Intent i = new Intent(ImageList.this, ImageDetail.class);
        i.putStringArrayListExtra(Config.IMAGE_LIST, mImageList);
        i.putExtra(Config.CLICK_INDEX, v.getId());
        i.putExtra(Config.CLICK_ITEM_INFO, clickItemInfo);
        i.putExtra(Config.THUMBNAIL_ID, mImageIds.get(v.getId()));
        
        startActivityForResult(i, Config.REQUEST_CODE);
        
        overridePendingTransition(R.anim.fade, R.anim.hold);
	}
	
	class GroupImageAnim {
		private int start_location[];
		private int mSelectedPosition;
		private int animLine[];
		private int animLineNum = 0;
		
		GroupImageAnim(int location[], int selectedPosition) {

			start_location = location;
			mSelectedPosition = selectedPosition;
			animLine = new int[2];
			
			calculateAnimLine();
		}
		
		public void calculateAnimLine() {
			
			if (mConfig.curent_orientation == Configuration.ORIENTATION_LANDSCAPE) {
				landscapeAnimLine();
			}
			else {
				portraitAnimLine();
			}
		}
		
		public void layoutAnim(AbsoluteLayout line, int childPosition) {
			
			if (mConfig.curent_orientation == Configuration.ORIENTATION_LANDSCAPE) {
				landscapeLayoutAnim(line, childPosition);
			}
			else {
				portraitLayoutAnim(line, childPosition);
			}
		}
		
		private void landscapeAnimLine() {
			
			int totalWidth = 0;
			int i = mSelectedPosition;
			
			animLine[0] = mSelectedPosition;
			
			for(; i < mGalleryLayout.getLineNum(); i++) {
				
				totalWidth += mGalleryLayout.lines.get(i).getWidth();
				if(totalWidth >= mConfig.screenWidth) {
					break;
				}
			}
			
			if (i < mGalleryLayout.getLineNum()) {
				animLine[1] = i;
			} else {
				animLine[1] = mGalleryLayout.getLineNum() - 1;
			}
			
			if(mSelectedPosition > 0 && totalWidth < mConfig.screenWidth) {
				
				i = mSelectedPosition-1;
				for(; i>=0; i--) {
					totalWidth += mGalleryLayout.lines.get(i).getWidth();
					
					if(totalWidth >= mConfig.screenWidth) {
						break;
					}
				}
				
				if (i >= 0) {
					animLine[0] = i;
				} else {
					animLine[0] = 0;
				}
			}
			
			animLineNum = animLine[1]-animLine[0]+1;
		}
		
		private void portraitAnimLine() {
			
			int contentTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
			int contentHeight = mConfig.screenHeight - contentTop;
			int totalHeight = 0;
			int i = mSelectedPosition;
			
			animLine[0] = mSelectedPosition;
			
			for(; i < mGalleryLayout.getLineNum(); i++) {
				
				totalHeight += mGalleryLayout.lines.get(i).getHeight();
				if(totalHeight >= contentHeight) {
					break;
				}
			}
			
			if (i < mGalleryLayout.getLineNum()) {
				animLine[1] = i;
			} else {
				animLine[1] = mGalleryLayout.getLineNum() - 1;
			}
			
			if(mSelectedPosition > 0 && totalHeight < contentHeight) {
				
				i = mSelectedPosition-1;
				for(; i>=0; i--) {
					totalHeight += mGalleryLayout.lines.get(i).getHeight();
					
					if(totalHeight >= contentHeight) {
						break;
					}
				}
				
				if (i >= 0) {
					animLine[0] = i;
				} else {
					animLine[0] = 0;
				}
			}
			
			animLineNum = animLine[1]-animLine[0]+1;
		}
		
		private void landscapeLayoutAnim(AbsoluteLayout line, int childPosition) {
			
			if (animLineNum > 0) {
				
				if (childPosition >= animLine[0] && childPosition <= animLine[1]) {
					
					ImageLineGroup currentLine = mGalleryLayout.lines.get(childPosition);
					int lineWidth = 0;
					int shiftLine = childPosition - animLine[0];

					for (int i = 0; i < shiftLine; i++) {
						
						lineWidth += mGalleryLayout.lines.get(i).getWidth();
					}

					float startX = (float) start_location[0] + (float) lineWidth / 2.0f;
					float startY = (float) start_location[1];

					float endX = (float) lineWidth + (float) currentLine.getWidth() / 4.0f;
					float endY = (float) currentLine.getHeight() / 4.0f;

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
					
					animLineNum--;
				}
			}
		}
		
		private void portraitLayoutAnim(AbsoluteLayout line, int childPosition) {
			
			if (animLineNum > 0) {
				
				if (childPosition >= animLine[0] && childPosition <= animLine[1]) {
					
					ImageLineGroup currentLine = mGalleryLayout.lines.get(childPosition);
					int lineHeight = 0;
					int shiftLine = childPosition - animLine[0];

					for (int i = 0; i < shiftLine; i++) {
						
						lineHeight += mGalleryLayout.lines.get(i).getHeight();
					}

					float startX = (float) start_location[0];
					float startY = (float) start_location[1]
							+ (float) lineHeight / 2.0f;

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
					
					animLineNum--;
				}
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
		
		public void test(int childPosition, int itemIndex, ImageView view) {

	        	ImageLineGroup currentLine = mGalleryLayout.lines.get(childPosition);
	        	ImageCell image = currentLine.getImage(itemIndex);
	        	
	    		int outLayout[] = new int[2]; //Width, Height
	    		
	    		Utils.imageLayout(context, outLayout, image.inWidth, image.inHeight);
	        	
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
		}
		
		public void anim(AbsoluteLayout line, int childPosition, int itemIndex, ImageView view) {
			if (childPosition == mImageLocation[0] && itemIndex == mImageLocation[1]) {
				
	        	ImageLineGroup currentLine = mGalleryLayout.lines.get(childPosition);
	        	ImageCell image = currentLine.getImage(itemIndex);
	        	
	    		int outLayout[] = new int[2]; //Width, Height
	    		
	    		Utils.imageLayout(context, outLayout, image.inWidth, image.inHeight);
	        	
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

			if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

				mHorizontalListView.setSelection(aImageLocation[0]);
			}
			else {
				if (mImageAnimObj.TOP_DISPLAY == changeState) {
					
					mVerticalListView.setSelection(aImageLocation[0]);
				} else if (mImageAnimObj.BOTTOM_DISPLAY == changeState) {
					
					ImageLineGroup currentLine = mGalleryLayout.lines.get(aImageLocation[0]);
					int y = mVerticalListView.getHeight() - currentLine.height;
					mVerticalListView.setSelectionFromTop(aImageLocation[0], y);
				}
			}
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
}