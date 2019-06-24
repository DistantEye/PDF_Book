/**
 * 
 */
package com.github.distanteye.pdf_book.ui;


import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.PatternSyntaxException;

import javax.imageio.ImageIO;

import com.github.distanteye.pdf_book.ui_helpers.DataManager;
import com.github.distanteye.pdf_book.ui_helpers.HandledUIException;
import com.github.distanteye.pdf_book.ui_helpers.ImageRenderer;
import com.github.distanteye.pdf_book.ui_helpers.MappedByteBufferDataManager;

/**
 * @author Vigilant
 *
 */
public class SimpleMuToolRenderer extends ImageRenderer {

	private DataManager dm;
	
	/**
	 * 
	 */
	public SimpleMuToolRenderer() {
		super();
		dm = new MappedByteBufferDataManager();
	}

	public static boolean IsAvailible()
	{
		File rootDir = new File("./external");
		File[] contents = rootDir.listFiles();
		ArrayList<String> paths = new ArrayList<String>();
		
		for (File f : contents)
		{
			if (f.isFile())
			{
				paths.add(f.getName());
			}
		}
		
		return paths.contains("mutool.exe") || paths.contains("mutool");

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
		
		BufferedImage currentLoadedImage = getCachedImage(t);
		
		// credit to https://stackoverflow.com/a/55629297 for how to get access to null
		File NULL_FILE = new File(
		          (System.getProperty("os.name")
		                     .startsWith("Windows") ? "NUL" : "/dev/null")
		   );
			
		// cache hit lets us skip some of these steps, otherwise we have to do actual rendering below
		if (currentLoadedImage == null)
		{
			
			ProcessBuilder pb = new ProcessBuilder("./external/mutool", "draw", "-F","png", "-o","-", "-r", ""+dpi, t.getFilePath(), ""+page);	
			pb.directory(rootDir);
			
			// Some mangled PDFs can generate a storm of error messages that blocks the buffer (even when we're not reading stderr...)
			// so we route stderr to null for extra safety
			pb.redirectError(NULL_FILE); 
			
			try {
				Process p = pb.start();				
				currentLoadedImage = ImageIO.read(p.getInputStream());
				p.waitFor(1,TimeUnit.SECONDS); // because of the above read this should resolve near immediately, so we have a short timeout
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				throw new HandledUIException(e.getMessage());
			}
		
		}
		
		setPageCount(t, getPageCount(t));
		t.setPreferredSize(new Dimension(currentLoadedImage.getWidth(), currentLoadedImage.getHeight()));
		t.setMaximumSize(new Dimension(currentLoadedImage.getWidth(), currentLoadedImage.getHeight()));
		checkInImage(t, currentLoadedImage);
		
		long endTime = System.nanoTime();
		double duration = (endTime - startTime)/1000000;
		System.out.println("Rendered " + t.getDisplayName() + "(" + page + ")" +  "in " + duration + " milliseconds");
		
		return currentLoadedImage;		
	}

	@Override
	public void closeOutTabInfo(Tab t) {
		super.closeOutTabInfo(t);
		// check back in to close out the memory mapped management
		dm.checkIn(t);
	}

	public int getPageCount(Tab t)
	{
		long startTime = System.nanoTime();
		
		int pageCount = super.getPageCount(t);
		
		if (pageCount != -1)
		{
			return pageCount; 
		}
		
		String filePath = t.getFilePath();
		File rootDir = new File("./external");

		ProcessBuilder pb = new ProcessBuilder("./external/mutool", "show", filePath, "pages");	
		pb.directory(rootDir);
		
		String lastLine = "";
		
		try {
			Process p = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String newLine;
			while((newLine = reader.readLine()) != null) {				
				if (newLine.startsWith("page"))
				{
					lastLine = newLine;
				}
			}
			
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			throw new HandledUIException(e.getMessage());
		}
		
		
		try {
			lastLine = lastLine.replaceFirst("page ", "");
			String outputStr = lastLine.substring(0, lastLine.indexOf(" = "));
					
			pageCount = Integer.parseInt(outputStr.trim());
		}
		catch(PatternSyntaxException | StringIndexOutOfBoundsException | NumberFormatException e )
		{
			throw new HandledUIException("Error getting PageCount: " + e.getMessage());
		}
		
		long endTime = System.nanoTime();
		double duration = (endTime - startTime)/1000000;
		System.out.println("Found maxPages of " + t.getDisplayName() + "(" + pageCount + ")" +  "in " + duration + " milliseconds");
		
		return pageCount;		
	}
	
}
