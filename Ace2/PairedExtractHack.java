package Ace2;
// hack:
// from a BAM of SAMExtractUnmapped reads, extract mapped
// reads and mates (mapped or unmapped).  Strip SAMExtractUnmapped
// read ID suffix for pairing purposes.

// rename: ReadMateExtractorUnpaired

import java.io.*;
import java.util.*;
import net.sf.samtools.*;
import java.util.regex.*;

public class PairedExtractHack {
  File bam;

  public PairedExtractHack (File bam) {
    this.bam = bam;
  }

  public void extract() {
    SAMFileReader sfr = new SAMFileReader(bam);

    SAMFileHeader h = sfr.getFileHeader();
    SAMSequenceDictionary dict = h.getSequenceDictionary();

    String seu_suffix = "\\.[FR]\\d+";
    Pattern pattern = Pattern.compile(seu_suffix);

    for (SAMSequenceRecord ssr : dict.getSequences()) {
      // query each reference sequence in turn
      System.err.println("ref = " + ssr.getSequenceName());  // debug

      Iterator<SAMRecord> query = sfr.queryOverlapping(ssr.getSequenceName(), 1, ssr.getSequenceLength());

      while (query.hasNext()) {
	SAMRecord sr = query.next();
	String name = sr.getReadName();
	System.err.println(name);  // debug

	Matcher matcher = pattern.matcher(name);
	if (matcher.find()) {
	  // strip SAMExtractUnmapped read suffix
	  name = matcher.replaceAll("");
	  sr.setReadName(name);
	}


      }



    }


  }
  
  public static void main (String[] argv) {
    PairedExtractHack peh = new PairedExtractHack(new File("rob_seu.bam"));
    peh.extract();
  }

}
