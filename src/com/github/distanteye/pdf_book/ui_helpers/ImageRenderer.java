/**
 * 
 */
package com.github.distanteye.pdf_book.ui_helpers;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import com.github.distanteye.pdf_book.ui.Tab;

/**
 * Skeletal ancestor for all ImageRenders : an Image render should be able to provide an image on demand for a particular tab,
 * and handle requests/updates when the Tab ceases to exist or otherwise changes state (in order to optionally provide better caching behavior for renders)
 * @author Vigilant
 *
 */
public abstract class ImageRenderer {
	
	protected HashMap<ImageKey,BufferedImage> storedImages;
	protected HashMap<String,Integer> storedPageCount;
	protected HashMap<Tab,ImageKey> tabMappings; // every time a render is called  this should be checked for changes and udpated;
	
	
	public ImageRenderer() {
		storedImages = new HashMap<ImageKey,BufferedImage>();
		storedPageCount = new HashMap<String,Integer>();
		tabMappings = new HashMap<Tab,ImageKey>();
	}
	
	
	/**
	 * Renders a PDF page using the implementing renderer. This should make use of storedImages whenever possible but this is not enforced, 
	 * implementing class is responsible
	 * 
	 * @param tab The tab tied to the request (render will modify settings and provide information to this tab as output)
	 * @param page The page number to render (note the interface assumption is that the first page of the pdf is page=1. 
	 * 			   Implementing classes that use 0 alignment should still take page=1 and convert internally
	 * @param dpi The dpi to render at
	 * @return If successful, a BufferedImage, if any errors occur : null
	 */
	public abstract BufferedImage renderPDF(Tab t, int page, int dpi);
	
	public BufferedImage getCachedImage(Tab t)
	{
		ImageKey key = new ImageKey(t);
		return storedImages.get(key);
	}
	
	protected void checkInImage(Tab t, BufferedImage img) 
	{
		ImageKey key = new ImageKey(t);
		
		tabMappings.put(t, key);
		storedImages.put(key, img);
		
	}
	
	public void closeOutTabInfo(Tab t) 
	{		
		clearOutCurrentTabPage(t);	
	}
	
	public void clearOutCurrentTabPage(Tab t)
	{
		ImageKey key = tabMappings.remove(t);
		
		if (key == null)
		{
			return; // no entries were found, nothing to do : this is common the first time a Tab tries to render
		}
		
		if (!tabMappings.containsValue(key))
		{
			storedImages.remove(key);
			storedPageCount.remove(key.filePath);
		}		
	}
	
	public int getPageCount(Tab t)
	{
		if (t.getMaxPages() != 0)
		{			
			int result = t.getMaxPages();
			storedPageCount.put(t.getFilePath(), result);
			return result; 
		}
		else if (storedPageCount.containsKey(t.getFilePath()))
		{
			return storedPageCount.get(t.getFilePath());
		}
		
		return -1; // not found
	}
	
	public void setPageCount(Tab t, int pageCount)
	{
		if (pageCount == -1)
		{
			throw new IllegalArgumentException("Attempted to set invalid pagecount");
		}
		
		t.setMaxPages(pageCount);
		storedPageCount.put(t.getFilePath(), pageCount);
	}
	
	protected class ImageKey {
		private String filePath;
		private int pageNum;
		private int dpi;
		private int hashCode;
		
		public ImageKey(String filePath, int pageNum, int dpi) {
			this.filePath = filePath;
			this.pageNum = pageNum;
			this.dpi = dpi;
			
			// hashcode hardcoding for faster lookup later
			String a = ""+filePath.hashCode();
			String b = ""+pageNum;
			String c = ""+dpi;
			String bc = b+"0"+c;
			
			String abc = a+"0"+bc;
			hashCode = abc.hashCode();
		}
		
		public ImageKey(Tab t)
		{
			this(t.getFilePath(), t.getCurrentPage(), t.getDpi());
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof ImageKey)) { return false; }
			
			ImageKey other = (ImageKey)o;
			
			return this.filePath.equals(other.filePath) && this.pageNum == other.pageNum && this.dpi == other.dpi;
		}
		
		@Override
		public int hashCode()
		{
			return hashCode;
		}
	}
}
