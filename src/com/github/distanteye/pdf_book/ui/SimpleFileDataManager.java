/**
 * 
 */
package com.github.distanteye.pdf_book.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Meant for debugging and comparisons as a control group : this DataManager doesn't manage anything at all, it just does straight File/IO
 * 
 * @author Vigilant
 *
 */
public class SimpleFileDataManager implements DataManager {


	/**
	 * Creates a new DataManager and initializes both maps necessary to maintain file/tab indexing
	 */
	public SimpleFileDataManager() {
	}
	
	public byte[] checkOut(Tab t) throws IOException
	{
		String key = t.getFilePath().toLowerCase();
		
		File input = new File(key);
		
		byte[] output = Files.readAllBytes(input.toPath());
		
		return output;
	}
	
	@Override
	public void checkOutQuiet(Tab t) throws IOException {
		// no action needed since checkOut isn't actually caching/storing anything
	}
	
	public void checkIn(Tab t)
	{
		// nothing needs to be done here since we don't actually do anything with mappings
	}

}
