package Ace2;

import java.io.*;
import java.util.*;

import net.sf.samtools.*;

public class SAMReadCounter {

  public SAMReadCounter() {
  }

  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    try {
      boolean tab_mode = false;
      String bam_file = null;

      for (int i=0; i < argv.length; i++) {
	if (argv[i].equals("-bam")) {
	  bam_file = new String(argv[++i]);
	} else if (argv[i].equals("-tab")) {
	  tab_mode = true;
	} else {
	  System.err.println("ERROR: unknown arg " + argv[i]);  // debug
	  System.exit(1);
	}
      }

      if (bam_file == null) {
	System.err.println("specify -bam [file]");
      } else {
	SAMFileReader sfr = new SAMFileReader(new File(bam_file));

	long mapped_reads = 0;
	long unmapped_reads = 0;
	long duplicate_reads = 0;
	long total_reads = 0;

	int REPORT_INTERVAL = 1000000;

	for (SAMRecord sr : sfr) {
	  if (++total_reads % REPORT_INTERVAL == 0) {
	    System.err.println("total:" + total_reads + " ref=" + sr.getReferenceName());  // debug
	  }
	  
	  if (sr.getDuplicateReadFlag()) duplicate_reads++;
	  if (sr.getReadUnmappedFlag()) {
	    unmapped_reads++;
	  } else {
	    mapped_reads++;
	  }
	}

	if (tab_mode) {
	  ArrayList<String> list = new ArrayList<String>();
	  list.add(bam_file);
	  list.add(Long.toString(total_reads));
	  list.add(Long.toString(mapped_reads));
	  list.add(Long.toString(duplicate_reads));
	  list.add(Long.toString(unmapped_reads));
	  System.out.println(Funk.Str.join("\t", list));
	} else {
	  System.err.println("total reads: " + total_reads);  // debug
	  System.err.println("     mapped: " + mapped_reads);  // debug
	  System.err.println("  duplicate: " + duplicate_reads);  // debug
	  System.err.println("   unmapped: " + unmapped_reads);  // debug
	}

      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }
      
}