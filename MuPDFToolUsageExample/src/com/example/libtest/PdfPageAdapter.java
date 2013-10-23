package com.example.libtest;

import java.io.File;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PdfPageAdapter extends PagerAdapter {

	private Activity activity;
	private String exportedPdfFolderPath;
	
	public PdfPageAdapter(MainActivity activity, String exportedPdfFolderPath) {
		this.activity = activity;
		this.exportedPdfFolderPath = exportedPdfFolderPath;
	}

	@Override
	public int getCount() {
		int count = 0;
		String[] items = new File(exportedPdfFolderPath).list();
		if (items != null) {
			count = items.length;
		}
		return count;
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		FrameLayout frame = new FrameLayout(activity);
		ImageView imageView = new ImageView(activity);
		String imagePath = exportedPdfFolderPath + "/" + position+ ".jpg";
		Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
		if (bitmap == null) {
			Log.e(MainActivity.TAG, "Image not found: "+imagePath);
		}
		imageView.setImageBitmap(bitmap);
		frame.addView(imageView, 0);
		((ViewPager) container).addView(frame, 0);
		return frame;
	}
	
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		View view = (View) object;
		((ViewPager) container).removeView(view);
	}
	
	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}
}