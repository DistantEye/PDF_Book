/**
 * 
 */
package com.github.distanteye.pdf_book.ui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * @author Vigilant
 *
 */
public class PdfBoxRenderer implements ImageRenderer {

	private DataManager dm;
	
	/**
	 * 
	 */
	public PdfBoxRenderer(DataManager dm) {
		this.dm = dm;
	}

	/**
	 * Renders a PDF page using internally packaged PDFBox. Note that PDFBox uses 0 aligned page numbers so we convert to 1 aligned by using page-1
	 * @param tab The tab tied to the request (render will modify settings and provide information to this tab as output)
	 * @param page The page number to render (note what pdfbox will process is (page-1) because PDFBox has the first page as page 0)
	 * @param dpi The dpi to render at
	 * @return If successful, a BufferedImage, if any errors occur : null
	 */
	@Override
	public BufferedImage renderPDF(Tab t, int page, int dpi) {
		long startTime = System.nanoTime();
		
		BufferedImage currentLoadedImage = null;
		
		// load PDF document
		PDDocument document;
		try {
			document = PDDocument.load(dm.checkOut(t));
		
			PDFRenderer pdfRenderer = new PDFRenderer(document);
			currentLoadedImage = pdfRenderer.renderImageWithDPI(page-1, t.getDpi(), ImageType.RGB);		
			t.setMaxPages(document.getNumberOfPages());
			t.setPreferredSize(new Dimension(currentLoadedImage.getWidth(), currentLoadedImage.getHeight()));
			t.setMaximumSize(new Dimension(currentLoadedImage.getWidth(), currentLoadedImage.getHeight()));
	
			document.close();
			
		} catch (InvalidPasswordException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		long endTime = System.nanoTime();
		double duration = (endTime - startTime)/1000000;
		System.out.println("Rendered " + t.getDisplayName() + "(" + page + ")" +  "in " + duration + " milliseconds");
		
		return currentLoadedImage;		
	}

	@Override
	public void closeOutTabInfo(Tab t) {
		dm.checkIn(t);
	}

	
}
