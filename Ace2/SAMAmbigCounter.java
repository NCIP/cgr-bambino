package Ace2;

import java.io.*;
import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.*;

public class SAMAmbigCounter {
  static int REPORT_INTERVAL = 100000;
  int total_nt = 0;
  int total_n = 0;
  int total_other = 0;
  int read_count=0;
  int read_count_ambig_tags = 0;
  int reads_with_n = 0;
  int h0=0;
  int h1=0;
  int h2=0;
  int not_primary=0;

  public void count (SAMFileReader sfr) {
    int i;
    total_nt = 0;
    total_n = 0;
    total_other = 0;
    read_count=0;
    read_count_ambig_tags = 0;
    reads_with_n = 0;
    h0=0;
    h1=0;
    h2=0;
    not_primary=0;

    for (SAMRecord sr : sfr) {
      if (++read_count % REPORT_INTERVAL == 0) report();

      byte[] read = sr.getReadBases();
      boolean has_n = false;
      for (i=0; i < read.length; i++) {
	total_nt++;
	switch (read[i]) {
	case 'a': case 'c': case 'g': case 't':
	case 'A': case 'C': case 'G': case 'T':
	  break;
	case 'n': case 'N':
	  total_n++;
	  has_n = true;
	  break;
	default:
	  System.err.println("other: " + (char) read[i]);  // debug
	  total_other++;
	  break;
	}
      }
      if (has_n) reads_with_n++;

      if (sr.getNotPrimaryAlignmentFlag()) not_primary++;

      for (SAMTagAndValue tav : sr.getAttributes()) {
	if (tav.tag.equals("SQ") || tav.tag.equals("E2") || tav.tag.equals("U2")) {
	  //	  System.err.println("hey now " + tav.tag + " => " + tav.value);  // debug
	  read_count_ambig_tags++;
	  break;
	} else if (tav.tag.equals("H0")) {
	  h0++;
	} else if (tav.tag.equals("H1")) {
	  h1++;
	} else if (tav.tag.equals("H2")) {
	  h2++;
	}
      }
    }

    System.err.println("done");  // debug
    report();
  }

  private void report() {
    System.err.println("            reads: " + read_count);  // debug
    System.err.println("       reads w/Ns: " + reads_with_n);  // debug
    System.err.println("non-primary reads: " + not_primary);  // debug

    System.err.println(" reads w/SQ|E2|U2: " + read_count_ambig_tags);  // debug
    System.err.println("       reads w/H0: " + h0);
    System.err.println("       reads w/H1: " + h1);
    System.err.println("       reads w/H2: " + h2);

    System.err.println("");  // debug

    System.err.println("   total nt: " + total_nt);  // debug
    // int rollover likely...
    System.err.println("    total n: " + total_n);  // debug
    System.err.println("total other: " + total_other);  // debug
  }

  
  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SAMAmbigCounter counter = new SAMAmbigCounter();
    if (argv.length == 0) {
      System.err.println("specify sam/bam file");  // debug
    } else {
      counter.count(new SAMFileReader(new File(argv[0])));
    }
  }

}