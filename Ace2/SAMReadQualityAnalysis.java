package Ace2;

import java.io.*;
import java.util.*;
import net.sf.samtools.*;

public class SAMReadQualityAnalysis {
  private ArrayList<File> bam_files;
  private int MAX_ANALYZE_READS_PER_FILE = 1000000;

  public SAMReadQualityAnalysis() {
    bam_files = new ArrayList<File>();
  }

  public void add_file (File f) {
    bam_files.add(f);
  }

  public void analyze() {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    for (File f : bam_files) {
      SAMFileReader sfr = new SAMFileReader(f);
      int processed = 0;
      for (SAMRecord sr : sfr) {
	if (sr.getDuplicateReadFlag() || sr.getReadUnmappedFlag()) continue;

	byte[] quals = sr.getBaseQualities();
	if (quals != null && quals.length > 0) {
	  //
	  // usable read with populated base qualities
	  //

	  if (processed++ >= MAX_ANALYZE_READS_PER_FILE) break;

	  //	for (AlignmentBlock ab : sr.getAlignmentBlocks()) {
	  //	}


	  System.err.println("hey now");  // debug
	}
	
      }

    }
    
  }

  public static void main (String[] argv) {
    SAMReadQualityAnalysis qa = new SAMReadQualityAnalysis();
    qa.add_file(new File("tcga-10-0931-11a-01w-0420-08.bam_mini.bam"));
    qa.analyze();
  }

}
