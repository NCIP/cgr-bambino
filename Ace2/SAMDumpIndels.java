package Ace2;

import net.sf.samtools.*;
import java.util.*;
import java.io.*;

import static Ace2.SAMMismatchFilter.MASK_CHAR;

public class SAMDumpIndels {
 
  private static int MIN_QUALITY = 15;

  public void report(SAMFileReader sfr) throws IOException {
    // unfinished
    int i;
    SAMIndelFilter sif = new SAMIndelFilter();

    for (SAMRecord sr : sfr) {
      if (sif.filter(sr)) {
	String name = sr.getReadName();
	for (IndelInfo ii : sif.get_indels()) {
	  System.out.println(sr.getReadName() + "," + ii.indel_type + "," + ii.length);  // debug
	}
	
      }
    }
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    try {
      SAMDumpIndels rpt = new SAMDumpIndels();
      rpt.report(new SAMFileReader(new File(argv[0])));
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }


}