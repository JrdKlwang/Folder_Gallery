package com.example.t_gallery;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class FolderList extends Activity {
	private static String TAG = "New-Gallery FolderList";
	private ArrayList<ArrayList<Long>> mAlbumList = new ArrayList<ArrayList<Long>>();
	private ArrayList<String> mAlbumName = new ArrayList<String>();
	private ArrayList<Integer> mAlbumCount = new ArrayList<Integer>();
	private CacheAndAsyncWork mCacheAndAsyncWork;
	private Bitmap mDefaultLayerImage = null;
	
	//Screen Info
	private int mContentTop = 0;
	private Config mConfig = null;
	
	private void fetchFolderList(){
		
		Cursor galleryList = getContentResolver().query(
				Config.MEDIA_URI, 
				Config.ALBUM_PROJECTION, 
				Config.ALBUM_WHERE_CLAUSE, 
				null, null);
		
		for (int i=0; i<galleryList.getCount(); i++){

			galleryList.moveToPosition(i);
			
			ArrayList<Long> album = new ArrayList<Long>();
			
			Cursor ImageList = getContentResolver().query(
					Config.MEDIA_URI, 
					Config.IMAGE_PROJECTION, 
					Config.IMAGE_WHERE_CLAUSE, 
					new String[]{galleryList.getString(galleryList.getColumnIndex(Media.BUCKET_ID))}, 
					null);
			
			ImageList.moveToLast();

			while (false == ImageList.isBeforeFirst() && album.size() < Config.IMAGE_NUM_PRE_ALBUM) {
				long id = ImageList.getLong(ImageList.getColumnIndex(Media._ID));
				int width = ImageList.getInt(ImageList.getColumnIndex(Media.WIDTH));
				int height = ImageList.getInt(ImageList.getColumnIndex(Media.HEIGHT));
				
				if (width != 0 && height != 0) {
					album.add(id);
				}
				ImageList.moveToPrevious();
			}
			
			if(album.size() > 0) {
				mAlbumList.add(album);
				
				String albumName = galleryList.getString(galleryList.getColumnIndex(Media.BUCKET_DISPLAY_NAME));
				mAlbumName.add(albumName);
				mAlbumCount.add(ImageList.getCount());
			}
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Screen Info
        mConfig = new Config(this);
        mContentTop = Config.STATUS_BAR_HEIGHT + Config.TITLE_BAR_HEIGHT;
        
        fetchFolderList();
        
		if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			setContentView(R.layout.horizontal_list_view);
			HorizontalListView mListView = (HorizontalListView) this.findViewById(R.id.horizontalListView);
	        mListView.setAdapter(new FolderListAdapter(this));
	        mListView.setMaxX(getHorizontalListWidth());
		}
		else {
	        setContentView(R.layout.vertical_list_view);
	        ListView mListView = (ListView) this.findViewById(R.id.verticalListView);
	        mListView.setAdapter(new FolderListAdapter(this));
	        mListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
		}
		getWindow().setBackgroundDrawable(null);
		
        mCacheAndAsyncWork = new CacheAndAsyncWork();
        
        createDefaultLayerImage(mCacheAndAsyncWork);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	Utils.putPrefScrollImageSum(this, 0);
    }
    
    private void createDefaultLayerImage(CacheAndAsyncWork asyncWorkObj) {
		Bitmap bmp = ((BitmapDrawable) getResources().getDrawable( R.drawable.grey)).getBitmap();

		ArrayList<Bitmap> image_list = new ArrayList<Bitmap>();
		for(int i = 0; i < Config.IMAGE_NUM_PRE_ALBUM; i++) {
			image_list.add(bmp);
		}

		int bigImageWh = 0;
		if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			int contentHeight = mConfig.screenHeight - mContentTop;
		    bigImageWh = contentHeight/2 - 2*Config.ALBUM_LIST_PADDING;
		}
		else {
			int contentWidth = mConfig.screenWidth;
			bigImageWh = contentWidth / 2 - (Config.IMAGE_NUM_PRE_ALBUM-1)*Config.ALBUM_OVERLIE_IMAGE_WIDTH - 2*Config.ALBUM_LIST_PADDING;
		}
		mDefaultLayerImage = Utils.layerImages(image_list, bigImageWh, bigImageWh);
    }
    
    private int getHorizontalListWidth() {
		int count = 0;
		int totalAlbum = mAlbumList.size();
		
		if(totalAlbum%2 == 0) {
			count = totalAlbum/2;
		}
		else {
			count = totalAlbum/2 + 1;
		}
		
		int contentHeight = mConfig.screenHeight - mContentTop;
	    int width = (contentHeight/2 + 3*Config.ALBUM_OVERLIE_IMAGE_WIDTH)*count;
	    
	    return width;
    }
    
	class FolderListAdapter extends BaseAdapter {
		private Context context;
		private ViewHolder holder;

		public FolderListAdapter(Context context) {
			super();
			this.context = context;
		}

		@Override
		public int getCount() {
			int count = 0;
			int totalAlbum = mAlbumList.size();
			
			if(totalAlbum%2 == 0) {
				count = totalAlbum/2;
			}
			else {
				count = totalAlbum/2 + 1;
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
			LinearLayout line;
			holder = null;

			if (convertView == null) {
				line = new LinearLayout(context);
			} else {
				line = (LinearLayout) convertView;
			}

			AbsoluteLayout albumLayout = new AbsoluteLayout(context);
				
			initView(line, albumLayout, convertView);
			
			fillView(position * 2, 0);
			if (position * 2 + 1 < mAlbumList.size()) {
				fillView(position * 2 + 1, 1);
			}
			
			if(position == getCount()-1) {
				if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
					line.setPadding(0, 0, Config.ALBUM_LIST_PADDING, 0);
				} else {
					line.setPadding(0, 0, 0, Config.ALBUM_LIST_PADDING);
				}
			}
			else {
				line.setPadding(0, 0, 0, 0);
			}
			
			line.addView(albumLayout);
				
			return line;
		}

		private void initView(LinearLayout parent, AbsoluteLayout line, View convertView) {
			/* Prepare the view (no content yet) */
			if (convertView == null) {
				holder = new ViewHolder();

				for (int i = 0; i < 2; i++) {
					holder.icons[i] = new GestureImageView(context, 1);
					holder.icons[i].setBackgroundColor(0xFF000000);
					holder.icons[i].setVisibility(View.GONE);
					holder.icons[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
					line.addView(holder.icons[i]);
	
					holder.albumDescBg[i] = new ImageView(context);
					holder.albumDescBg[i].setBackgroundColor(0xFF000000);
					holder.albumDescBg[i].setVisibility(View.GONE);
					holder.albumDescBg[i].setAlpha(0.3f);
					line.addView(holder.albumDescBg[i]);

					holder.albumDescName[i] = new TextView(context);
					holder.albumDescName[i].setTextColor(Color.WHITE);
					holder.albumDescName[i].setTextSize(14);
					holder.albumDescName[i].setVisibility(View.GONE);
					holder.albumDescName[i].setAlpha(0.8f);
					holder.albumDescName[i].setSingleLine(true);
					holder.albumDescName[i].setEllipsize(TextUtils.TruncateAt.END);
					line.addView(holder.albumDescName[i]);

					holder.albumDescCount[i] = new TextView(context);
					holder.albumDescCount[i].setTextColor(Color.WHITE);
					holder.albumDescCount[i].setTextSize(12);
					holder.albumDescCount[i].setVisibility(View.GONE);
					holder.albumDescCount[i].setAlpha(0.8f);
					line.addView(holder.albumDescCount[i]);
				}

				parent.setTag(R.id.data_holder, holder);
			} else {
				holder = (ViewHolder) (parent.getTag(R.id.data_holder));

				for (int i = 0; i < 2; i++) {
					if (holder.task[i] != null) {
						holder.task[i].cancel(true);
						holder.task[i] = null;
					}
					holder.icons[i].setVisibility(View.GONE);
					holder.albumDescBg[i].setVisibility(View.GONE);
					holder.albumDescName[i].setVisibility(View.GONE);
					holder.albumDescCount[i].setVisibility(View.GONE);
				}
			}
		}
		
		private void fillView(int albumIndex, int locate) {

			int x=0, y=0, width=0, height=0, bigImageWh = 0, descriptionWidth=0;
			int cacheId = 0;

			if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				int contentHeight = mConfig.screenHeight - mContentTop;
				bigImageWh = contentHeight/2 - 2*Config.ALBUM_LIST_PADDING;
			    
				x = Config.ALBUM_LIST_PADDING;
				y = Config.ALBUM_LIST_PADDING + locate*contentHeight/2;
				width = bigImageWh + (Config.IMAGE_NUM_PRE_ALBUM - 1)* Config.ALBUM_OVERLIE_IMAGE_WIDTH;
				height = bigImageWh;
				
				cacheId = albumIndex + 50; //Todo
			}
			else {
				int contentWidth = mConfig.screenWidth;
				bigImageWh = contentWidth / 2
						- (Config.IMAGE_NUM_PRE_ALBUM - 1)* Config.ALBUM_OVERLIE_IMAGE_WIDTH
						- 2*Config.ALBUM_LIST_PADDING;

				x = Config.ALBUM_LIST_PADDING + locate * contentWidth / 2;
				y = Config.ALBUM_LIST_PADDING;
				width = contentWidth / 2 - 2 * Config.ALBUM_LIST_PADDING;
				height = bigImageWh;
				
				cacheId = albumIndex;
			}

			holder.icons[locate].setLayoutParams(new AbsoluteLayout.LayoutParams(width,height, x, y));
			holder.icons[locate].setVisibility(View.VISIBLE);
			holder.icons[locate].setId(albumIndex);

			Bitmap bmp = mCacheAndAsyncWork.getBitmapFromRamCache(cacheId);

			if (bmp != null) {
				holder.icons[locate].setImageBitmap(bmp);
			} else {
				ArrayList<Long> album = mAlbumList.get(albumIndex);
				long aId[] = new long[album.size()];
				
				for(int i = 0; i < aId.length; i++) {
					aId[i] = 0;
				}
				for(int i = 0; i < album.size(); i++) {
					aId[i] = album.get(i);
				}
				
				CacheAndAsyncWork.BitmapWorkerTask task = mCacheAndAsyncWork.new BitmapWorkerTask(
						holder.icons[locate], context.getContentResolver(),
						null);
				task.execute((long)cacheId, 0l, aId[0], aId[1], aId[2], aId[3], (long)bigImageWh);
				holder.task[locate] = task;
				holder.icons[locate].setImageBitmap(mDefaultLayerImage);
			}

			if (mConfig.currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				int contentHeight = mConfig.screenHeight - mContentTop;
			    int biggestHeight = contentHeight/2 - 2*Config.ALBUM_LIST_PADDING;
			    
				x = Config.ALBUM_LIST_PADDING;
				y = Config.ALBUM_LIST_PADDING+biggestHeight-Config.ALBUM_DESCRIPTION_HEIGHT + locate*contentHeight/2;
				descriptionWidth = contentHeight/2 -2*Config.ALBUM_LIST_PADDING + 3*Config.ALBUM_OVERLIE_IMAGE_WIDTH;
			}
			else {
				int contentWidth = mConfig.screenWidth;
				int biggestWidth = contentWidth / 2 - (Config.IMAGE_NUM_PRE_ALBUM-1)*Config.ALBUM_OVERLIE_IMAGE_WIDTH - 2*Config.ALBUM_LIST_PADDING;
				
				x = Config.ALBUM_LIST_PADDING + locate*contentWidth/2;
				y = Config.ALBUM_LIST_PADDING+biggestWidth-Config.ALBUM_DESCRIPTION_HEIGHT;
				descriptionWidth = contentWidth/2 -2*Config.ALBUM_LIST_PADDING;
			}
			
			// Album Description
			holder.albumDescBg[locate].setVisibility(View.VISIBLE);
			holder.albumDescBg[locate].setLayoutParams(new AbsoluteLayout.LayoutParams(
					descriptionWidth, Config.ALBUM_DESCRIPTION_HEIGHT,
					x, y));
			holder.albumDescBg[locate].setBackgroundResource(R.drawable.grey);
			
			holder.albumDescName[locate].setVisibility(View.VISIBLE);
			holder.albumDescName[locate].setLayoutParams(new AbsoluteLayout.LayoutParams(
					descriptionWidth, Config.ALBUM_DESCRIPTION_HEIGHT/2,
					x+5, y));
			holder.albumDescName[locate].setText(mAlbumName.get(albumIndex));
			
			holder.albumDescCount[locate].setVisibility(View.VISIBLE);
			holder.albumDescCount[locate].setLayoutParams(new AbsoluteLayout.LayoutParams(
					descriptionWidth, Config.ALBUM_DESCRIPTION_HEIGHT/2,
					x+5, y + Config.ALBUM_DESCRIPTION_HEIGHT/2));
			holder.albumDescCount[locate].setText(mAlbumCount.get(albumIndex).toString());
		}

	    class ViewHolder{
			ImageView icons[] = new GestureImageView[2];
			CacheAndAsyncWork.BitmapWorkerTask task[] = new CacheAndAsyncWork.BitmapWorkerTask[2];
			
			TextView albumDescName[] = new TextView[2];
			TextView albumDescCount[] = new TextView[2];
			ImageView albumDescBg[] = new ImageView[2];
		}
	}
	
    public void entryImageList(View view) {
    	
        final Intent i = new Intent(FolderList.this, ImageList.class);
        
		int clickItemInfo[] = new int[2];
		view.getLocationOnScreen(clickItemInfo);
		
		clickItemInfo[1] -= mContentTop;
		
        i.putExtra(Config.CLICK_ITEM_INFO, clickItemInfo);
        i.putExtra(Config.ALBUM_INDEX, view.getId());
        
        Utils.putPrefAnimFlag(this, true);
        
        startActivity(i);
        
        overridePendingTransition(R.anim.out, R.anim.in);
    }
}
