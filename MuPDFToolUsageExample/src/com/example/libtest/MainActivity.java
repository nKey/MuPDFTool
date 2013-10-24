package com.example.libtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

	public static final String TAG = "MuPDFToolUsageExample";
	private static final String LIBMUPDF = "libmupdf.so";
	private static final String MUPDFTOOL = "mupdftool.jar";
	private static final String MUPDFTOOL_SH = "run_mupdftool.sh";
	private static String MUPDFTOOL_SH_LOCATION;
	private static final String outputPath = Environment.getExternalStorageDirectory()+"/Download/";
	
	private static int screenWidth;
	private static int screenHeight;
	private static ViewPager viewPager;
	private static PdfPageAdapter viewPagerAdapter;
	private static ProgressBar progressBar;
	private static long beginTime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		storeScreenSize();
		beginTime = System.currentTimeMillis();
		prepareLibFiles();
		setupSubviews();
		askUserForPdfPath();
	}

	private void askUserForPdfPath() {
		final EditText input = new EditText(this);
		new AlertDialog.Builder(this)
	    .setTitle("Import PDF")
	    .setMessage("Type the path to the PDF you want to import")
	    .setView(input)
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	    		executeBashCommand("/system/bin/sh " + MUPDFTOOL_SH_LOCATION + " " + getCacheDir().getAbsolutePath() + " -pdf "+input.getText().toString() + " -outputPath " + outputPath + " -size " + screenWidth + "x" + screenHeight);
	    		//other call examples: (obs: you will need to change PdfPageAdapter creation arguments to match the outputPath folder)
//	    		executeBashCommand("/system/bin/sh " + MUPDFTOOL_SH_LOCATION + " " + getCacheDir().getAbsolutePath() + " -pdf "+input.getText().toString() + " -outputPath " + outputPath + " -pages 0-29 -size "+screenWidth+"x"+screenHeight);
//	    		executeBashCommand("/system/bin/sh " + MUPDFTOOL_SH_LOCATION + " " + getCacheDir().getAbsolutePath() + " -pdf "+input.getText().toString() + " -outputPath " + outputPath + " -pages 10 -size "+screenWidth/3+"x"+screenHeight/3+":"+screenWidth+"x"+screenHeight);
//	    		executeBashCommand("/system/bin/sh " + MUPDFTOOL_SH_LOCATION + " " + getCacheDir().getAbsolutePath() + " -pdf "+input.getText().toString() + " -outputPath " + outputPath + " -pages 0-9 -zoom 40:100");
//	    		executeBashCommand("/system/bin/sh " + MUPDFTOOL_SH_LOCATION + " " + getCacheDir().getAbsolutePath() + " -pdf "+input.getText().toString() + " -outputPath " + outputPath + " -pages 0-4 -zoom 100");
	        }
	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            // Do nothing.
	        }
	    }).show();
	}
	
	private void setupSubviews() {
		viewPager = (ViewPager) findViewById(R.id.viewpager);
		viewPagerAdapter = new PdfPageAdapter(this, outputPath+Integer.toString(screenWidth)+"x"+Integer.toString(screenHeight));
		viewPager.setAdapter(viewPagerAdapter);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		progressBar.setProgress(0);
	}

	private void prepareLibFiles() {
		copyAssetToFilesDir(LIBMUPDF);
		copyAssetToFilesDir(MUPDFTOOL);
		MUPDFTOOL_SH_LOCATION = copyAssetToFilesDir(MUPDFTOOL_SH);
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	private void storeScreenSize() {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			display.getSize(size);
		} else {
			size.x = display.getWidth();
			size.y = display.getHeight();
		}
		screenWidth = size.x;
		screenHeight = size.y;
	}

	private String copyAssetToFilesDir(String asset) {
		String cachedAssetFilePath = getCacheDir().getAbsolutePath()+"/"+asset;
		Log.d(TAG, "copyAssetToFilesDir "+cachedAssetFilePath);
		File existingAsset = new File(cachedAssetFilePath);
		AssetFileDescriptor newAssetDescriptor = null;
		try {
			newAssetDescriptor = getAssets().openFd(asset);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if (!existingAsset.exists()||newAssetDescriptor==null||newAssetDescriptor.getLength()!=existingAsset.length()) {
			try {
				InputStream in = getAssets().open(asset);
				FileOutputStream out = new FileOutputStream(existingAsset);//
				byte[] buffer = new byte[1024];
				int read = 0;
				try {
					while ((read = in.read(buffer)) > 0) {
						out.write(buffer, 0, read);
					}
				} finally {
					in.close();
					out.close();
					Log.d(TAG, "copy ended! "+cachedAssetFilePath);
				}
				String myExec = "/system/bin/chmod +x "+cachedAssetFilePath;
				Runtime.getRuntime().exec(myExec);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			Log.d(TAG, "file already exists "+cachedAssetFilePath);
		}
		return cachedAssetFilePath;
	}

	private void executeBashCommand(String myExec) {
		new AsyncTask<Object, String, Void>() {
			ViewPager viewPager;
			ProgressBar progressBar;
			@Override
			protected Void doInBackground(Object... params) {
				String myExec = (String) params[0];
				viewPager = (ViewPager) params[1];
				progressBar = (ProgressBar) params[2];
				try {
					Log.d(TAG, "executeBashFile "+myExec);
					Process process = Runtime.getRuntime().exec(myExec);
					BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					StringBuffer output = new StringBuffer();
					char[] buffer = new char[6];
				    int read;
					while ((read = reader.read(buffer)) > 0) {
				        output.append(buffer, 0, read);

				        StringBuffer lastAppendBuffer = new StringBuffer();
				        lastAppendBuffer.append(buffer,0,buffer.length);
				        
				        String lastAppend = lastAppendBuffer.toString().trim();
				        if (lastAppend.length() > 3 && lastAppend.substring(0, 2).equals("%%")) {
				        	publishProgress(lastAppend.substring(3));
				        }
				    }
				    reader.close();
				    for( String line : output.toString().split("\n") ) {
				        Log.d( TAG, line );
				    }
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
			
			protected void onProgressUpdate(String... progress) {
				Log.d(TAG, "PROGRESS UPDATE '"+progress[0]+"'");
				progressBar.setProgress(Integer.parseInt(progress[0]));
			};
			
			protected void onPostExecute(Void result) {
				viewPagerAdapter.notifyDataSetChanged();
				viewPager.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
				Log.e(TAG, "Total time spent: "+(System.currentTimeMillis()-beginTime)/1000+" seconds");
			};
		}.execute(myExec, viewPager, progressBar);
	}
}