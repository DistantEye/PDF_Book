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
 * This implementation stores MappedByteBuffers instead of the Mapped files themselves
 * 
 * @author Vigilant
 *
 */
public class MappedByteBufferDataManager implements DataManager {

	private HashMap<String,MappedByteBuffer> fileMap;
	private HashMap<Tab,String> tabMap; 
	
	/**
	 * Creates a new DataManager and initializes both maps necessary to maintain file/tab indexing
	 */
	public MappedByteBufferDataManager() {
		fileMap = new HashMap<String, MappedByteBuffer>();
		tabMap = new HashMap<Tab, String>();
	}
	
	public byte[] checkOut(Tab t) throws IOException
	{
		String key = t.getFilePath().toLowerCase();
		
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
		String key = t.getFilePath().toLowerCase();
		
		tabMap.put(t, key);
		getByteBuffer(key);
		
		return;
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
	
	protected MappedByteBuffer getByteBuffer(String filePath) throws IOException
	{
		if (fileMap.containsKey(filePath))
		{
			return fileMap.get(filePath);
		}
		else
		{
			RandomAccessFile mappedFile = new RandomAccessFile(filePath, "r");
			long size = mappedFile.length();
			MappedByteBuffer buf = mappedFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, size);
			buf.load(); // we want to make sure the whole thing ends up in there
			mappedFile.close();
			fileMap.put(filePath, buf);
			return buf;
		}
	}	

}
