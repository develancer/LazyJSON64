package me.doubledutch.lazyjson;

/**
 * Interface to accessing data for LazyParser and LazyNode.
 */
public interface CharSource {

	/**
	 * Return a single character at given position.
	 * @param n absolute position of the character
	 * @return n-th character
	 */
	public char get(long n);

	/**
	 * Return a total length of the source.
	 * @return length in characters
	 */
	public long length();

	/**
	 * Return a sequence of characters as a String object.
	 * @param startIndex the beginning index, inclusive
	 * @param endIndex the ending index, exclusive
	 * @return sequence of characters from startIndex to endIndex-1
	 */
	public String substring(long startIndex, long endIndex);

}
