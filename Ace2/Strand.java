package Ace2;

import net.sf.samtools.*;

public enum Strand {
  STRAND_POSITIVE, STRAND_NEGATIVE;

  public static Strand valueOfSAMRecord (SAMRecord sr) {
    return sr.getReadNegativeStrandFlag() ? STRAND_NEGATIVE : STRAND_POSITIVE;
  }

}