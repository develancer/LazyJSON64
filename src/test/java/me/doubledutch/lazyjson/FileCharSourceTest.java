
package me.doubledutch.lazyjson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.*;

public class FileCharSourceTest{

	private final static String STRING = "abcdefghi";
	private static File tempFile;
	private FileCharSource source;

	@BeforeClass
	public static void setUpBeforeClass() throws IOException{
		tempFile = File.createTempFile("tmp", "txt");
		try (FileWriter fw = new FileWriter(tempFile)) {
			fw.write(STRING);
		}
	}

	@Before
	public void setUp() throws IOException{
		source = new FileCharSource(tempFile, 4);
	}

	@AfterClass
	public static void tearDownAfterClass(){
		tempFile.delete();
	}

	@Test
    public void testGet() throws IOException{
		for (int i=0; i<STRING.length(); ++i) {
			Assert.assertEquals(STRING.charAt(0), source.get(0));
		}
    }

    @Test
    public void testLength() throws IOException{
		Assert.assertEquals(STRING.length(), source.length());
    }

    @Test
	public void testSubstring(){
		Assert.assertEquals(STRING.substring(4, 9), source.substring(4, 9));
		Assert.assertEquals(STRING.substring(2, 9), source.substring(2, 9));
		Assert.assertEquals(STRING.substring(2, 5), source.substring(2, 5));
		Assert.assertEquals(STRING.substring(0, 0), source.substring(0, 0));
	}

}