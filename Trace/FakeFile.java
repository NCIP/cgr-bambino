package Trace;

import java.io.*;
import java.util.*;
import Funk.Notifier;

/*
   A class that fakes random file i/o methods on an array of byte data.
   Converts stream data to random access.  
   Disadvantage: has to read entire stream first.
 */

public class FakeFile extends Notifier implements RandomAccessStream {
  private byte [] bytes;
  private int position = 0;
  private DataInputStream s;

  FakeFile (byte [] b) {
    bytes = b;
  }

  FakeFile (DataInputStream s) {
    this.s = s;
    load();
  }

  FakeFile (DataInputStream s, Observer o) {
    this.s = s;
    addObserver(o);
    load();
  }

  private void load() {
    // copy all data from a stream into FakeFile byte buffer.
    bytes = null;
    byte [] buffer = new byte[16384];
    int buffer_length = buffer.length;
    int i = 0;
    boolean done = false;
    int b,av,buf_left,count,actual_read;
    try {
      long start = System.currentTimeMillis();
      while (done == false) {
	i = 0;
	while (s != null) {
	  av = s.available();
	  //  System.out.println("avail:" + av);
 	  if (av > 0) {
 	    // we can read multiple bytes without blocking; do it
 	    buf_left = buffer_length - i;
 	    count = (av > buf_left) ? buf_left : av;
	    // 	    System.out.println("reading chunk of " + count + " at i=" + i);  // debug
 	    actual_read = s.read(buffer, i, count);
	    //	    System.out.println("actual:" + actual_read);  // debug
	    if (actual_read == -1) {
	      System.out.println("fakefile: -1 read??");  // debug
	      break;
	    } else {
	      i += actual_read;
	    }
 	  } else {
	    // have to block to read more, or we're at end of stream.
	    // Try reading a single byte.
	    if ((b = s.read()) == -1) {
	      // end of stream
	      done = true;
	      break;
	    } else {
	      buffer[i++] = (byte) b;
	    }
	  }
	  int bl = bytes == null ? i : bytes.length + i;
	  notify_check(bl);
	  if (i == buffer_length) break;
	}

	if (bytes == null) {
	  // first pass
	  bytes = new byte[i];
	  System.arraycopy(buffer, 0, bytes, 0, i);
	} else {
	  // subsequent passes: append (yuck)
	  byte [] newdata = new byte[bytes.length + i];
	  System.arraycopy(bytes, 0, newdata, 0, bytes.length);
	  System.arraycopy(buffer, 0, newdata, bytes.length, i);
	  bytes = newdata;
	}
      }
      //      System.out.println("fakefile load:" + (System.currentTimeMillis() - start));  // debug
    } catch (java.io.IOException e) {
      System.out.println(e);
    }
    // System.out.println("read " + bytes.length + " bytes");
  }

  public void seek (int pos) {
    this.position = pos;
  }

  public byte [] read (int count) {
    // return count bytes from the stream in a byte array.
    byte [] result = new byte[count];
    System.arraycopy(bytes, position, result, 0, count);
    return(result);
  }

  public int read (byte [] b) {
    // read into specified array, filling it if possible, returning #
    // of bytes loaded.
    int count=0;
    for (int i=0; i < b.length; i++) {
      if (position >= bytes.length) break;
      b[i] = bytes[position++];
      count++;
    }
    return(count);
  }
  
  public byte readByte() {
    return(bytes[position++]);
  }

  public int readUnsignedByte() {
    return(bytes[position++] & 0xff);
  }

  public int readInt() {
    // convert 4 bytes of data into a big-endian integer.
    int result = (bytes[position + 3] & 0xff) +
      ((bytes[position + 2] & 0xff) << 8)  +
      ((bytes[position + 1] & 0xff) << 16) +
      ((bytes[position] & 0xff) << 24);

    position += 4;
    return(result);
  }

  public void skipBytes(int count) {
    position += count;
  }
  
  public short readShort() {
    // convert 2 bytes of data into a big-endian short.
    short result = (short) ((bytes[position + 1] & 0xff) +
			    ((bytes[position] & 0xff) << 8));
    position += 2;
    return(result);
  }

  public String readString (int num_bytes) {
    byte [] b = new byte[num_bytes];
    if (this.read(b) < num_bytes) {
      // not enough data to fill the string!  Return empty string.
      // FIX ME?
      return("");
    } else {
      return(new String(b));
      // if this is problematic might also try:
      // new String(b, "UTF8"));
    }
  }

  public long length () {
    // return length of data read
    return bytes.length;
  }

}
