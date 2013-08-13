package Funk;

import java.io.*;

public class Sys {

  public static int exec_command (String cmd, boolean print) {
    int result = -1;
    try {
      System.err.println("executing " + cmd);  // debug
      Runtime rt = Runtime.getRuntime();
      Process p = rt.exec(cmd);
      try {
	if (print) print_process(p);
      } catch (Exception e) {
	System.err.println("ERROR: " + e);  // debug
      }
      p.waitFor();
      result = p.exitValue();
    } catch (Exception e) {
      System.err.println("ERROR during exec: " + e);  // debug
    }
    return result;
  }

  public static int exec_command (String[] cmd, boolean print) {
    int result = -1;
    try {
      Runtime rt = Runtime.getRuntime();
      Process p = rt.exec(cmd);
      try {
	if (print) print_process(p);
      } catch (Exception e) {
	System.err.println("ERROR: " + e);  // debug
      }
      p.waitFor();
      result = p.exitValue();
    } catch (Exception e) {
      System.err.println("ERROR during exec: " + e);  // debug
    }
    return result;
  }

  private static void print_process (Process p) throws IOException {
    InputStream in = p.getInputStream();
    BufferedInputStream buf = new BufferedInputStream(in);
    InputStreamReader inread = new InputStreamReader(buf);
    BufferedReader bufferedreader = new BufferedReader(inread);
    String line;
    while ((line = bufferedreader.readLine()) != null) {
      System.err.println(line);
    }
  }

}