package me.doubledutch.lazyjson;

import java.util.*;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import me.doubledutch.lazyjson.compressor.*;
import java.nio.charset.StandardCharsets;
/**
 * The LazyNode is the primary output of the LazyParser.
 */
public final class LazyNode{
	// Token types used for classification during parsing
	protected static final byte OBJECT=0;
	protected static final byte ARRAY=1;
	protected static final byte FIELD=2;
	protected static final byte EFIELD=3; // A field with escaped characters

	protected static final byte VALUE_TRUE=4;
	protected static final byte VALUE_FALSE=5;
	protected static final byte VALUE_NULL=6;
	protected static final byte VALUE_STRING=7;
	protected static final byte VALUE_ESTRING=8; // A string with escaped characters
	protected static final byte VALUE_INTEGER=9;
	protected static final byte VALUE_FLOAT=10;

	protected static final byte END_MARKER=11;

	protected byte type;

	protected CharSource source=null;

	// Start and end index into source string for this token.
	// For an object or array, the end index will be the end of the entire
	// object or array.
	protected long startIndex;
	protected long endIndex=-1;

	// Children are stored as a linked list by maintaining the first and last
	// child of this token, as well as a link to the next sibling
	protected LazyNode child;
	protected LazyNode lastChild;
	protected LazyNode next;

	/**
	 * Construct a new LazyNode with the given type and index into the source string
	 *
	 * @param type the type of this token
	 * @param startIndex the index into the source string where this token was found
	 */
	protected LazyNode(byte type,long startIndex){
		this.startIndex=startIndex;
		this.type=type;
	}

	/**
	 * Add a new child to the current linked list of child tokens
	 *
	 * @param token the child to add
	 */
	protected void addChild(LazyNode token){
		// If no children have been added yet, lastChild will be null
		if(lastChild==null){
			child=token;
			lastChild=token;
			return;
		}
		// Set the next pointer on the current end of the child list and set last child to the given token
		lastChild.next=token;
		lastChild=token;
	}

	/**
	 * Count the children attached to this token. Be aware that this requires actual linked list traversal!
	 *
	 * @return the number of child tokens attached to this token
	 */
	protected int getChildCount(){
		int num=0;
		LazyNode token=child;
		while(token!=null){
			num++;
			token=token.next;
		}
		return num;
	}

	/**
	 * Convenience method to create a new token with the type set to array and
	 * with the starting index set to the given index.
	 *
	 * @param index the starting index for this token
	 * @return a new token
	 */
	protected static LazyNode cArray(long index){
		return new LazyNode(ARRAY,index);
	}

	/**
	 * Convenience method to create a new token with the type set to object and
	 * with the starting index set to the given index.
	 *
	 * @param index the starting index for this token
	 * @return a new token
	 */
	protected static LazyNode cObject(long index){
		return new LazyNode(OBJECT,index);
	}

	/**
	 * Convenience method to create a new token with the type set to field and
	 * with the starting index set to the given index.
	 *
	 * @param index the starting index for this token
	 * @return a new token
	 */
	protected static LazyNode cField(long index){
		return new LazyNode(FIELD,index);
	}

	/**
	 * Convenience method to create a new token with the type set to string value and
	 * with the starting index set to the given index.
	 *
	 * @param index the starting index for this token
	 * @return a new token
	 */
	protected static LazyNode cStringValue(long index){
		return new LazyNode(VALUE_STRING,index);
	}

	/**
	 * Convenience method to create a new token with the type set to number value and
	 * with the starting index set to the given index.
	 *
	 * @param index the starting index for this token
	 * @return a new token
	 */
	protected static LazyNode cNumberValue(long index){
		return new LazyNode(VALUE_INTEGER,index);
	}

	/**
	 * Convenience method to create a new token with the type set to a boolean true value and
	 * with the starting index set to the given index.
	 *
	 * @param index the starting index for this token
	 * @return a new token
	 */
	protected static LazyNode cValueTrue(long index){
		return new LazyNode(VALUE_TRUE,index);
	}

	/**
	 * Convenience method to create a new token with the type set to a boolean false value and
	 * with the starting index set to the given index.
	 *
	 * @param index the starting index for this token
	 * @return a new token
	 */
	protected static LazyNode cValueFalse(long index){
		return new LazyNode(VALUE_FALSE,index);
	}

	/**
	 * Convenience method to create a new token with the type set to a null value and
	 * with the starting index set to the given index.
	 *
	 * @param index the starting index for this token
	 * @return a new token
	 */
	protected static LazyNode cValueNull(long index){
		return new LazyNode(VALUE_NULL,index);
	}
	/*
	protected int getIntValue(char[] source) throws LazyException{
		return getIntValue(source,null);
	}*/

