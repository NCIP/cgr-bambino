package Ace2;
// determine which reference sequences in SAM header correspond
// to canonical references (i.e. chromosomes)
// - build index of flags indicating usability
// - when parsing SAMRecords, use header indices rather than names
//   (presumably faster)

import net.sf.samtools.*;

import java.util.*;

public class SAMCanonicalReference {
  private ChromosomeDisambiguator cd;
  private boolean[] usable;

  public SAMCanonicalReference (SAMFileReader sfr) {
    setup(sfr);
  }

  public void setup(SAMFileReader sfr) {
    cd = new ChromosomeDisambiguator(sfr);
    SAMSequenceDictionary dict = sfr.getFileHeader().getSequenceDictionary();
    List<SAMSequenceRecord> seqs = dict.getSequences();
    usable = new boolean[seqs.size()];
    for (SAMSequenceRecord ssr : seqs) {
      String name = ssr.getSequenceName();
      Chromosome ref = Chromosome.valueOfString(name);
      boolean ok = ref != null;
      //      System.err.println("name="+name + " idx=" + ssr.getSequenceIndex() + " usable="+ok);  // debug
      usable[ssr.getSequenceIndex()] = ok;
    }
  }

  public boolean is_canonical (int index) {
    // whether a SAM header reference index corresponds to a canonical
    // genome sequence
    return usable[index];
  }

}
