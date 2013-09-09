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

import com.example.t_gallery.CacheAndAsyncWork.BitmapWorkerTask;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.provider.MediaStore.Images.Thumbnails;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class ImageDetail extends FragmentActivity {
	private ArrayList<String> mImageList;
	private long mThumbnailId;
    private int mClickIndex;
    private int mClickItemInfo[];
    
    private ImagePagerAdapter mAdapter;
    private ViewPager mPager;
    private CacheAndAsyncWork mCacheAndAsyncWork;
    
    private HashMap<Integer , Bitmap> mFlipmap;
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
        
        mCacheAndAsyncWork = new CacheAndAsyncWork();
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
	
    public void imageLayout(Context context, int outLayout[], int iWidth, int iHeight) {
    	float layoutWidth = 0.0f, layoutHeight = 0.0f;
    	
    	Point outPoint = new Point();
    	WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
    	
    	wm.getDefaultDisplay().getSize(outPoint);
    	
    	int screenWidth = outPoint.x;
    	int screenHeight = outPoint.y;
    	
		float yRatio = (float)iHeight / (float)iWidth;
		
		if (yRatio > 1.0f) {
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
	
    /**
     * load the position images 
     * @param position
     */
    private void preLoadPhoto(final Context context) {
    	
    	final int position = mPager.getCurrentItem();
    	
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
        
    	Log.v("t-gallery", "pre load position: " + position);
    	
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
							imageLayout(context, outLayout,
									bitmapFactoryOptions.outWidth,
									bitmapFactoryOptions.outHeight);

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
        	
        	preLoadPhoto(context);
        	
        	ImageDetailFragment  detailObj = new ImageDetailFragment(context);
        	
			if (mClickIndex == position) {
				
				mClickIndex = -1;// Avoid anim every time
				detailObj.init(mImageList.get(position), mClickItemInfo, true, position);
			} else {
				detailObj.init(mImageList.get(position), null, false, position);
			}
			
			return detailObj;
        }  
    }
    
    /**
     * This fragment will populate the children of the ViewPager from {@link ImageDetailActivity}.
     */
    public class ImageDetailFragment extends Fragment {
        private String mImagePath;
        private int mClickItemInfo[]; //x,y,width, height
        private boolean bAnim;
        private int position;
        private ImageView mImageView;
        private View mLayout;
        private Context context;

        /**
         * Factory method to generate a new instance of the fragment given an image number.
         *
         * @param imageNum The image redId to load
         * @return A new instance of ImageDetailFragment with imageNum extras
         */
        public void init(String path, int clickItemInfo[], boolean bAnim, int position) {

            final Bundle args = new Bundle();  
            args.putString(Config.IMAGE_PATH, path);
            args.putIntArray(Config.CLICK_ITEM_INFO, clickItemInfo);
            args.putBoolean(Config.ANIM_CONTROL, bAnim);
            args.putInt(Config.CLICK_INDEX, position);
            setArguments(args);
        }

        /**
         * Empty constructor as per the Fragment documentation
         */
        public ImageDetailFragment(Context context) {
        	this.context = context;
        }
        
        public ImageDetailFragment() {}

        /**
         * Populate image using a url from extras, use the convenience factory method
         * {@link ImageDetailFragment#newInstance(String)} to create this fragment.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
              
            mImagePath = getArguments() != null ? getArguments().getString (Config.IMAGE_PATH, null) : null;
            mClickItemInfo = getArguments() != null ? getArguments().getIntArray (Config.CLICK_ITEM_INFO) : null;
            bAnim = getArguments() != null ? getArguments().getBoolean (Config.ANIM_CONTROL) : null;
            position = getArguments() != null ? getArguments().getInt (Config.CLICK_INDEX) : -1;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	
        	mLayout = inflater.inflate(R.layout.image_detail_fragment, container, false);
            mImageView = (ImageView) mLayout.findViewById(R.id.imageView);
            return mLayout;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
       
            
			Bitmap image = null;
			BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();

			bitmapFactoryOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(mImagePath, bitmapFactoryOptions);

			// Image layout
			int outLayout[] = new int[2]; // Width, Height
			imageLayout(getActivity(), outLayout,
					bitmapFactoryOptions.outWidth,
					bitmapFactoryOptions.outHeight);

			mImageView.setLayoutParams(new FrameLayout.LayoutParams(
					outLayout[0], outLayout[1], Gravity.CENTER));
			mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            
			// Image anim
			if (true == bAnim) {
				imageAnim(outLayout);
			}
			
			// Image resource
            if (mFlipmap.get(position) != null) {
            	Log.v("t-gallery", "exist: " + position);
            	
            	mImageView.setImageBitmap(mFlipmap.get(position));
            }
            else {
            	
            	Log.v("t-gallery", "not exist: " + position);

				int scaleSize = bitmapFactoryOptions.outWidth / outLayout[0];

				if (scaleSize > 1) {
					CacheAndAsyncWork.BitmapPathWorkerTask task = mCacheAndAsyncWork.new BitmapPathWorkerTask(
							mImageView, scaleSize);
					task.execute(mImagePath);

					if (true == bAnim) {
						Bitmap thumb = mCacheAndAsyncWork.getBitmapFromRamCache(mThumbnailId);

						mImageView.setImageBitmap(thumb);
					}
					else {
						mImageView.setImageResource(R.drawable.grey);
					}
				} else {
					bitmapFactoryOptions.inJustDecodeBounds = false;
					bitmapFactoryOptions.inSampleSize = scaleSize;
					image = BitmapFactory.decodeFile(mImagePath, bitmapFactoryOptions);

					mImageView.setImageBitmap(image);
				}
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mImageView != null) {
                mImageView.setImageDrawable(null);
            }
        }
        
        private void imageAnim(int outLayout[]) {
        	AlphaAnimation alphaAnimation = new AlphaAnimation(0.3f, 1.0f);
        	alphaAnimation.setDuration(300);
        	mLayout.startAnimation(alphaAnimation);
        		
        	Point outPoint = new Point();
        	getActivity().getWindowManager().getDefaultDisplay().getSize(outPoint);
        	
            //Translate anim 
        	int imageX = (outPoint.x - mClickItemInfo[2])/2;
        	int imageY = (outPoint.y - mClickItemInfo[3])/2; 
        	
        	float fromXDelta = (float)mClickItemInfo[0] - (float)imageX;
        	float fromYDelta = (float)mClickItemInfo[1] - (float)imageY;
			TranslateAnimation translateAnimation = new TranslateAnimation(fromXDelta, 0, fromYDelta, 0);
			translateAnimation.setDuration(300);
              
            //Sacle anim
        	float toX = (float)outLayout[0]/(float)mClickItemInfo[2];
        	float toY = (float)outLayout[1]/(float)mClickItemInfo[3];
			ScaleAnimation scaleAnimation = new ScaleAnimation(1 / toX, 1,
					1 / toY, 1, Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			scaleAnimation.setDuration(300);
            
            //Animation set  
            AnimationSet set = new AnimationSet(true);
            set.addAnimation(translateAnimation); 
            set.addAnimation(scaleAnimation); 
              
            mImageView.startAnimation(set);
        }
    }
} 
