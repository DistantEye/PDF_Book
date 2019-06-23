package com.github.distanteye.pdf_book.ui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JPanel;

import com.github.distanteye.pdf_book.ui_helpers.ImageRenderer;




/**
 * Main model representing the UI data for each Tab open in the system, as well as managing the retrieval and display of the current Tab's open PDF page
 * 
 * @author Vigilant
 *
 */
public class Tab extends JPanel {


	private static final long serialVersionUID = -1549012741303631233L;
	
	private Sheet sheet;
	
	private ImageRenderer renderer;
	
	private String displayName;
	private int scrollPositionX;
	private int scrollPositionY;
	
	private BufferedImage currentLoadedImage;
	
	/**
	 * Constructs and initializes a UI tab, handling all the load/page rendering for the relevant filepath/page 
	 * @param displayName What text the navigation bar's tab should display for this Tab
	 * @param filePath Valid file path to the PDF the sheet is associated to
	 * @param currentPage What page on the linked PDF should be open
	 * @param dpi The dpi to load the pdf into an image from
	 * @param dm A valid DataManager instance for managing the file IO 
	 * @throws IOException 
	 * @throws InvalidPasswordException 
	 */
	public Tab(String displayName, String filePath, int currentPage, int dpi, ImageRenderer renderer) throws IOException {
		super();
		this.sheet = new Sheet(filePath, currentPage, dpi);
		this.displayName = displayName;
		this.scrollPositionX = 0;
		this.scrollPositionY = 0;
		this.renderer = renderer;
				
		recalculatePage();
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;	
	}

	public int getCurrentPage() {
		return sheet.getCurrentPage();
	}
	
	public int getMaxPages() {
		return sheet.getMaxPages();
	}
	
	public void setMaxPages(int pages) {
		sheet.setMaxPages(pages);
	}

	public void setCurrentPage(int currentPage) {
		int oldPage = sheet.getCurrentPage();
		
		// don't recalculate unnecessarily
		if (oldPage != currentPage && currentLoadedImage != null)
		{
			sheet.setCurrentPage(currentPage);
			recalculatePage();
		}
	}

	public String getFilePath() {
		return sheet.getFilePath();
	}
	
	public int getDpi() {
		return sheet.getDpi();
	}

	public void setDpi(int dpi) {
		int oldDpi = sheet.getDpi();
		
		// don't recalculate unnecessarily
		if (oldDpi != dpi && currentLoadedImage != null)
		{
			sheet.setDpi(dpi);
			recalculatePage();
		}
	}
	
	protected void recalculatePage() 
	{
		currentLoadedImage = renderer.renderPDF(this, getCurrentPage(), getDpi());
		
		repaint();
	}
	
	public void onLeaving()
	{
		renderer.closeOutTabInfo(this);
		System.gc();
	}
	
	@Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(currentLoadedImage, 0, 0, this);
    }

	public int getScrollPositionX() {
		return scrollPositionX;
	}

	public void setScrollPositionX(int scrollPositionX) {
		this.scrollPositionX = scrollPositionX;
	}

	public int getScrollPositionY() {
		return scrollPositionY;
	}

	public void setScrollPositionY(int scrollPositionY) {
		this.scrollPositionY = scrollPositionY;
	}
	
}
