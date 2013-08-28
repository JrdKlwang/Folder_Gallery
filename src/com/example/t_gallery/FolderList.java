package com.example.t_gallery;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Images.Thumbnails;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FolderList extends ListActivity {
	private Cursor mGalleryList;
	private GalleryLayout mGalleryLayout;
	ArrayList<String> mAlbumName = new ArrayList<String>();
	ArrayList<Integer> mAlbumCount = new ArrayList<Integer>();
	
	private void fetchGalleryList(){
		
		mGalleryList = getContentResolver().query(
				Config.MEDIA_URI, 
				Config.ALBUM_PROJECTION, 
				Config.ALBUM_WHERE_CLAUSE, 
				null, null);
	
		mGalleryLayout = new GalleryLayout(1080);
		
		for (int i=0; i<mGalleryList.getCount(); i++){
			
			mGalleryList.moveToPosition(i);
			
			String album = mGalleryList.getString(mGalleryList.getColumnIndex(Media.BUCKET_DISPLAY_NAME));
			mAlbumName.add(album);
			
			Cursor ImageList = getContentResolver().query(
					Config.MEDIA_URI, 
					Config.IMAGE_PROJECTION, 
					Config.IMAGE_WHERE_CLAUSE, 
					new String[]{mGalleryList.getString(mGalleryList.getColumnIndex(Media.BUCKET_ID))}, 
					null);
			
			mAlbumCount.add(ImageList.getCount());
			ImageList.moveToFirst();
			
			long id = ImageList.getLong(ImageList.getColumnIndex(Media._ID));
			int width = ImageList.getInt(ImageList.getColumnIndex(Media.WIDTH));
			int height = ImageList.getInt(ImageList.getColumnIndex(Media.HEIGHT));
			
			mGalleryLayout.addImage(id, width, height, i);
		}
		mGalleryLayout.addImageFinish();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_list);
        
        fetchGalleryList();
        setListAdapter(new FolderListAdapter(this));
    }
    
	class FolderListAdapter extends BaseAdapter {
		private Context context;

		public FolderListAdapter(Context context) {
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
	            	//Image
	            	holder.icons[i] = new ImageView(context);
	            	holder.icons[i].setBackgroundColor(0xFF000000);
	            	holder.icons[i].setAdjustViewBounds(true);
					holder.icons[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
					holder.icons[i].setPadding(Config.THUMBNAIL_PADDING, Config.THUMBNAIL_PADDING, Config.THUMBNAIL_PADDING, Config.THUMBNAIL_PADDING);
					
	    			line.addView(holder.icons[i]);
	    			
	    			//Description background image
	    			holder.backgrounds[i] = new ImageView(context);
	            	holder.backgrounds[i].setAdjustViewBounds(true);
					holder.backgrounds[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
					holder.backgrounds[i].setAlpha(100);
					holder.backgrounds[i].setPadding(Config.THUMBNAIL_PADDING, Config.THUMBNAIL_PADDING, Config.THUMBNAIL_PADDING, Config.THUMBNAIL_PADDING);
					line.addView(holder.backgrounds[i]);
					
					//Album name
					holder.albumNames[i] = new TextView(context);
					holder.albumNames[i].setTextColor(Color.WHITE);
					holder.albumNames[i].setTextSize(18);
					line.addView(holder.albumNames[i]);
					
					//Album count
					holder.counts[i] = new TextView(context);
					holder.counts[i].setTextColor(Color.WHITE);
					holder.counts[i].setTextSize(15);
					line.addView(holder.counts[i]);
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
			
			//Fill in the content
	        for (int i=0; i< Config.THUMBNAILS_PER_LINE; i++){

	        	if (i >= currentLine.getImageNum()){
	        		holder.icons[i].setVisibility(View.GONE);
	        		holder.backgrounds[i].setVisibility(View.GONE);
	        		holder.albumNames[i].setVisibility(View.GONE);
	        		holder.counts[i].setVisibility(View.GONE);
	        	}
	        	else {
	        		ImageCell image = currentLine.getImage(i);
	      
	        		holder.icons[i].setLayoutParams(new AbsoluteLayout.LayoutParams(image.outWidth+2*Config.THUMBNAIL_PADDING, image.outHeight+2*Config.THUMBNAIL_PADDING, image.outX, image.outY));
	        		holder.icons[i].setVisibility(View.VISIBLE);
	        		
	        	    BitmapWorkerTask task = new BitmapWorkerTask(holder.icons[i]);
					task.execute(image.id);
					holder.task[i] = task;
					holder.icons[i].setScaleType(ImageView.ScaleType.FIT_XY);
					holder.icons[i].setImageResource(R.drawable.grey);
					
					holder.icons[i].setId(image.position);

					holder.icons[i].setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							entryImageList(v);
						}
					});
					
					//Album background image layout params
					holder.backgrounds[i].setVisibility(View.VISIBLE);
	        		holder.backgrounds[i].setLayoutParams(new AbsoluteLayout.LayoutParams(image.outWidth+2*Config.THUMBNAIL_PADDING, 
	        				Config.ALBUM_DESCRIPTION_HEIGHT, 
	        				image.outX, 
	        				image.outY + (image.outHeight+2*Config.THUMBNAIL_PADDING - Config.ALBUM_DESCRIPTION_HEIGHT)));
	        		holder.backgrounds[i].setImageResource(R.drawable.grey);
	        		
	        		//Album name layout params
	        		holder.albumNames[i].setLayoutParams(new AbsoluteLayout.LayoutParams(image.outWidth+2*Config.THUMBNAIL_PADDING, 
	        				Config.DESCRIPTION_TEXT_HEIGHT, 
	        				image.outX + 20, 
	        				image.outY + (image.outHeight+2*Config.THUMBNAIL_PADDING - Config.ALBUM_DESCRIPTION_HEIGHT)));
	        		holder.albumNames[i].setVisibility(View.VISIBLE);
	        		holder.albumNames[i].setText(mAlbumName.get(image.position));
	        		
	        		//Album name layout params
	        		holder.counts[i].setLayoutParams(new AbsoluteLayout.LayoutParams(image.outWidth+2*Config.THUMBNAIL_PADDING, 
	        				Config.DESCRIPTION_TEXT_HEIGHT, 
	        				image.outX + 20, 
	        				image.outY + (image.outHeight+2*Config.THUMBNAIL_PADDING - Config.ALBUM_DESCRIPTION_HEIGHT)+Config.DESCRIPTION_TEXT_HEIGHT));
	        		holder.counts[i].setVisibility(View.VISIBLE);
	        		holder.counts[i].setText(mAlbumCount.get(image.position).toString());
	        	}
			}

			return line;
		}
		
	    private void entryImageList(View view) {
	    	
	        final Intent i = new Intent(FolderList.this, ImageList.class);
	        i.putExtra(Config.ALBUM_INDEX, view.getId());
	        
	        startActivity(i);
	    }
		
	    class ViewHolder{
			ImageView icons[] = new ImageView[Config.THUMBNAILS_PER_LINE];
			BitmapWorkerTask task[] = new BitmapWorkerTask[Config.THUMBNAILS_PER_LINE];
			
			TextView albumNames[] = new TextView[Config.THUMBNAILS_PER_LINE];
			TextView counts[] = new TextView[Config.THUMBNAILS_PER_LINE];
			ImageView backgrounds[] = new ImageView[Config.THUMBNAILS_PER_LINE];
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

				return thumb;
		
			}
			
			public void onPostExecute(Bitmap bitmap){
				
				if (iconReference != null && bitmap != null){
					final ImageView iconView = iconReference.get();
					
					if (iconView != null){
						iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
						iconView.setImageBitmap(bitmap);
					}
				}
			}
		}
	}
}