	/**
	 * Parses the characters of this token and attempts to construct an integer
	 * value from them.
	 *
	 * @return the integer value if it could be parsed
	 * @throws LazyException if the value could not be parsed
	 */
	protected int getIntValue() throws LazyException{
		long i=startIndex;
		boolean sign=false;
		int value=0;
		if(type==VALUE_FLOAT){
			return (int)getDoubleValue();
		}else if(type==VALUE_STRING || type==VALUE_ESTRING){
			// Attempt to parse as an int, throw if impossible
			{
				if(source.get(i)=='-'){
					sign=true;
					i++;
				}
				for(;i<endIndex;i++){
					char c=source.get(i);
					if(c<'0'||c>'9')throw new LazyException("'"+getStringValue()+"' is not a valid integer",startIndex);
					value+='0'-c;
					if(i+1<endIndex){
						value*=10;
					}
				}
			}
			return sign?value:-value;
		}else if(type==VALUE_INTEGER){
			{
				if(source.get(i)=='-'){
					sign=true;
					i++;
				}
				for(;i<endIndex;i++){
					char c=source.get(i);
					// If we only allow this to be called on integer values, the parsing is pre done!
					// if(c<'0'||c>'9')throw new LazyException("'"+getStringValue(source)+"' is not a valid integer",startIndex);
					value+='0'-c;
					if(i+1<endIndex){
						value*=10;
					}
				}
			}
			return sign?value:-value;
		}
		throw new LazyException("Not an integer",startIndex);
	}

	/**
	 * Parses the characters of this token and attempts to construct a long
	 * value from them.
	 *
	 * @return the long value if it could be parsed
	 * @throws LazyException if the value could not be parsed
	 */
	protected long getLongValue() throws LazyException{
		long i=startIndex;
		boolean sign=false;
		long value=0;
		if(type==VALUE_FLOAT){
			return (long)getDoubleValue();
		}else if(type==VALUE_STRING || type==VALUE_ESTRING){
			// Attempt to parse as an int, throw if impossible
			{
				if(source.get(i)=='-'){
					sign=true;
					i++;
				}
				for(;i<endIndex;i++){
					char c=source.get(i);
					if(c<'0'||c>'9')throw new LazyException("'"+getStringValue()+"' is not a valid long",startIndex);
					value+='0'-c;
					if(i+1<endIndex){
						value*=10;
					}
				}
			}
			return sign?value:-value;
		}else if(type==VALUE_INTEGER){
			{
				if(source.get(i)=='-'){
					sign=true;
					i++;
				}
				for(;i<endIndex;i++){
					char c=source.get(i);
					// If we only allow this to be called on integer values, the parsing is pre done!
					// if(c<'0'||c>'9')throw new LazyException("'"+getStringValue(source)+"' is not a valid integer",startIndex);
					value+='0'-c;
					if(i+1<endIndex){
						value*=10;
					}
				}
			}
			return sign?value:-value;
		}
		throw new LazyException("Not a long",startIndex);
	}

	// protected double getDoubleValue(char[] source) throws LazyException{
	//	return getDoubleValue(source,null);
	// }

	/**
	 * Parses the characters of this token and attempts to construct a double
	 * value from them.
	 *
	 * @return the double value if it could be parsed
	 * @throws LazyException if the value could not be parsed
	 */
	protected double getDoubleValue() throws LazyException{
		double d=0.0;
		String str=getStringValue();
		try{
			d=Double.parseDouble(str);
		}catch(NumberFormatException nfe){
			// This basically can't happen since we already validate the numeric format when parsing
			// throw new LazyException("'"+str+"' is not a valid double",startIndex);
		}
		return d;
	}

	/*protected String getStringValue(char[] source){
		return getStringValue(source,null);
	}*/

