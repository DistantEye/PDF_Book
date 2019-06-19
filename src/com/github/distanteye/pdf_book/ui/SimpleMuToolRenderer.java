/**
 * 
 */
package com.github.distanteye.pdf_book.ui;


import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;

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
		File rootDir = new File(".");
		File[] contents = rootDir.listFiles();
		ArrayList<String> paths = new ArrayList<String>();
		
		for (File f : contents)
		{
			paths.add(f.getPath());
		}
		
		return paths.contains(".\\mutool.exe");

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
		File rootDir = new File(".");
		BufferedImage currentLoadedImage = null;
		
		setPageCount(t);
		
		ProcessBuilder pb = new ProcessBuilder("mutool.exe", "draw", "-ooutput.png", "-r "+dpi, t.getFilePath(), ""+page);	
		pb.directory(rootDir);		
		try {
			Process p = pb.start();
			
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		
		// either we've null returned or the file should exist now?
		
		try {
			File outputFile = new File("output.png");			
			currentLoadedImage = ImageIO.read(outputFile);
			outputFile.delete();
			
		} catch (IOException e) {
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

	// TODO this is honestly a mess of different duct tape, but the end goal would be to replace this with a more direct interaction with MuPDF's libary
	// so as to obtain page counts and render the current page at the same time.
	protected void setPageCount(Tab t)
	{
		if (t.getMaxPages() != 0)
		{
			return; // assume we never need to recalculate pages that were already set, PDFs should be static
		}
		
		String filePath = t.getFilePath();
		File rootDir = new File(".");
		BufferedInputStream output = null;
		
		// this is cheaty but we want things self contained and not packaged alongside mini js files...
		PrintWriter writer;
		try {
			writer = new PrintWriter("pageCount.js");
			writer.println("var doc = new Document(argv[1]);");
			writer.println("var n = doc.countPages();");
			writer.println("print(n);");
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ProcessBuilder pb = new ProcessBuilder("mutool.exe", "run", "pageCount.js", filePath);	
		pb.directory(rootDir);		
		try {
			Process p = pb.start();
			output = new BufferedInputStream(p.getInputStream());
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		File f = new File("pageCount.js");
		f.delete();
		
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
