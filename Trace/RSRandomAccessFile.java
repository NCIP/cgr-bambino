package Trace;
import java.io.*;

// if only we could extend RandomAccessFile (sigh)

class RSRandomAccessFile implements RandomAccessStream {
  RandomAccessFile raf;
  
  private String name;

  RSRandomAccessFile (String name, String mode) {
    this.name = name;
    try {
      raf = new RandomAccessFile(name, mode);
    } catch (java.io.IOException e) {
      System.out.println("local file read of " + name + " failed");
    }
  }
  
  public void seek (int offset) {
    try {
      raf.seek(offset);
    } catch (java.io.IOException e) {
      System.out.println("seek to " + offset + " failed");
    }
  }

  public int read (byte [] b) {
    int num_bytes = -1;
    try { 
      num_bytes = raf.read(b);
    } catch (java.io.IOException e) {
      System.out.println("read failed");
    }
    return(num_bytes);
  }

  public int readInt () {
    int result = -1;
    try {
      result = raf.readInt();
    } catch (java.io.IOException e) {
      System.out.println("readInt failed");
    }
    return result;
  }

  public int readUnsignedByte () {
    int result = -1;
    try {
      result = raf.readUnsignedByte();
    } catch (java.io.IOException e) {
      System.out.println("readUnsignedByte failed");
    }
    return result;
  }

  public void skipBytes (int num) {
    try {
      raf.skipBytes(num);
    } catch (java.io.IOException e) {
      System.out.println("skipBytes failed");
    }
  }

  public byte readByte() {
    byte result = -1;
    try {
      result = raf.readByte();
    } catch (java.io.IOException e) {
      System.out.println("skipBytes failed");
    }
    return result;
  }

  public short readShort () {
    short result = -1;
    try {
      result = raf.readShort();
    } catch (java.io.IOException e) {
      System.out.println("readShort failed");
    }
    return result;
  }
  
  public String readString (int num_bytes) {
    byte [] b = new byte[num_bytes];
    if (this.read(b) < num_bytes) {
      // not enough data to fill the string!  Return empty string.
      // FIX ME?
      return("");
    } else {
      //      return(new String(b, 0));
      return(new String(b));
      // see FakeFile.java as well
    }
  }

  public long length () {
    File f = new File(name);
    return (f.length());
  }

}