	/**
	 * Extracts a string containing the characters given by this token. If the
	 * token was marked as having escaped characters, they will be unescaped
	 * before the value is returned.
	 *
	 * @return the string value held by this token
	 */
	protected String getStringValue(){
		if(type==VALUE_NULL){
			return null;
		}else if(!(type==VALUE_ESTRING||type==EFIELD)){
			return source.substring(startIndex, endIndex);
		}else{
			long length = endIndex - startIndex;
			if (length > Integer.MAX_VALUE) {
				throw new LazyException("string value is too long");
			}
			StringBuilder buf=new StringBuilder((int) length);
			{
				for(long i=startIndex;i<endIndex;i++){
					char c=source.get(i);
					if(c=='\\'){
						i++;
						c=source.get(i);
						if(c=='"' || c=='\\' || c=='/'){
							buf.append(c);
						}else if(c=='b'){
							buf.append('\b');
						}else if(c=='f'){
							buf.append('\f');
						}else if(c=='n'){
							buf.append('\n');
						}else if(c=='r'){
							buf.append('\r');
						}else if(c=='t'){
							buf.append('\t');
						}else if(c=='u'){
							String code = source.substring(i+1, i+5);
							buf.append((char)Integer.parseInt(code, 16));
							i+=4;
						}
					}else{
						buf.append(c);
					}
				}
			}
			return buf.toString();
		}
	}

	protected String getRawStringValue(){
		return source.substring(startIndex, endIndex);
	}

	/**
	 * Returns a string iterator for this tokens children.
	 *
	 * @return an iterator for the children of this token as strings
	 */
	protected Iterator<String> getStringIterator(){
		return new StringIterator(this);
	}

	
	// Debug method used for development purposes only

	protected String toString(int pad){
		String out="";
		for(int i=0;i<pad;i++)out+=" ";
		if(type==OBJECT){
			out+="{";
		}else if(type==ARRAY){
			out+="[";
		}else if(type==FIELD){
			out+="\"";
		}else if(type==VALUE_INTEGER){
			out+="i";
		}else if(type==VALUE_TRUE || type==VALUE_FALSE){
			out+="b";
		}else if(type==VALUE_FLOAT){
			out+="f";
		}else if(type==VALUE_STRING || type==VALUE_ESTRING){
			out+="s";
		}else if(type==VALUE_NULL){
			out+="n";
		}
		out+=":["+startIndex+","+endIndex+"]";
		out+="\n";
		if(child!=null){
			LazyNode token=child;
			while(token!=null){
				out+=token.toString(pad+2);
				token=token.next;
			}
		}else{
			if(child!=null){
				out+=child.toString(pad+2);
			}
		}
		return out;
	}

	// Internal class used to iterate over children as strings
	private final class StringIterator implements Iterator<String>{
		private LazyNode next;

		protected StringIterator(LazyNode token){
			next=token.child;
		}

		public boolean hasNext(){
			return next!=null;
		}

		public void remove() throws UnsupportedOperationException{
			throw new UnsupportedOperationException("Can't remove from token");
		}

		public String next() throws NoSuchElementException{
			if(hasNext()){
				String value=next.getStringValue();
				next=next.next; // If only I could squeeze one more "next" into this statement
				return value;
			}
			throw new NoSuchElementException();
		}
	}
	// Functionality for extracting templates
	private void addCommaSeparatedChildren(Template template){
		LazyNode next=child;
		boolean first=true;
		while(next!=null){
			if(first){
				first=false;
			}else{
				template.addConstant(",");
			}
			next.addSegments(template);
			next=next.next;
		}
	}

	private String getFieldString(){
		return "\""+getRawStringValue()+"\":";
	}

	private void putString(ByteBuffer buf,DictionaryCache dict){
		String str=getStringValue(); 
		short pos=dict.put(str);
		buf.putShort(pos);
		if(pos>-1){
			return;
		}
		byte[] data=str.getBytes(StandardCharsets.UTF_8);
		int size=data.length;
		while(size>255){
			buf.put((byte)0xFF);
			size-=255;
		}
		buf.put((byte)size);
		buf.put(data);
	}

	protected void writeSegmentValues(ByteBuffer buf,DictionaryCache dict) throws BufferOverflowException{
		if(type==OBJECT || type==ARRAY){
			LazyNode next=child;
			while(next!=null){
				next.writeSegmentValues(buf,dict);
				next=next.next;
			}
		}else if(type==FIELD){
			if(child.type==VALUE_TRUE){
				buf.put((byte)1);
			}else if(child.type==VALUE_FALSE){
				buf.put((byte)0);
			}else if(child.type==VALUE_STRING || type==VALUE_ESTRING){
				child.putString(buf,dict);
			}else if(child.type==VALUE_INTEGER){
				long l=child.getLongValue();
				if(l<128 && l>=-128){
					buf.put((byte)l);
				}else if(l<32768 && l>=-32768){
					buf.putShort((short)l);
				}else if(l<=2147483647 && l>=-2147483648){
					buf.putInt((int)l);
				}else{
					buf.putLong(l);
				}
			}else if(child.type==VALUE_FLOAT){
				buf.putDouble(child.getDoubleValue());
			}else{
				child.writeSegmentValues(buf,dict);
			}
		}else if(type==VALUE_TRUE){
			buf.put((byte)1);
		}else if(type==VALUE_FALSE){
			buf.put((byte)0);
		}else if(type==VALUE_STRING || type==VALUE_ESTRING){
			putString(buf,dict);
		}else if(type==VALUE_INTEGER){
			long l=getLongValue();
			if(l<128 && l>=-128){
				buf.put((byte)l);
			}else if(l<32768 && l>=-32768){
				buf.putShort((short)l);
			}else if(l<=2147483647 && l>=-2147483648){
				buf.putInt((int)l);
			}else{
				buf.putLong(l);
			}
		}else if(type==VALUE_FLOAT){
			buf.putDouble(getDoubleValue());
		}
	}

