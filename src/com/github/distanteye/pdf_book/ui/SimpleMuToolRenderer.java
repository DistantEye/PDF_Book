/**
 * 
 */
package com.github.distanteye.pdf_book.ui;


import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.github.distanteye.pdf_book.ui_helpers.DataManager;
import com.github.distanteye.pdf_book.ui_helpers.ImageRenderer;
import com.github.distanteye.pdf_book.ui_helpers.MappedByteBufferDataManager;

/**
 * @author Vigilant
 *
 */
public class SimpleMuToolRenderer implements ImageRenderer {

	private DataManager dm;
	
	/**
	 * 
	 */
	public SimpleMuToolRenderer() {
		dm = new MappedByteBufferDataManager();
	}

	public static boolean IsAvailible()
	{
		File rootDir = new File("./external");
		File[] contents = rootDir.listFiles();
		ArrayList<String> paths = new ArrayList<String>();
		
		for (File f : contents)
		{
			paths.add(f.getName());
		}
		
		return (paths.contains("mutool.exe") || paths.contains("mutool")) && paths.contains("pageCount.js");

	}
	
	/* (non-Javadoc)
	 * @see com.github.distanteye.pdf_book.ui.ImageRenderer#renderPDF(java.lang.String, int, int)
	 */
	@Override
	public BufferedImage renderPDF(Tab t, int page, int dpi) {
		// we don't actually need the contents in here, we just want to make sure the file gets memory mapped for later access
		try {
			dm.checkOut(t);
		} catch (IOException e1) {			
			e1.printStackTrace();
			return null;
		}
		
		long startTime = System.nanoTime();
		File rootDir = new File("./external");

		BufferedImage currentLoadedImage = null;
		
		setPageCount(t);
		
		ProcessBuilder pb = new ProcessBuilder("./external/mutool", "draw", "-F","png", "-o","-", "-r", ""+dpi, t.getFilePath(), ""+page);	
		pb.directory(rootDir);

		try {
			Process p = pb.start();
			currentLoadedImage = ImageIO.read(p.getInputStream());
			p.waitFor(); // because of the above read this should resolve near immediately
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		
		t.setPreferredSize(new Dimension(currentLoadedImage.getWidth(), currentLoadedImage.getHeight()));
		t.setMaximumSize(new Dimension(currentLoadedImage.getWidth(), currentLoadedImage.getHeight()));
		
		long endTime = System.nanoTime();
		double duration = (endTime - startTime)/1000000;
		System.out.println("Rendered " + t.getDisplayName() + "(" + page + ")" +  "in " + duration + " milliseconds");
		
		return currentLoadedImage;		
	}

	@Override
	public void closeOutTabInfo(Tab t) {
		// check back in to close out the memory mapped management
		dm.checkIn(t);
	}

	protected void setPageCount(Tab t)
	{
		if (t.getMaxPages() != 0)
		{
			return; // assume we never need to recalculate pages that were already set, PDFs should be static
		}
		
		String filePath = t.getFilePath();
		File rootDir = new File("./external");
		BufferedInputStream output = null;
		
		ProcessBuilder pb = new ProcessBuilder("./external/mutool", "run", "pageCount.js", filePath);	
		pb.directory(rootDir);		
		try {
			Process p = pb.start();
			output = new BufferedInputStream(p.getInputStream());
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		String outputStr = "";
		try {
			int length = output.available();
			byte[] parsedOutput = new byte[length];
			output.read(parsedOutput);
			output.close();
			
			outputStr = new String(parsedOutput, "US-ASCII");
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
			
		t.setMaxPages(Integer.parseInt(outputStr.trim()));
		
	}
}
