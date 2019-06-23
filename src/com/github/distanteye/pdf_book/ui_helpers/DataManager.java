/**
 * 
 */
package com.github.distanteye.pdf_book.ui_helpers;

import java.io.IOException;

import com.github.distanteye.pdf_book.ui.Tab;

/**
 * Handles translation of file requests (via path) to memory mapped files and the resulting read requests
 * 
 * Files are maintained in a checkout/checkin system that tries to minimize thrashing and redundancies via keeping files
 * in memory when a Tab leaves but other Tabs still use that file, as well as removing files from memory once no Tabs use the file
 * 
 * @author Vigilant
 *
 */
public interface DataManager {
	byte[] checkOut(Tab t) throws IOException;
	void checkOutQuiet(Tab t) throws IOException;
	void checkIn(Tab t);
	int getFileSize(Tab t);
}
