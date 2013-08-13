package Ace2;
//
// high-level wrapper for alignment data classes, to aid abstraction
// between .ace and SAM formats
//
// MNE 7/2009

import java.util.*;

import net.sf.samtools.*;

public interface AssemblySequence {
  // make this ABSTRACT rather than an interface?
  // - have simple fields in main class, rather than using subroutine calls?
  //   NO: for backwards compatibility, Ace version uses 2 classes (Ace/AlignmentSequence)
  //   might be something to look into though (bigger rewrite)

  public String get_name();

  public int get_asm_start();
  public int get_asm_end();
  public int get_clip_start();
  public int get_clip_end();
  // all these are padded/consensus positions
  // FIX ME: REPLACE WITH PUBLIC VARIABLES IN ABSTRACT CLASS???

  public boolean is_complemented();

  public void get_visible_sequence(char[] buf, int cpos);
  // populate buffer
  // FIX ME: PUT IN BASE CLASS!  All we need is underlying padded array...

  public int get_quality (int cpos);
  // get quality score at a particular consensus position
  
  public char get_base(int cpos);
  // get nucleotide at a particular consensus position

  // public boolean is_visible (int csv, int cev);
  // TO DO! (alternative to get_asm_start/end?)

  public SAMRecord get_samrecord();

  public int get_padded_index (int cpos);


}
