package com.example.t_gallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * This fragment will populate the children of the ViewPager from
 * {@link ImageDetailActivity}.
 */
public class ImageDetailFragment extends Fragment {
	private String mImagePath;
	private int mClickItemInfo[]; // x,y,width, height
	private boolean bAnim;
	private int position;
	private ImageView mImageView;
	private View mLayout;
	private Long mThumbnailId;
	private CacheAndAsyncWork mCacheAndAsyncWork;

	/**
	 * Factory method to generate a new instance of the fragment given an image
	 * number.
	 * 
	 * @param imageNum
	 *            The image redId to load
	 * @return A new instance of ImageDetailFragment with imageNum extras
	 */
	public void init(String path, int clickItemInfo[], boolean bAnim,
			int position, Long thumbnailId) {

		final Bundle args = new Bundle();
		args.putString(Config.IMAGE_PATH, path);
		args.putIntArray(Config.CLICK_ITEM_INFO, clickItemInfo);
		args.putBoolean(Config.ANIM_CONTROL, bAnim);
		args.putInt(Config.CLICK_INDEX, position);
		args.putLong(Config.THUMBNAIL_ID, thumbnailId);
		setArguments(args);
	}

	/**
	 * Populate image using a url from extras, use the convenience factory
	 * method {@link ImageDetailFragment#newInstance(String)} to create this
	 * fragment.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mImagePath = getArguments() != null ? getArguments().getString(Config.IMAGE_PATH, null) : null;
		mClickItemInfo = getArguments() != null ? getArguments().getIntArray(Config.CLICK_ITEM_INFO) : null;
		bAnim = getArguments() != null ? getArguments().getBoolean(Config.ANIM_CONTROL) : null;
		position = getArguments() != null ? getArguments().getInt(Config.CLICK_INDEX) : -1;
		mThumbnailId = getArguments() != null ? getArguments().getLong(Config.THUMBNAIL_ID) : -1;
		
		mCacheAndAsyncWork = new CacheAndAsyncWork();
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
		Utils.imageLayout(getActivity(), outLayout, bitmapFactoryOptions.outWidth,
				bitmapFactoryOptions.outHeight);

		mImageView.setLayoutParams(new FrameLayout.LayoutParams(outLayout[0],
				outLayout[1], Gravity.CENTER));
		mImageView.setScaleType(ImageView.ScaleType.FIT_XY);

		// Image anim
		if (true == bAnim) {
			imageAnim(outLayout);
		}

		// Image resource
		if (ImageDetail.getFlipMap().get(position) != null) {
			Log.v("t-gallery", "exist: " + position);

			mImageView.setImageBitmap(ImageDetail.getFlipMap().get(position));
		} else {

			Log.v("t-gallery", "not exist: " + position);

			int scaleSize = bitmapFactoryOptions.outWidth / outLayout[0];

			if (scaleSize > 1) {
				CacheAndAsyncWork.BitmapPathWorkerTask task = mCacheAndAsyncWork.new BitmapPathWorkerTask(
						mImageView, scaleSize);
				task.execute(mImagePath);

				if (true == bAnim) {
					Bitmap thumb = mCacheAndAsyncWork.getBitmapFromRamCache(mThumbnailId);

					mImageView.setImageBitmap(thumb);
				} else {
					mImageView.setImageResource(R.drawable.grey);
				}
			} else {
				bitmapFactoryOptions.inJustDecodeBounds = false;
				bitmapFactoryOptions.inSampleSize = scaleSize;
				image = BitmapFactory.decodeFile(mImagePath,
						bitmapFactoryOptions);

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
		Point outPoint = new Point();
		getActivity().getWindowManager().getDefaultDisplay().getSize(outPoint);

		// Translate anim
		int imageX = (outPoint.x - mClickItemInfo[2]) / 2;
		int imageY = (outPoint.y - mClickItemInfo[3]) / 2;

		float fromXDelta = (float) mClickItemInfo[0] - (float) imageX;
		float fromYDelta = (float) mClickItemInfo[1] - (float) imageY;
		
		float toX = (float) outLayout[0] / (float) mClickItemInfo[2];
		float toY = (float) outLayout[1] / (float) mClickItemInfo[3];
		
		TranslateAnimation translateAnimation = new TranslateAnimation(
				fromXDelta*toX, 0, fromYDelta*toY, 0);
		translateAnimation.setDuration(300);

		// Sacle anim
		ScaleAnimation scaleAnimation = new ScaleAnimation(1 / toX, 1, 1 / toY,
				1, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		scaleAnimation.setDuration(300);

		// Animation set
		AnimationSet set = new AnimationSet(true);
		set.addAnimation(translateAnimation);
		set.addAnimation(scaleAnimation);

		mImageView.startAnimation(set);
	}
}