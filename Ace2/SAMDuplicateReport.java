package Ace2;

import net.sf.samtools.*;
import java.io.*;

public class SAMDuplicateReport {

  public void report (SAMFileReader sfr) {
    for (SAMRecord sr : sfr) {
      System.err.println(sr.getReadName() +
			 (sr.getReadNegativeStrandFlag() ? ".R" : ".F") + 
			 " unmapped:" + sr.getReadUnmappedFlag() + 
			 
			 " duplicate:" + sr.getDuplicateReadFlag());  // debug
    }
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    try {
      SAMDuplicateReport rpt = new SAMDuplicateReport();
      rpt.report(new SAMFileReader(new File(argv[0])));
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }


}