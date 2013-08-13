// 1.1

package Funk;

import java.io.*;
import java.util.zip.*;

public class Stream {
  public static ByteArrayInputStream get_zip_stream (String zipfile,
						     String filename) {
    // return a ByteArrayInputStream for the entire uncompressed
    // contents of the specified zipfile entry.
    try {
      ZipInputStream zip = new ZipInputStream(new FileInputStream(zipfile));
      ZipEntry ze;
      while ((ze = zip.getNextEntry()) != null) {
	if (ze.getName().equals(filename)) {
	  int size = (int) ze.getSize();
	  System.out.println("got zipfile!");  // debug
	  byte [] b = new byte[size];
	  int pending = size;
	  int offset = 0;
	  int read;
	  //	  while ((read = zip.read(b,offset,pending)) != -1) {
	  // doesn't seem to return -1 (at least if last read finished)
	  while (offset < size) {
	    read = zip.read(b, offset, pending);
	    pending -= read;
	    offset += read;
	  }
	  return new ByteArrayInputStream(b);
	}
      }
    } catch (Exception e) {
      System.err.println("get_zip_stream(): " + e);  // debug
    }
    return null;
  }
}
