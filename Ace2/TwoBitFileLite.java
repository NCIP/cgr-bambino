package Ace2;
//
// memory-saving "lite" version of TwoBitFile:
//  - never cache TwoBitSequence records
//  - when checking length, use special limited instantiation of TwoBitSeqence
//    which skips block-loading portion

import java.io.*;
import java.util.*;

public class TwoBitFileLite implements ReferenceSequence {
  private TwoBitHeader tbh;

  public TwoBitFileLite (String filename) throws FileNotFoundException,IOException {
    tbh = new TwoBitHeader(filename);
    tbh.set_caching(false);
  }

  public byte[] get_region (String sequence_name, int start_base, int length) throws IOException {
    TwoBitSequence tbs = tbh.get_sequence(sequence_name);
    byte[] result = null;
    if (tbs == null) {
      //      System.err.println("WARNING: .2bit file doesn't contain a sequence named " + sequence_name);  // debug
    } else {
      result = tbs.get_region(start_base, length);
    }
    return result;
  }
  
  public byte[] get_all (String sequence_name) throws IOException {
    TwoBitSequence tbs = tbh.get_sequence(sequence_name);

//     System.err.println("BEFORE 2...");  // debug
// try {
// System.out.println("killing time...");
// Thread.sleep(1000 * 60);
// } catch (InterruptedException e) {}

    byte[] result = null;
    if (tbs == null) {
      //      System.err.println("WARNING: .2bit file doesn't contain a sequence named " + sequence_name);  // debug
    } else {
      result = tbs.get_full_sequence();
    }
    return result;
  }

  public int get_length (String sequence_name) throws IOException {
    TwoBitSequence tbs = tbh.get_sequence(sequence_name, false);
    int length = ReferenceSequence.NULL_LENGTH;
    if (tbs == null) {
      //     System.err.println("WARNING: .2bit file doesn't contain a sequence named " + sequence_name);  // debug
    } else {
      length = tbs.get_length();
    }
    return length;
  }

  public static void main (String[] argv) {
    String fn = "c:/generatable/hg18/hg18.2bit";
    try {
      TwoBitFileLite tbf = new TwoBitFileLite(fn);
      //      TwoBitFile tbf = new TwoBitFile(fn);
      if (argv.length == 3) {
	String chr_name = argv[0];
	int start = Integer.parseInt(argv[1]);
	int len = Integer.parseInt(argv[2]);

	byte[] seq = tbf.get_region(chr_name, start, len);
	System.err.println("sequence: " + new String(seq));  // debug
      } else if (true) {
	byte[] chr = tbf.get_all("chr2");
	System.err.println("chr length = " + chr.length);  // debug

	while (true) {
	  System.gc();
	  try {
	    System.err.println("sleeping...");  // debug
	    Thread.sleep(1000 * 10);
	  } catch (InterruptedException e) {}
	}
      } else {
	// chr lengths only
	for (int i = 1; i <= 22; i++) {
	  String key = "chr" + i;
	  int len = tbf.get_length(key);
	  System.err.println(key + ": " + len);  // debug
	}
	try {
	  System.out.println("killing time...");
	  Thread.sleep(1000 * 180);
	} catch (InterruptedException e) {}

      }

    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public boolean supports_sequence_list() {
    return true;
  }

  public ArrayList<String> get_sequence_names() {
    return tbh.get_sequence_names();
  }

}
