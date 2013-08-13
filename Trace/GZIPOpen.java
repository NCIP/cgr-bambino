// 1.1

package Trace;

import java.io.*;
import java.net.*;
import java.util.zip.*;

public class GZIPOpen {
  // isolate in a separate class so we can run rest of code on old JVMs

  public static DataInputStream open (DataInputStream d) throws java.io.IOException {
    return new DataInputStream(new GZIPInputStream(d));
  }

}
