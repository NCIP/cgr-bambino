package Ace2;

public class Constants {

  public static final char ALIGNMENT_GAP_CHAR = '*';
  // represents an insertion to the reference sequence

  public static final char ALIGNMENT_DELETION_CHAR = '-';
  // represents a deletion from the reference sequence

  public static final char ALIGNMENT_SKIPPED_CHAR_F = '>';
  public static final char ALIGNMENT_SKIPPED_CHAR_R = '<';
  // represents a skipped region in the reference sequence
  // (e.g. mRNA -> genomic mapping)

  public static final char ALIGNMENT_HARD_CLIPPED_CHAR = '_';
  
  public static final char ALIGNMENT_PADDING_CHAR = '%';
  // SAM CIGAR string, P (padding)

}