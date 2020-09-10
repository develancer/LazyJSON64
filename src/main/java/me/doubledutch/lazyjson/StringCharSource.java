package me.doubledutch.lazyjson;

/**
 * Simple string-based implementation of CharSource.
 */
public class StringCharSource implements CharSource {

	private final String string;
	private final int length;

	/**
	 * Create a new String-based character source.
	 * Given object will be used as reference,
	 * and its contents shall not be copied by this call.
	 * @param string string to be accessed by this instance
	 */
	public StringCharSource(String string){
		this.string = string;
		this.length = string.length();
	}

	@Override
	public char get(long n){
		if (n < 0 || n >= length) {
			throw new IllegalArgumentException("given n outside string range");
		}
		return string.charAt((int) n);
	}

	@Override
	public long length() {
		return length;
	}

	@Override
	public String substring(long startIndex, long endIndex){
		if (startIndex < 0 || endIndex > length) {
			throw new IllegalArgumentException("given index outside file range");
		}
		if (startIndex >= endIndex) {
			return "";
		}
		return string.substring((int) startIndex, (int) endIndex);
	}

}
