package Funk;

import java.io.*;
import java.util.zip.*;

public class ZipTools {
  private ZipInputStream zis;
  private ZipEntry current_ze;
  private byte[] current_bar;

  public ZipTools (ZipInputStream zis) {
    this.zis = zis;
  }

  public byte[] get_bytearray () throws IOException {
    // convert current ZipEntry into bytearray
    byte[] buf = null;
    if (current_ze != null) {
      long size = current_ze.getSize();
      if (size == -1) {
        throw new IOException("unknown size for " + current_ze.getName());
      } else {
	//        System.err.println(current_ze.getName() + ": " + size);
        buf = new byte[(int) size];
        int wanted = (int) size;
        int ptr = 0;
        int read;
        while (wanted > 0) {
          read = zis.read(buf, ptr, wanted);
          ptr += read;
          wanted -= read;
          if (read == -1) break;
        }

        if (wanted > 0) {
          throw new IOException("read length mismatch");
        }
      }
    }
    return buf;
  }

  public ByteArrayInputStream get_bytearrayinputstream() throws IOException {
    byte[] buf = get_bytearray();
    ByteArrayInputStream bais = null;
    if (buf != null) bais = new ByteArrayInputStream(buf);
    return bais;
  }

  
  public boolean next() throws IOException {
    current_bar = null;
    current_ze = zis.getNextEntry();
    return current_ze == null ? false : true;
  }

  public String get_name() {
    return current_ze == null ? null : current_ze.getName();
  }

  public static void main (String [] argv) {
    try {
      FileInputStream fis = new FileInputStream("text.zip");
      ZipInputStream zis = new ZipInputStream(fis);

      ZipTools zt = new ZipTools(zis);
      while (zt.next()) {
        if (true) {
          ByteArrayInputStream bais = zt.get_bytearrayinputstream();
          BufferedReader br = new BufferedReader(new InputStreamReader(bais));
          String line;
          System.err.println("name=" + zt.get_name());  // debug

          while ((line = br.readLine()) != null) {
            System.err.println("hey now:" + line);  // debug
          }
        } else {
          byte[] buf = zt.get_bytearray();
          System.err.println(new String(buf));  // debug
        }
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }

}
