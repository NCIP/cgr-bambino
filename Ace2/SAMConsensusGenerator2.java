package Ace2;
//
// generate consensus sequence from a BAM assembly
// MNE 2/2013
//
// revised/standalone class, not dependent on wider application
//

import net.sf.samtools.*;
import java.io.*;

public class SAMConsensusGenerator2 {
  SAMFileReader sfr;

  public SAMConsensusGenerator2 (SAMFileReader sfr) {
    this.sfr = sfr;
  }

  public byte[] generate_consensus (String ref_name) {
    byte[] results = null;

    SAMFileHeader h = sfr.getFileHeader();
    SAMSequenceRecord ssr = h.getSequence(ref_name);
    if (ssr == null) {
      System.err.println("ERROR: no header record for " + ref_name);  // debug
    } else {
      System.err.println("yay");  // debug
    }
    return results;
  }

  public static void main (String[] argv) {
    String fn = "reads50.bam";
    SAMFileReader sfr = new SAMFileReader(new File(fn));
    SAMConsensusGenerator2 scg = new SAMConsensusGenerator2(sfr);
    scg.generate_consensus("CY097774");
  }

}
