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
		this.currentPage = currentPage;
		this.dpi = dpi;
		
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
		this.currentPage = currentPage;
	}

	public String getFilePath() {
		return filePath;
	}


	public int getDpi() {
		return dpi;
	}


	public void setDpi(int dpi) {
		this.dpi = dpi;
	}
	

}
