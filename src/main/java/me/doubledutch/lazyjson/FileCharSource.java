package me.doubledutch.lazyjson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * File-based implementation of CharSource.
 * Allows to read JSON content exceeding 2 GB in size.
 * The single-byte Latin-1 encoding for file contents is assumed.
 */
public class FileCharSource implements CharSource {

	private final RandomAccessFile file;
	private final long length;

	private final int chunkStep;
	private final byte[] chunk;
	private long chunkFirstOffset = 0;
	private long chunkLastOffset = -1;

	/**
	 * Create a new file-based character source.
	 * The Latin-1 encoding will be assumed.
	 *
	 * @param file file to be accessed
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public FileCharSource(File file) throws IOException{
		this(file, 65536); // default chunk
	}

	/**
	 * Create a new file-based character source with buffer of specified size.
	 * This variant of constructor is used mainly for testing purposes.
	 * For production code, the one-argument constructor should work just fine.
	 *
	 * @param file file to be accessed
	 * @param chunkStep the internal buffer will be allocated with chunkStep+1 bytes
	 * @throws IOException
	 */
	public FileCharSource(File file, int chunkStep) throws IOException{
		this.file = new RandomAccessFile(file, "r");
		this.length = this.file.length();
		this.chunkStep = chunkStep;
		this.chunk = new byte[chunkStep+1]; // one extra byte to allow for fast reverse lookup
	}

	@Override
	public char get(long n){
		if (n < 0 || n >= length) {
			throw new IllegalArgumentException("given n outside file range");
		}
		fetchChunk(n);
		int result = chunk[(int) (n - chunkFirstOffset)];
		// implicitly assuming Latin-1 encoding
		return (char) (result & 0xFF);
	}

	@Override
	public long length() {
		return length;
	}

	@Override
	public String substring(long startIndex, long endIndex) {
		if (startIndex < 0 || endIndex > length) {
			throw new IllegalArgumentException("given index outside file range");
		}
		if (startIndex >= endIndex) {
			return "";
		}

		String result = "";
		while (startIndex < endIndex) {
			fetchChunk(startIndex);
			int offsetInChunk = (int) (startIndex-chunkFirstOffset);
			int readFromChunk = (endIndex <= chunkLastOffset+1) ? (int) (endIndex-startIndex) : (int) (chunkLastOffset+1-startIndex);

			result += new String(chunk, offsetInChunk, readFromChunk, StandardCharsets.ISO_8859_1);

			startIndex += readFromChunk;
		}
		return result;
	}

	private void fetchChunk(long n){
		if (n < chunkFirstOffset || n > chunkLastOffset) try {
			long chunkOffset = (n / chunkStep) * chunkStep;
			file.seek(chunkOffset);
			int bytesRead = file.read(chunk);
			chunkFirstOffset = chunkOffset;
			chunkLastOffset = chunkOffset + bytesRead - 1;
		} catch (IOException ex) {
			throw new LazyException("cannot read from file", n);
		}
	}

}
