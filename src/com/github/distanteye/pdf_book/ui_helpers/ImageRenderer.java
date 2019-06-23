/**
 * 
 */
package com.github.distanteye.pdf_book.ui_helpers;

import java.awt.image.BufferedImage;

import com.github.distanteye.pdf_book.ui.Tab;

/**
 * @author Vigilant
 *
 */
public interface ImageRenderer {
	
	/**
	 * Renders a PDF page using the implementing renderer
	 * @param tab The tab tied to the request (render will modify settings and provide information to this tab as output)
	 * @param page The page number to render (note the interface assumption is that the first page of the pdf is page=1. 
	 * 			   Implementing classes that use 0 alignment should still take page=1 and convert internally
	 * @param dpi The dpi to render at
	 * @return If successful, a BufferedImage, if any errors occur : null
	 */
	BufferedImage renderPDF(Tab t, int page, int dpi);
	void closeOutTabInfo(Tab t);
}
