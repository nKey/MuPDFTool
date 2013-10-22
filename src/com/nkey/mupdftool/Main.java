package com.nkey.mupdftool;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.PointF;

import com.artifex.mupdfdemo.MuPDFCore;

public class Main {
	
	private static final int ZOOM_MIN = 40;
	private static final int ZOOM_MAX = 150;
	private static final int LOG_MAX_LENGTH = 50;
	
	private static MuPDFCore core;

	public static void main(String[] args) {
		log("INIT");
		if (args==null || args.length==0) {
			log("No args! To help run mupdftool -h");
			System.exit(1);
		} else if (args[0].equals("-h")) {
			log("MuPDFTool: generate images from PDF.");
			log("	Usage: mupdftool -lib <libmupdf.so path> -pdf <pdf_path> -outputPath <output_path> (-zoom <zoom_level>| -size <width>x<height>) -pages <page_range>");
			log("		<page_range> must be a valid page range in the <pdf_path> pdf. Accepted single numbers ('1', '38'), ranges ('1-47') and empty, that means all pages.");
			log("		<zoom_level> must be greater than "+ZOOM_MIN+" and less than "+ZOOM_MAX+". You can provide two zoom levels to render the pages. Example: '-zoom 40:100'.");
			log("		-size can be provided with two values to render the pages. Example: '-size 200x400:1000x2000'.");
			System.exit(0);
		} else {
			HashMap<String, String> parameters = new HashMap<String, String>();
			for (int i = 0; i < args.length; i=i+2) {
				if (args.length>=i+1 && args[i].startsWith("-")) {
					parameters.put(args[i], args[i+1]);
				} else {
					log("Error: Invalid args. To help run mupdftool -h");
					System.exit(1);
				}
			}
			
			String mupdfLibPath = parameters.get("-lib");
			if (mupdfLibPath == null) {
				log("Error: The libmupdf.so path is needed to that tool. To help run mupdftool -h");
				System.exit(1);
			} else {
				try {
					log("Loading lib: "+mupdfLibPath+"/libmupdf.so");
					System.load(mupdfLibPath+"/libmupdf.so");
				} catch (Exception e) {
					log("Error loading lib "+mupdfLibPath);
				}
			}
			
			String pdfPath = parameters.get("-pdf");
			if (pdfPath == null) {
				log("Error: A pdf path is needed to that tool. To help run mupdftool -h");
				System.exit(1);
			}
			
			String savePath = parameters.get("-outputPath");
			if (savePath == null) {
				log("Error: A path to save the result is needed.");
				System.exit(1);
			}
			
			String pageRange = parameters.get("-pages");
			int pageStart = -1;
			int pageEnd = -1;
			if (pageRange!=null) {
				String[] pageSplitted = pageRange.split("-");
				if (pageSplitted.length<=0 || pageSplitted.length>=3) {
					log("Error: '"+pageRange+"' is not a valid page range.");
					System.exit(1);
				} else if (pageSplitted.length==1) {
					pageStart = pageEnd = Integer.parseInt(pageSplitted[0]);
				} else {
					pageStart = Integer.parseInt(pageSplitted[0]);
					pageEnd = Integer.parseInt(pageSplitted[1]);
				}
			}
			
			boolean containsZoom = parameters.containsKey("-zoom");
			boolean containsSize = parameters.containsKey("-size");
			if ((containsZoom && containsSize) || (!containsZoom && !containsSize)) {
				log("Error: You must specify one (and only one) of the parameters: '-zoom' or '-size'. To help run mupdftool -h");
				System.exit(1);
			} else {
				if (containsZoom) {
					String zoomString = parameters.get("-zoom");
					String[] zoomSplitted = zoomString.split(":");
					
					int zoom0Level = Integer.parseInt(zoomSplitted[0]);
					Integer zoom1Level = null;
					if (zoomSplitted.length>1) {
						zoom1Level = Integer.parseInt(zoomSplitted[1]);
					}
					if (zoom0Level>ZOOM_MAX || zoom0Level<ZOOM_MIN || (zoom1Level!=null && (zoom1Level>ZOOM_MAX || zoom1Level<ZOOM_MIN))) {
						log("Error: '"+zoomString+"' is not a valid zoom level. Must be greater than "+ZOOM_MIN+" and less than "+ZOOM_MAX);
						System.exit(1);
					} else {
						generatePdfPageRangeWithZoom(pdfPath, savePath, pageStart, pageEnd, zoom0Level, zoom1Level);
					}
				} else {
					String sizesString = parameters.get("-size");
					try{
						String[] sizesSplitted = sizesString.split(":");
						String[] size0Splitted = sizesSplitted[0].split("x");
						PointF size0 = new PointF(Integer.parseInt(size0Splitted[0]), Integer.parseInt(size0Splitted[1]));
						PointF size1 = null;
						if (sizesSplitted.length>1) {
							String[] size1Splitted = sizesSplitted[1].split("x");
							size1 = new PointF(Integer.parseInt(size1Splitted[0]), Integer.parseInt(size1Splitted[1]));
						}
						generatePdfPageRange(pdfPath, savePath, pageStart, pageEnd, size0, size1);
					} catch (Exception e) {
						log("Error: '"+sizesString+"' is not a valid size. Must be <width>x<height> format. Example: 1024x768 ");
						System.exit(1);
					}
				}
			}
		}
		System.exit(0);
	}

