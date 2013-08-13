package Ace2;

import java.io.*;
import java.util.*;

public class BigEndianReader extends EndianReader {

  public BigEndianReader (RandomAccessFile raf) {
    super(raf);
  }

  public int readInt() throws IOException {
    return raf.readInt();
  }

}