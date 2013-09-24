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
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
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
	private GroupImageAnim mGroupImageAnim = null;
	
	private HorizontalListView  mHorizontalListView;
	private ListView  mVerticalListView;
	
	//Screen Info
	private int mContentTop = 0;
	private Config mConfig = null;
	
	private void fetchImageList(int album_index){
		
		Cursor galleryList = getContentResolver().query(
				Config.MEDIA_URI, 
				Config.ALBUM_PROJECTION, 
				Config.ALBUM_WHERE_CLAUSE, 
				null, null);
	
		mImageList = new ArrayList<String>();
		mImageIds = new ArrayList<Long>();
		
		galleryList.moveToPosition(album_index);
		
		String album = galleryList.getString(galleryList.getColumnIndex(Media.BUCKET_DISPLAY_NAME));
		int folderType = album.compareTo("Camera") == 0 ? Config.CAMERA_FOLDER:Config.COMMON_FOLDER;
		
		if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			mGalleryLayout = new GalleryLayout(0, 
					mConfig.screenHeight - mContentTop,
					Config.IMAGE_LIST_THUMBNAIL_PADDING,
					mConfig.currentOrientation,
					folderType);
		} 
		else {
			mGalleryLayout = new GalleryLayout(mConfig.screenWidth,
					0,
					Config.IMAGE_LIST_THUMBNAIL_PADDING,
					mConfig.currentOrientation,
					folderType);
		}
		
		String galleryId = galleryList.getString(galleryList.getColumnIndex(Media.BUCKET_ID));
		Cursor listCursor = getContentResolver().query(
				Config.MEDIA_URI, 
				Config.IMAGE_PROJECTION, 
				Config.IMAGE_WHERE_CLAUSE, 
				new String[]{galleryList.getString(galleryList.getColumnIndex(Media.BUCKET_ID))}, 
				null);
		
		listCursor.moveToLast();
		
		int j = 0;
		
		while (false == listCursor.isBeforeFirst()){
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
			listCursor.moveToPrevious();
		}
		mGalleryLayout.addImageFinish();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mAdapter = new ImageListAdapter(this);
		
        //Screen Info
		mConfig = new Config(this);
        mContentTop = Config.STATUS_BAR_HEIGHT;
		
		if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			setContentView(R.layout.horizontal_list_view);
			mHorizontalListView = (HorizontalListView) this.findViewById(R.id.horizontalListView);
			mHorizontalListView.setAdapter(mAdapter);
			mImageAnimObj = new ImageAnim(this, (ViewGroup)findViewById(R.id.horizontal_root_container));
		}
		else {
	        setContentView(R.layout.vertical_list_view);
	        mVerticalListView = (ListView) this.findViewById(R.id.verticalListView);
	        mVerticalListView.setAdapter(mAdapter);
	        mImageAnimObj = new ImageAnim(this, (ViewGroup)findViewById(R.id.vertical_root_container));
		}
		
		int album_index = getIntent().getIntExtra (Config.ALBUM_INDEX, 0);
		fetchImageList(album_index);
		
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
    	
		int scrollImageSum = 0;
		int line = 0;
		if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			
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
			
			if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {

				int shiftX = 0;
				
				for(int j = 0; j < ret; j++) {
					shiftX += mGalleryLayout.lines.get(j).getWidth();
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
	            	holder.icons[i].setVisibility(View.GONE);
					holder.icons[i].setPadding(
							Config.IMAGE_LIST_THUMBNAIL_PADDING,
							Config.IMAGE_LIST_THUMBNAIL_PADDING,
							Config.IMAGE_LIST_THUMBNAIL_PADDING,
							Config.IMAGE_LIST_THUMBNAIL_PADDING);
					
	    			line.addView(holder.icons[i]);
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
					holder.icons[i].setVisibility(View.GONE);
				}
			}
			
			ImageLineGroup currentLine = mGalleryLayout.lines.get(position);
			
			/*Fill in the content*/
	        for (int i=0; i<currentLine.getImageNum(); i++){

				Bitmap bmp = mCacheAndAsyncWork.getBitmapFromRamCache(currentLine.getImage(i).id);
				ImageCell image = currentLine.getImage(i);

				holder.icons[i].setLayoutParams(new AbsoluteLayout.LayoutParams(
								image.outWidth + 2*Config.IMAGE_LIST_THUMBNAIL_PADDING,
								image.outHeight + 2*Config.IMAGE_LIST_THUMBNAIL_PADDING,
								image.outX, image.outY));
				holder.icons[i].setVisibility(View.VISIBLE);
				holder.icons[i].setScaleType(ImageView.ScaleType.FIT_XY);
				
				if (bmp != null) {
					holder.icons[i].setImageBitmap(bmp);
				} else {
					CacheAndAsyncWork.BitmapWorkerTask task = mCacheAndAsyncWork.new BitmapWorkerTask(
							holder.icons[i], context.getContentResolver(), mImageList);
					task.execute(image.id, (long) image.outWidth,
							(long) image.outHeight, (long) image.position, (long) image.crop);
					holder.task[i] = task;
					holder.icons[i].setImageResource(R.drawable.grey);
				}

				holder.icons[i].setId(image.position);
				mImageAnimObj.anim(position, i, holder.icons[i]);
			}
	        
			if (mGroupImageAnim != null) {
				mGroupImageAnim.layoutAnim(line, position);
			}
			return line;
		}
		
	    class ViewHolder{
	    	GestureImageView icons[] = new GestureImageView[Config.THUMBNAILS_PER_LINE];
			CacheAndAsyncWork.BitmapWorkerTask task[] = new CacheAndAsyncWork.BitmapWorkerTask[Config.THUMBNAILS_PER_LINE];
		}
	}
	
	public void entryImageDetail(View v) {
		 
		//Store display state
		mImageAnimObj.storeDisplayState();
		
		int clickItemInfo[] = new int[4];
		v.getLocationOnScreen (clickItemInfo);
		
    	clickItemInfo[1] -= mContentTop;
		
		clickItemInfo[2] = v.getWidth() - 2*Config.IMAGE_LIST_THUMBNAIL_PADDING;
		clickItemInfo[3] = v.getHeight() - 2*Config.IMAGE_LIST_THUMBNAIL_PADDING;
		
        final Intent i = new Intent(ImageList.this, ImageDetail.class);
        i.putStringArrayListExtra(Config.IMAGE_LIST, mImageList);
        i.putExtra(Config.CLICK_INDEX, v.getId());
        i.putExtra(Config.CLICK_ITEM_INFO, clickItemInfo);
        i.putExtra(Config.THUMBNAIL_ID, mImageIds.get(v.getId()));
        
        startActivityForResult(i, Config.REQUEST_CODE);
        
        overridePendingTransition(R.anim.out, R.anim.in);
        mGroupImageAnim = null;
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
			
			if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				landscapeAnimLine();
			}
			else {
				portraitAnimLine();
			}
		}
		
		public void layoutAnim(AbsoluteLayout line, int childPosition) {
			
			if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
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
			
			int contentHeight = mConfig.screenHeight - mContentTop;
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
					translateAnimation.setDuration(Config.ANIM_DURATION);

					// Scale Anim
					ScaleAnimation scaleAnimation = new ScaleAnimation(0.5f,
							1.0f, 0.5f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					scaleAnimation.setDuration(Config.ANIM_DURATION);

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
					translateAnimation.setDuration(Config.ANIM_DURATION);

					// Scale Anim
					ScaleAnimation scaleAnimation = new ScaleAnimation(0.5f,
							1.0f, 0.5f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					scaleAnimation.setDuration(Config.ANIM_DURATION);

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
		private int mItemIndex = 0;
		
		static public final int TOP_DISPLAY = 1;
		static public final int NOT_CHANGE = 0;
		static public final int BOTTOM_DISPLAY = -1;
		
		private Context context;
		private ViewGroup container;
		private Bitmap bmp = null;
		private ImageView animImage = null;
		
		ImageAnim(Context context, ViewGroup container) {
			
			this.context = context;
			this.container = container;
			
			mImageLocation = new int[2];
			for(int i = 0; i < mImageLocation.length; i++) {
				mImageLocation[i] = -1;
			}
			
			mDisplayState = new int[2];
			for(int i = 0; i < mDisplayState.length; i++) {
				mDisplayState[i] = -1;
			}
		}
		
		public void storeDisplayState() {
			
			if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				
				int beginX = mHorizontalListView.getCurrentX();
				int endX = beginX + mConfig.screenWidth;
				int i = 0;
				for(i = 0; i < mGalleryLayout.getLineNum(); i++) {
					beginX -= mGalleryLayout.lines.get(i).getWidth();
					if(beginX < 0) {
						break;
					}
				}
				
				mDisplayState[0] = i;
				
				for(i = 0; i < mGalleryLayout.getLineNum(); i++) {
					endX -= mGalleryLayout.lines.get(i).getWidth();
					if(endX < 0) {
						break;
					}
				}
				
				mDisplayState[1] = i;
			}
			else {
				mDisplayState[0] = mVerticalListView.getFirstVisiblePosition();
				mDisplayState[1] = mVerticalListView.getLastVisiblePosition();
			}
		}
		
		public void calculateChildPosition() {
			
			mImageLocation[0] = -1;
			int totalLines = mGalleryLayout.getLineNum();
			
			int sum = 0;
			for(int i = 0; i < totalLines; i++) {
				sum += mGalleryLayout.lines.get(i).imageList.size();
				if(mItemIndex < sum) {
					mImageLocation[0] = i;
					break;
				}
			}
			
			if (-1 != mImageLocation[0]) {
				mImageLocation[1] = mItemIndex - (sum - mGalleryLayout.lines.get(mImageLocation[0]).imageList.size());
			}
		}
		
		public int getChangeState(int itemIndex) {
			
			int state = NOT_CHANGE;
			
			mItemIndex = itemIndex;
			calculateChildPosition();
			
			if(mImageLocation[0] <= mDisplayState[0]) {
				state = TOP_DISPLAY;
			}
			else if(mImageLocation[0] >= mDisplayState[1]) {
				state = BOTTOM_DISPLAY;
			}
			return state;
		}
		
		public void virtualImageAnim(final ViewTreeObserver observer, final int changeState) {

			observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
				
				@Override
				public boolean onPreDraw() {
					observer.removeOnPreDrawListener(this);
					
					Long id = mImageIds.get(mItemIndex);
					
					bmp = mCacheAndAsyncWork.getBitmapFromRamCache(id);
					
					if(bmp == null) {
						bmp = Thumbnails.getThumbnail(context.getContentResolver(), id, Thumbnails.MINI_KIND, null);
					}
					
		        	ImageLineGroup currentLine = mGalleryLayout.lines.get(mImageLocation[0]);
		        	ImageCell image = currentLine.getImage(mImageLocation[1]);
		        	
		    		int outLayout[] = new int[2]; //Width, Height
		    		
		    		ImageDetailFragment.imageLayout(context, outLayout, image.inWidth, image.inHeight);
		    			
		    		float startX = (float)(mConfig.screenWidth - image.outWidth)/2;
		    		float startY = (float)(mConfig.screenHeight - image.outHeight)/2;
		    		float endX = 0;
		    		float endY = 0;
		        	
					if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
						
						int left = 0;
						if(TOP_DISPLAY == changeState) {
							left = 0;
						}
						else if(BOTTOM_DISPLAY == changeState) {
							left = mConfig.screenWidth - mGalleryLayout.lines.get(mImageLocation[0]).getWidth();
						}
						else {
							int diff = mImageLocation[0] - mDisplayState[0];
							left = mHorizontalListView.getChildAt(diff).getLeft();
						}
						
						Log.v("t-galleryanim", "mImageLocation[0]: " + mImageLocation[0] + "  ,left: " + left);

			    		endX = left + image.outX;
			    		endY = image.outY;
					} else {
						int diff = mImageLocation[0] - mVerticalListView.getFirstVisiblePosition();
						int top = mVerticalListView.getChildAt(diff).getTop();
						
			    		endX = image.outX;
			    		endY = top + image.outY;
					}
		    		animImage = new ImageView(context);
		    		animImage.setImageBitmap(bmp);
		    		animImage.setX(endX);
		    		animImage.setY(endY);
		    		animImage.setAlpha(0.5f);
		    		animImage.setTranslationX(startX);
		    		animImage.setTranslationY(startY);
		    		animImage.animate().translationX(endX).setDuration(Config.ANIM_DURATION);
		    		animImage.animate().translationY(endY).setDuration(Config.ANIM_DURATION);
		    		
		        	float toX = (float)image.outWidth/(float)outLayout[0];
		        	float toY = (float)image.outHeight/(float)outLayout[1];
		    		
		        	animImage.setScaleX(1/toX);
		        	animImage.setScaleY(1/toY);
		        	animImage.animate().scaleX(1).setDuration(Config.ANIM_DURATION);
		        	animImage.animate().scaleY(1).setDuration(Config.ANIM_DURATION).withEndAction(new Runnable(){
						@Override
						public void run() {
							container.removeView(animImage);
							animImage = null;
						}
						
					});
					
					container.addView(animImage, new ViewGroup.LayoutParams(image.outWidth + 2*Config.IMAGE_LIST_THUMBNAIL_PADDING,
							image.outHeight + 2*Config.IMAGE_LIST_THUMBNAIL_PADDING));
					
					for(int i= 0; i < mImageLocation.length; i++) {
						mImageLocation[i] = -1;
					}
					
					return false;
				}
			});
			
		}
		
		public void anim(int childPosition, int index, ImageView view) {
			if (childPosition == mImageLocation[0] && index == mImageLocation[1]) {
				
				ScaleAnimation scaleAnimation = new ScaleAnimation(0.9f, 1,
						0.9f, 1, Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f);
				scaleAnimation.setDuration(Config.ANIM_DURATION);

	            view.startAnimation(scaleAnimation);
			}
		}
		
		public void listChange(int itemIndex) {

			int changeState = getChangeState(itemIndex);
			
			if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {

				int shiftX = 0;
				
				if (mImageAnimObj.TOP_DISPLAY == changeState) {

					for(int i = 0; i < mImageLocation[0]; i++) {
						shiftX += mGalleryLayout.lines.get(i).getWidth();
					}
				} else if (mImageAnimObj.BOTTOM_DISPLAY == changeState) {
					
					for(int i = 0; i < mImageLocation[0]; i++) {
						shiftX += mGalleryLayout.lines.get(i).getWidth();
					}
					
					shiftX -=(mConfig.screenWidth - mGalleryLayout.lines.get(mImageLocation[0]).getWidth());
				}
				else {
					shiftX = mHorizontalListView.getCurrentX();
				}
				mHorizontalListView.scrollTo(shiftX);
				virtualImageAnim(mHorizontalListView.getViewTreeObserver(), changeState);
			}
			else {
				mAdapter.notifyDataSetChanged();
				if (TOP_DISPLAY == changeState) {

					mVerticalListView.setSelection(mImageLocation[0]);
				} else if (BOTTOM_DISPLAY == changeState) {
					
					ImageLineGroup currentLine = mGalleryLayout.lines.get(mImageLocation[0]);
					int y = mVerticalListView.getHeight() - currentLine.height;
					mVerticalListView.setSelectionFromTop(mImageLocation[0], y);
				}

				virtualImageAnim(mVerticalListView.getViewTreeObserver(), changeState);
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == Config.REQUEST_CODE && resultCode == Config.REQUEST_CODE) {
			int itemIndex = data.getIntExtra(Config.DISPLAY_ITEM_INDEX, 0);
			mImageAnimObj.listChange(itemIndex);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}