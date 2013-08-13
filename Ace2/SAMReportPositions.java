package Ace2;

import net.sf.samtools.*;
import java.io.*;
import java.util.*;

public class SAMReportPositions {

  public void report (SAMFileReader sfr, String tname) {
    String rn;
    HashSet<String> skipped = new HashSet<String>();
    for (SAMRecord sr : sfr) {
      if (sr.getReadUnmappedFlag()) continue;
      rn = sr.getReferenceName();
      if (rn.equals(tname)) {
	System.err.println(
			   (sr.getReadNegativeStrandFlag() ? "R" : "F") + " " +
			   sr.getReadName() + " " +
			   rn + " " +
			   sr.getAlignmentStart() + " " + 
			   sr.getAlignmentEnd() + " " + 
			   new String(sr.getReadBases())
			   );
      } else if (!skipped.contains(rn)) {
	System.err.println("skipping " + rn);  // debug
	skipped.add(rn);
      }
    }
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    try {
      SAMReportPositions rpt = new SAMReportPositions();
      rpt.report(new SAMFileReader(new File(argv[0])), argv[1]);
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }


}