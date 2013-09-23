/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.t_gallery;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;

public class ImageDetail extends FragmentActivity {
	private ArrayList<String> mImageList;
	private long mThumbnailId;
    private int mClickIndex;
    private int mClickItemInfo[];
    
    private ImagePagerAdapter mAdapter;
    private ViewPager mPager;
    
    private static HashMap<Integer , Bitmap> mFlipmap;
    public static final int OFFSET = 3;
    public static final int PRELOADSIZE = OFFSET*2+1;
    private int mOldIndex = -1;

    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_image_detail); // Contains just a ViewPager 

        mImageList = getIntent().getStringArrayListExtra (Config.IMAGE_LIST);
        mClickIndex = getIntent().getIntExtra (Config.CLICK_INDEX, 0);
        mClickItemInfo = getIntent().getIntArrayExtra(Config.CLICK_ITEM_INFO);
        mThumbnailId = getIntent().getLongExtra(Config.THUMBNAIL_ID, 0);

        mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), this);  
        mPager = (ViewPager) findViewById(R.id.pager);  
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(mClickIndex);
        
        mFlipmap = new HashMap<Integer , Bitmap>();
    }  

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent data = getIntent();
			
			data.putExtra(Config.DISPLAY_ITEM_INDEX, mPager.getCurrentItem());
			setResult(Config.REQUEST_CODE, data);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
        if (this.mFlipmap != null) {
            this.mFlipmap.clear();
        }
	}

	public static HashMap<Integer , Bitmap> getFlipMap() {
		return mFlipmap;
	}
	
    private void loadImageThread(final int position) {
    	
        Log.v("t-gallery", "pre load position: " + position);
    	
    	final Context context = this;
    	
        if (position >= 0 && position < mImageList.size()) {
            new Thread() {
                @Override
                public void run() {
                	int begin = position - OFFSET;
                	if(begin < 0) {
                		begin = 0;
                	}
                	
                	int end = position + OFFSET;
                	if(end > (mImageList.size() -1)) {
                		end = mImageList.size() -1;
                	}
                
					for (int i = begin; i <= end; i++) {
						if (mFlipmap.get(i) == null) {
							String path = mImageList.get(i);

							Bitmap bitmap = null;
							BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();

							bitmapFactoryOptions.inJustDecodeBounds = true;
							BitmapFactory.decodeFile(path, bitmapFactoryOptions);

							// Image layout
							int outLayout[] = new int[2]; // Width, Height
							ImageDetailFragment.imageLayout(context, outLayout,
									bitmapFactoryOptions.outWidth, bitmapFactoryOptions.outHeight);

							bitmapFactoryOptions.inJustDecodeBounds = false;
							bitmapFactoryOptions.inSampleSize = bitmapFactoryOptions.outWidth / outLayout[0];
							bitmap = BitmapFactory.decodeFile(path, bitmapFactoryOptions);

							mFlipmap.put(i, bitmap);
						}
					}
                }
            }.start();
        }
    }
    
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
        	
        	int position = msg.arg1;
        	
        	loadImageThread(position);
        };
    };
    
    private void preLoadImage(int delay) {
    	
    	int position = mPager.getCurrentItem();
    	
    	if(position > mOldIndex) {
    		
    		int reclyceIndex = position - (OFFSET + 1);
    		
    		if(reclyceIndex >= 0) {
				if (mFlipmap.get(reclyceIndex) != null) {
					mFlipmap.get(reclyceIndex).recycle();
					mFlipmap.put(reclyceIndex, null);
					mFlipmap.remove(reclyceIndex);
				}
    		}
    	}
    	else if(position < mOldIndex){
    		int reclyceIndex = position + (OFFSET + 1);
    		
    		if(reclyceIndex <= (mImageList.size() -1)) {
				if (mFlipmap.get(reclyceIndex) != null) {
					mFlipmap.get(reclyceIndex).recycle();
					mFlipmap.put(reclyceIndex, null);
					mFlipmap.remove(reclyceIndex);
				}
    		}
    	}
    	else {
    		return;
    	}
    	
        mOldIndex = position;
        
        mHandler.removeMessages(1);
        Message obtainMessage = mHandler.obtainMessage();
        obtainMessage.what = 1;
        obtainMessage.arg1 = position;
        mHandler.sendMessageDelayed(obtainMessage, delay);
    }
    
    public class ImagePagerAdapter extends FragmentStatePagerAdapter {  
        private Context context;
  
        public ImagePagerAdapter(FragmentManager fm, Context context) {  
            super(fm);  
            this.context = context;
        }  
  
        @Override  
        public int getCount() {  
            return mImageList.size();  
        }  

        @Override  
        public Fragment getItem(int position) {
        	
        	ImageDetailFragment  detailObj = new ImageDetailFragment();
        	
			if (mClickIndex == position) {
				
	        	preLoadImage(400);
				mClickIndex = -1;// Avoid anim every time
				detailObj.init(mImageList.get(position), mClickItemInfo, true, position, mThumbnailId);
			} else {
				
	        	preLoadImage(0);
				detailObj.init(mImageList.get(position), null, false, position, mThumbnailId);
			}
			
			return detailObj;
        }  
    }
} 