	protected void addSegments(Template template){
		if(type==OBJECT){
			template.addConstant("{");
			addCommaSeparatedChildren(template);
			template.addConstant("}");
		}else if(type==ARRAY){
			template.addConstant("[");
			addCommaSeparatedChildren(template);
			template.addConstant("]");
		}else if(type==FIELD){
			if(child.type==VALUE_TRUE || child.type==VALUE_FALSE){
				template.addBoolean(getFieldString());
			}else if(child.type==VALUE_STRING){
				template.addString(getFieldString());
			}else if(child.type==VALUE_NULL){
				template.addNull(getFieldString());
			}else if(child.type==VALUE_INTEGER){
				long l=child.getLongValue();
				if(l<128 && l>=-128){
					template.addByte(getFieldString());
				}else if(l<32768 && l>=-32768){
					template.addShort(getFieldString());
				}else if(l<=2147483647 && l>=-2147483648){
					template.addInt(getFieldString());
				}else{
					template.addLong(getFieldString());
				}
			}else if(child.type==VALUE_FLOAT){
				// TODO: could we differentiate for float's vs doubles?
				template.addDouble(getFieldString());
			}else{
				template.addConstant(getFieldString());
				child.addSegments(template);
			}
		}else if(type==VALUE_TRUE || type==VALUE_FALSE){
			template.addBoolean();
		}else if(type==VALUE_NULL){
			template.addNull();
		}else if(type==VALUE_STRING){
			template.addString();
		}else if(type==VALUE_INTEGER){
			long l=getLongValue();
			if(l<128 && l>=-128){
				template.addByte();
			}else if(l<32768 && l>=-32768){
				template.addShort();
			}else if(l<=2147483647 && l>=-2147483648){
				template.addInt();
			}else{
				template.addLong();
			}
		}else if(type==VALUE_FLOAT){
			template.addDouble();
		}
	}

	// Functionality for reading and writing LazyNode structures
	protected static LazyNode readFromBuffer(byte[] raw){
		ByteBuffer buf=ByteBuffer.wrap(raw);
		return readFromBuffer(buf);
	}

	protected static LazyNode readFromBuffer(ByteBuffer buf){
		byte type=buf.get();
		if(type==END_MARKER)return null;
		long startIndex=buf.getLong();
		long endIndex=buf.getLong();
		// TODO: add constructor for this purpose
		LazyNode node=new LazyNode(type,startIndex);
		node.endIndex=endIndex; 
		if(type==OBJECT || type==ARRAY){
			LazyNode child=readFromBuffer(buf);
			node.child=child;
			node.lastChild=child;
			child=readFromBuffer(buf);
			while(child!=null){
				node.lastChild.next=child;
				node.lastChild=child;
				child=readFromBuffer(buf);
			}
		}else if(type==FIELD){
			LazyNode child=readFromBuffer(buf);
			node.child=child;
			node.lastChild=child;
		}
		return node;
	}

	protected void writeToBuffer(ByteBuffer buf){
		// ByteBuffer must be allocated with enough space before calling
		buf.put(type);
		buf.putLong(startIndex);
		buf.putLong(endIndex);
		if(type==OBJECT || type==ARRAY){
			LazyNode n=child;
			while(n!=null){
				n.writeToBuffer(buf);
				n=n.next;
			}
			buf.put(END_MARKER);
		}else if(type==FIELD){
			child.writeToBuffer(buf);
		}
	}

	protected int getBufferSize(){
		int size=1+8+8; // type, start and end index, modifier
		if(type==OBJECT || type==ARRAY){
			LazyNode n=child;
			while(n!=null){
				size+=n.getBufferSize();
				n=n.next;
			}
			size+=1;
		}else if(type==FIELD){
			size+=child.getBufferSize();
		}
		return size;
	}


}