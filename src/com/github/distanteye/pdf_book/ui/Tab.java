package com.github.distanteye.pdf_book.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JPanel;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Main model representing the UI data for each Tab open in the system, as well as managing the retrieval and display of the current Tab's open PDF page
 * 
 * @author Vigilant
 *
 */
public class Tab extends JPanel {


	private static final long serialVersionUID = -1549012741303631233L;
	
	private Sheet sheet;
	private String displayName;
	private int scrollPositionX;
	private int scrollPositionY;
	private PDDocument document;
	private boolean isOpen;
	
	private BufferedImage currentLoadedImage;
	
	private int maxPages;
	
	/**
	 * Constructs and initializes a
	 * @param displayName What text the navigation bar's tab should display for this Tab
	 * @param filePath Valid file path to the PDF the sheet is associated to
	 * @param currentPage What page on the linked PDF should be open
	 * @param dpi The dpi to load the pdf into an image from
	 * @throws IOException 
	 * @throws InvalidPasswordException 
	 */
	public Tab(String displayName, String filePath, int currentPage, int dpi) throws InvalidPasswordException, IOException {
		super();
		this.sheet = new Sheet(filePath, currentPage, dpi);
		this.displayName = displayName;
		this.scrollPositionX = 0;
		this.scrollPositionY = 0;
		document = null;
		isOpen = false;
		maxPages = 0; // this should be filled in later
		recalculatePage(false);
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
		return maxPages;
	}

	public void setCurrentPage(int currentPage) {
		sheet.setCurrentPage(currentPage);
		recalculatePage(true);
	}

	public String getFilePath() {
		return sheet.getFilePath();
	}
	
	public int getDpi() {
		return sheet.getDpi();
	}

	public void setDpi(int dpi) {
		sheet.setDpi(dpi);
		recalculatePage(true);
	}
	
	protected void recalculatePage(boolean leaveOpen) 
	{
		String filePath = getFilePath();
		int currentPage = getCurrentPage();
				
		try {
			
			document = PDDocument.load(new File(filePath));
		
			isOpen = true;
			PDFRenderer pdfRenderer = new PDFRenderer(document);
			currentLoadedImage = pdfRenderer.renderImageWithDPI(currentPage, sheet.getDpi(), ImageType.RGB);		
			maxPages = document.getNumberOfPages();
			this.setPreferredSize(new Dimension(currentLoadedImage.getWidth(), currentLoadedImage.getHeight()));
			this.setMaximumSize(new Dimension(currentLoadedImage.getWidth(), currentLoadedImage.getHeight()));
	
			if (!leaveOpen)
			{
				isOpen = false;
				document.close();
			}
		
		} catch (IOException e) {
			throw new HandledUIException(e.getMessage());
		}
		
		repaint();
		
	}
	
	public void onLeaving() throws IOException
	{
		if (document != null && isOpen)
		{
			document.close();
			isOpen = false;
		}
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
