package com.github.distanteye.pdf_book.ui;

import java.io.File;
import java.io.IOException;

/**
 * Main model representing the non-UI data for each Tab open in the system
 * 
 * @author Vigilant
 *
 */
public class Sheet {

	private String filePath;
	private int currentPage;
	private int dpi;
	private int maxPages;
		
	/**
	 * Constructs and initializes a Sheet with specified PDF file location, and currently opened page
	 * @param filePath Valid file path to the PDF the sheet is associated to
	 * @param currentPage What page on the linked PDF should be open
	 * @param dpi The dpi to load the pdf into an image from
	 * @throws IOException Exception if filePath doesn't point to a usable PDF file
	 */
	public Sheet(String filePath, int currentPage, int dpi) throws IOException {
		super();
		this.filePath = filePath;
		
		// note this is a potential hole with verifying a sane currentPage (vs using setCurrentPage) but with maxPages not generated until later...
		this.currentPage = currentPage; 
		this.dpi = dpi;
		maxPages = 0;
		
		// check file path for sanity
		File f = new File(filePath);
		if (!f.exists() || !f.isFile() || !f.canRead())
		{
			throw new IOException("'" + filePath + "' does not exist or is otherwise not a valid PDF");
		}
	}


	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		if ((maxPages != 0 && currentPage > maxPages) || currentPage < 1)
		{
			throw new IllegalArgumentException("Can't change current page past the last page or before the first one");
		}
		
		this.currentPage = currentPage;
	}

	public String getFilePath() {
		return filePath;
	}
	
	public int getMaxPages() {
		return maxPages;
	}


	public void setMaxPages(int maxPages) {
		this.maxPages = maxPages;
	}


	public int getDpi() {
		return dpi;
	}


	public void setDpi(int dpi) {
		this.dpi = dpi;
	}
	

}
