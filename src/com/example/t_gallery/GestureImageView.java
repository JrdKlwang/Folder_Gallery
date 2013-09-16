package com.example.t_gallery;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;


public class GestureImageView extends ImageView{
	private GestureDetector mGesture;
	private View mView;
	private Context mContext;
	private int mType = -1; //1: Folder list, 2: Image list
	
	GestureImageView(Context context, int type) {
		super(context);
		
		mContext = context;
		mType = type;
		mGesture = new GestureDetector(context, mOnGesture);
		setOnTouchListener(new OnTouch());
	}
	
	class OnTouch implements OnTouchListener {

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			mView = v;
			mGesture.onTouchEvent(event);
			return true;
		}
	}
	
	private OnGestureListener mOnGesture = new GestureDetector.SimpleOnGestureListener() {

		@Override
	    public boolean onSingleTapUp(MotionEvent e) {
			if (mType == 1) {
				
				FolderList list = (FolderList) mContext;
				list.entryImageList(mView);
			} else if (mType == 2) {
				
				ImageList list = (ImageList) mContext;
				list.entryImageDetail(mView);
			}
	        return true;   
	    } 
	};
}