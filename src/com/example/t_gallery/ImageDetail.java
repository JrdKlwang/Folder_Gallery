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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Window;

public class ImageDetail extends FragmentActivity {
	public static final String IMAGE_LIST = "image_list";
	public static final String CLICK_INDEX = "click_index";
	public static final String CLICK_ITEM_INFO = "click_item_info";

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

        mImageList = getIntent().getStringArrayListExtra (IMAGE_LIST);
        mClickIndex = getIntent().getIntExtra (CLICK_INDEX, 0);
        mClickItemInfo = getIntent().getIntArrayExtra(CLICK_ITEM_INFO);

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
			if (mClickIndex == position) {
				mClickIndex = -1;// Avoid anim every time
				return ImageDetailFragment.newInstance(
						mImageList.get(position), mClickItemInfo, true);
			} else {
				return ImageDetailFragment.newInstance(
						mImageList.get(position), null, false);
			}
        }  
    }
} 