	private static void generatePdfPageRangeWithZoom(String pdfPath, String savePath, int pageStart, int pageEnd, int zoom0Level, Integer zoom1Level) {
		loadCorePDF(pdfPath);
		if (pageStart == -1 && pageEnd == -1) {
			pageStart = 0;
			pageEnd = core.countPages()-1;
		}
		int totalPages = pageEnd-pageStart+1;
		if (zoom1Level != null) {
			totalPages *= 2;
		}
		String saveZoom0Path =  savePath+"/"+Integer.toString(zoom0Level);
		for (int page = pageStart; page <= pageEnd; page++) {
			generatePdfPageWithZoom(pdfPath, saveZoom0Path, page, zoom0Level);
			log("Page % completed "+(int)(((double)(page-pageStart+1)/totalPages)*100));
		}
		if (zoom1Level != null) {
			String saveZoom1Path =  savePath+"/"+Integer.toString(zoom1Level);
			int previousLoadedPages = (pageEnd-pageStart+1);
			for (int page = pageStart; page <= pageEnd; page++) {
				generatePdfPageWithZoom(pdfPath, saveZoom1Path, page, zoom1Level);
				log("Page % completed "+(int)(((double)((page-pageStart+1)+previousLoadedPages)/totalPages)*100));
			}
		}
	}

	private static void generatePdfPageRange(String pdfPath, String savePath, int pageStart, int pageEnd, PointF size0, PointF size1) {
		loadCorePDF(pdfPath);
		if (pageStart == -1 && pageEnd == -1) {
			pageStart = 0;
			pageEnd = core.countPages()-1;
		}
		int totalPages = pageEnd-pageStart+1;
		if (size1 != null) {
			totalPages *= 2;
		}
		String saveSize0Path = savePath+"/"+Integer.toString((int)size0.x)+"x"+Integer.toString((int)size0.y);
		for (int page = pageStart; page <= pageEnd; page++) {
			generatePdfPage(pdfPath, saveSize0Path, page, (int)size0.x, (int)size0.y);
			log("Page % completed "+(int)(((double)(page-pageStart+1)/totalPages)*100));
		}
		if (size1 != null) {
			String saveSize1Path = savePath+"/"+Integer.toString((int)size1.x)+"x"+Integer.toString((int)size1.y);
			int previousLoadedPages = (pageEnd-pageStart+1);
			for (int page = pageStart; page <= pageEnd; page++) {
				generatePdfPage(pdfPath, saveSize1Path, page, (int)size1.x, (int)size1.y);
				log("Page % completed "+(int)(((double)((page-pageStart+1)+previousLoadedPages)/totalPages)*100));
			}	
		}
	}

	private static void loadCorePDF(String pdfPath) {
		try {
			core = new MuPDFCore(pdfPath);
			log("	pdf loaded. number of pages: "+core.countPages());
			//core.countPages() call is needed because reasons. without it the core thinks we want the page -2 and crashes.
		} catch (Exception e) {
			log("	Exception when loadingCorePDF");
			e.printStackTrace();
			System.exit(2);
		}
	}
	
	private static void generatePdfPageWithZoom(String pdfPath, String savePath, int pageNumber, int zoomLevel) {
		PointF size = core.getPageSize(pageNumber);
		double zoomRatio = (double) zoomLevel/100;
		int pageWidth = (int)(size.x*zoomRatio);
		int pageHeight = (int)(size.y*zoomRatio);
		generatePdfPage(pdfPath, savePath, pageNumber, pageWidth, pageHeight);
	}
	
	private static void generatePdfPage(String pdfPath, String savePath, int pageNumber, int width, int height) {
		int parentWidth = width;
		int parentHeight = height;
		PointF pageSize = core.getPageSize(pageNumber);
		float mSourceScale = Math.min(parentWidth/pageSize.x, parentHeight/pageSize.y);
		Point newSize = new Point((int)(pageSize.x*mSourceScale), (int)(pageSize.y*mSourceScale));
		
		Bitmap bitmapPage = Bitmap.createBitmap(newSize.x, newSize.y, Config.ARGB_8888);
		core.drawPage(bitmapPage, pageNumber, newSize.x, newSize.y, 0, 0, newSize.x, newSize.y);
		
		String saveFilePath = savePath+"/"+pageNumber+".jpg";
		File outputFile = new File(saveFilePath);
		outputFile.getParentFile().mkdirs();
		try {
			FileOutputStream out = new FileOutputStream(outputFile);
			bitmapPage.compress(Bitmap.CompressFormat.JPEG, 92, out);
			out.close();
			log("	saved at "+saveFilePath);
		} catch (Exception e) {
	    	log("	Exception when generatingPdfPage");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void log(String string) {
		if (string.length()<=LOG_MAX_LENGTH) {
			System.out.println(padString(string, LOG_MAX_LENGTH));
		} else {
			System.out.println(string.substring(0, LOG_MAX_LENGTH));
			log(string.substring(LOG_MAX_LENGTH));
		}
	}
	
	public static String padString(String str, int leng) {
        for (int i = str.length(); i <= leng; i++)
            str += " ";
        return str;
    }
}