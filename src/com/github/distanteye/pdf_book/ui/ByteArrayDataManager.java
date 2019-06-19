/**
 * 
 */
package com.github.distanteye.pdf_book.ui;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

/**
 * Handles translation of file requests (via path) to memory mapped files and the resulting read requests
 * 
 * Files are maintained in a checkout/checkin system that tries to minimize thrashing and redundancies via keeping files
 * in memory when a Tab leaves but other Tabs still use that file, as well as removing files from memory once no Tabs use the file
 * 
 * This implementation uses RandomAccessFile/MappedByteBuffer
 * 
 * @author Vigilant
 *
 */
public class ByteArrayDataManager implements DataManager {

	private HashMap<String,byte[]> fileMap;
	private HashMap<Tab,String> tabMap; 
	
	/**
	 * Creates a new DataManager and initializes both maps necessary to maintain file/tab indexing
	 */
	public ByteArrayDataManager() {
		fileMap = new HashMap<String, byte[]>();
		tabMap = new HashMap<Tab, String>();
	}
	
	public byte[] checkOut(Tab t) throws IOException
	{
		String key = t.getFilePath().toLowerCase();
		
		tabMap.put(t, key);
		
		return getByteArray(key);
	}
	
	@Override
	public void checkOutQuiet(Tab t) throws IOException {
		String key = t.getFilePath().toLowerCase();
		
		tabMap.put(t, key);
		getByteArray(key);
	}
	
	
	public void checkIn(Tab t)
	{
		String key = tabMap.get(t);
		tabMap.remove(t);
		
		if (!tabMap.values().contains(key))
		{
			fileMap.remove(key);
		}
	}
	
	protected byte[] getByteArray(String filePath) throws IOException
	{
		if (fileMap.containsKey(filePath))
		{
			return fileMap.get(filePath);
		}
		else
		{
			RandomAccessFile mappedFile = new RandomAccessFile(filePath, "r");
			long size = mappedFile.length();
			byte[] contents = new byte[(int) size];
			
			mappedFile.read(contents);
			
			mappedFile.close();
			
			fileMap.put(filePath, contents);
			return contents;
		}
	}

}
