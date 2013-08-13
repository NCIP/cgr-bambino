package Ace2;

import net.sf.samtools.*;

public class SAMReadQuery {
  String readName;
  int alignmentStart;
  boolean negativeStrandFlag;
  // mimic names from SAMRecord

  SAMRecord hit;

  public SAMReadQuery (String readName,
		       int alignmentStart,
		       boolean negativeStrandFlag
		       ) {
    this.readName = readName;
    this.alignmentStart = alignmentStart;
    this.negativeStrandFlag = negativeStrandFlag;
    hit = null;
  }

  public SAMReadQuery (SAMRecord sr, boolean use_mate) {
    readName = sr.getReadName();
    if (use_mate) {
      alignmentStart = sr.getMateAlignmentStart();
      negativeStrandFlag = sr.getMateNegativeStrandFlag();
    } else {
      alignmentStart = sr.getAlignmentStart();
      negativeStrandFlag = sr.getReadNegativeStrandFlag();
    }
  }

  public boolean matches (SAMRecord sr) {
    return (
	    negativeStrandFlag == sr.getReadNegativeStrandFlag() &&
	    alignmentStart == sr.getAlignmentStart() &&
	    readName.equals(sr.getReadName())
	    );
  }

}
