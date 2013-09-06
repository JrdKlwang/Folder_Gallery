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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
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
    private int mClickIndex;
    private int mClickItemInfo[];
    
    private ImagePagerAdapter mAdapter;
    private ViewPager mPager;

    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_image_detail); // Contains just a ViewPager 

        mImageList = getIntent().getStringArrayListExtra (Config.IMAGE_LIST);
        mClickIndex = getIntent().getIntExtra (Config.CLICK_INDEX, 0);
        mClickItemInfo = getIntent().getIntArrayExtra(Config.CLICK_ITEM_INFO);

        mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), mImageList.size());  
        mPager = (ViewPager) findViewById(R.id.pager);  
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(mClickIndex);
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
    
    public class ImagePagerAdapter extends FragmentStatePagerAdapter {  
        private final int mSize;  
  
        public ImagePagerAdapter(FragmentManager fm, int size) {  
            super(fm);  
            mSize = size;  
        }  
  
        @Override  
        public int getCount() {  
            return mSize;  
        }  
  
        @Override  
        public Fragment getItem(int position) {
        	
        	ImageDetailFragment  detailObj = new ImageDetailFragment();
        	
			if (mClickIndex == position) {
				
				mClickIndex = -1;// Avoid anim every time
				detailObj.init(mImageList.get(position), mClickItemInfo, true);
			} else {
				detailObj.init(mImageList.get(position), null, false);
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
        private ImageView mImageView;
        private View mLayout;

        /**
         * Factory method to generate a new instance of the fragment given an image number.
         *
         * @param imageNum The image redId to load
         * @return A new instance of ImageDetailFragment with imageNum extras
         */
        public void init(String path, int clickItemInfo[], boolean bAnim) {

            final Bundle args = new Bundle();  
            args.putString(Config.IMAGE_PATH, path);
            args.putIntArray(Config.CLICK_ITEM_INFO, clickItemInfo);
            args.putBoolean(Config.ANIM_CONTROL, bAnim);
            setArguments(args);
        }

        /**
         * Empty constructor as per the Fragment documentation
         */
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
    		
    		int outLayout[] = new int[2]; //Width, Height
    		imageLayout(getActivity(), outLayout, bitmapFactoryOptions.outWidth, bitmapFactoryOptions.outHeight);
    		
    		mImageView.setLayoutParams(new FrameLayout.LayoutParams(outLayout[0], outLayout[1], Gravity.CENTER));
    		mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
    		
    		bitmapFactoryOptions.inJustDecodeBounds = false;
    		bitmapFactoryOptions.inSampleSize = bitmapFactoryOptions.outWidth/outLayout[0];
    		image = BitmapFactory.decodeFile(mImagePath, bitmapFactoryOptions);
    		
    		mImageView.setImageBitmap(image);
    		
            if(true == bAnim) {
            	
            	AlphaAnimation alphaAnimation = new AlphaAnimation(0.3f, 1.0f);
            	alphaAnimation.setDuration(400);
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

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mImageView != null) {
                mImageView.setImageDrawable(null);
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
    }
} 
