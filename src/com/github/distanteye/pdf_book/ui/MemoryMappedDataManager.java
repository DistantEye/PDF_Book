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
 * This implementation uses RandomAccessFile & MappedByteBuffer, storing only the former, which may be the wrong move
 * 
 * @author Vigilant
 *
 */
public class MemoryMappedDataManager implements DataManager {

	private HashMap<String, RandomAccessFile> fileMap;
	private HashMap<String,Integer> fileSizeMap;
	private HashMap<Tab,String> tabMap; 
	
	/**
	 * Creates a new DataManager and initializes both maps necessary to maintain file/tab indexing
	 */
	public MemoryMappedDataManager() {
		fileMap = new HashMap<String, RandomAccessFile>();
		fileSizeMap = new HashMap<String,Integer>();
		tabMap = new HashMap<Tab, String>();
	}
	
	public byte[] checkOut(Tab t) throws IOException
	{
		String key = t.getFilePath();
		
		tabMap.put(t, key);
		
		MappedByteBuffer buf = getByteBuffer(key);
		byte[] output = new byte[buf.remaining()];
		buf.get(output);
		buf.rewind();
		
		return output;
	}
	
	@Override
	public void checkOutQuiet(Tab t) throws IOException {
		// basically a short copy of the checkOut method that doesn't bother to map the byteBuffer to an expensive variable
		String key = t.getFilePath();
		
		tabMap.put(t, key);
		getByteBuffer(key);
		
		return;
	}
	
	public int getFileSize(Tab t)
	{
		if (fileSizeMap.containsKey(t))
		{
			return fileSizeMap.get(t);
		}
		else
		{
			try {
				return checkOut(t).length;
			} catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
		}
	}
	
	public void checkIn(Tab t)
	{
		String key = tabMap.get(t);
		tabMap.remove(t);
		
		if (!tabMap.values().contains(key))
		{
			RandomAccessFile removedItem = fileMap.remove(key);
			try {
				removedItem.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected MappedByteBuffer getByteBuffer(String filePath) throws IOException
	{
		RandomAccessFile mappedFile;
		
		if (fileMap.containsKey(filePath))
		{
			mappedFile = fileMap.get(filePath);
		}
		else
		{
			mappedFile = new RandomAccessFile(filePath, "r");	
			fileMap.put(filePath, mappedFile);
		}
		
		long size = mappedFile.length();
		MappedByteBuffer buf = mappedFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, size);
		fileSizeMap.put(filePath, (int)size);
		return buf;
	}	

}
