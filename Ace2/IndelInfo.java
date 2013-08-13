package Ace2;

import net.sf.samtools.*;

public class IndelInfo {
  CigarOperator indel_type;
  int reference_i, length;
  SAMRecord sr;
  // can't remember if there's a good reason we don't just set this in the constructor.
  // leave it alone (null) for now.

  String sequence;
  byte tumor_normal, quality;
  int minimum_flanking_sequence;
  
  public IndelInfo (CigarOperator indel_type, int reference_i, int length, SAMRecord sr, int read_i) {
    this.indel_type=indel_type;
    this.reference_i=reference_i;
    this.length=length;
    sequence = null;
    set_minimum_flanking_sequence(sr, read_i);
    set_quality(sr, read_i);
  }

  public String getTypeHashString() {
    // basic hash key of just indel type and length
    return indel_type.toString() + length;
  }

  private void set_quality (SAMRecord sr, int read_i) {
    int start = -1;
    int end = -1;
    if (indel_type.equals(CigarOperator.DELETION)) {
      start = read_i - 1;
      end = read_i;
    } else if (indel_type.equals(CigarOperator.INSERTION)) {
      start = read_i;
      end = read_i + (length - 1);
    }
    if (start == -1 || end == -1) {
      quality = -1;
    } else {
      int count = 0;
      int total = 0;
      if (sr == null) System.err.println("WTF?");  // debug
      byte[] qual = sr.getBaseQualities();
      for (int i = start; i <= end; i++, count++) {
	//	System.err.println("q = " + qual[i]);  // debug
	total += qual[i];
      }
      quality = (byte) (total / count);
    }
    //    System.err.println("op=" + indel_type + " s=" + start + " e=" + end + " sz=" + length +  " quality="+quality);  // debug

  }

  private void set_minimum_flanking_sequence(SAMRecord sr, int read_i) {
    int flank_l = read_i;
    int flank_r = sr.getReadLength() - (read_i + (indel_type.equals(CigarOperator.INSERTION) ? length : 1));
    // for insertions, count right-flanking sequence after inserted bases
    // for deletions, count immediately after event

    minimum_flanking_sequence = flank_l < flank_r ? flank_l : flank_r;
  }

}
