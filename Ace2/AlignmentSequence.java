package Ace2;
import java.util.*;

import net.sf.samtools.*;
// urgh

public class AlignmentSequence implements AssemblySequence {
  // Represents a single sequence in the alignment.
  // Methods can dynamically add optional data such as trace
  // positions and polymorphism data.
  //  char [] sequence = null;
  private int[] quality = null;
  //
  // Deep Thoughts: might it make sense to have a single array
  // of some structure that contains sequence, quality, phd, poly data?
  //
  //  Pros: integration makes lookups more obvious, easy to extend
  //  Cons: less efficient [?], less sensible for modules to populate, 
  //        less clear interface to external data (ie unpadded, etc)

  private int cs;  // clip start, in consensus space
  private int ce;  // clip end, in consensus space

  public AceSequence ace_seq;

  AlignmentSequence (AceSequence ace_seq) {
    // TO DO: can we define this to extend/copy a given AceSequence?
    this.ace_seq = ace_seq;
    //    as = Math.abs(leftmost - ace_seq.asm_start_padded);
//     ae = Math.abs(leftmost - ace_seq.asm_end_padded);
    //    cs = as + ace_seq.clip_start_padded - 1;
    //    ce = as + ace_seq.clip_end_padded - 1;
//     tcs = cs + leftmost;
//     tce = ce + leftmost;

    cs = ace_seq.asm_start_padded + ace_seq.clip_start_padded - 1;
    ce = ace_seq.asm_start_padded + ace_seq.clip_end_padded - 1;

//     sequence = new char[alignment.assembly_width];
//     int i;
//     for (i=0; i < as; i++) {
//       sequence[i] = ' ';
//     }
//     char [] s = ace_seq.sequence.toString().toCharArray();
//     int length = s.length;
//     System.arraycopy(s, 0, sequence, as, length);
//     for (i = as + length; i < alignment.assembly_width; i++) {
//       sequence[i] = ' ';
//     }
  }
  
  public String get_id () {
    // get identifier for this sequence
    return(ace_seq.name);
  }

  public char get_base (int consensus_pos) {
    // return base from sequence at the specified consensus position.
    return ace_seq.get_base(consensus_pos);
  }

  public int get_quality (int consensus_pos) {
    // return quality score for this sequence at this consensus offset.
    int i = ace_seq.consensus_to_index(consensus_pos);
    if (quality != null && i > -1) {
      return quality[i];
    } else {
      return -1;
    }
  }

  public boolean complemented () {
    return ace_seq.complemented;
  }

  boolean sanity_check (char base1, char base2, int asm_pos, String type) {
    boolean broken = true;
    if (base1 == base2 ||
	base1 == 'x' || base1 == 'X' ||
	base2 == 'x' || base2 == 'X') {
      // bases match or one of the two is vector-screened
      broken = false;
    } else {
      switch (base1) {
      case 'a': case 'A':
	if (base2 == 'a' || base2 == 'A') broken = false; break;
      case 'c': case 'C':
	if (base2 == 'c' || base2 == 'C') broken = false; break;
      case 'g': case 'G':
	if (base2 == 'g' || base2 == 'G') broken = false; break;
      case 't': case 'T':
	if (base2 == 't' || base2 == 'T') broken = false; break;
      }
    }

    if (broken) {
      // data is out of sync with .ace file data!
      System.out.println(type + " data for " + ace_seq.name + " is out of sync at " + asm_pos + ". base1:" + base1 + " base2:" + base2);
    }

    return broken;
  }
  
  public boolean is_visible (int offset, boolean hq) {
    // whether or not sequence is aligned at this consensus offset.
    if (hq) {
      // only if untrimmed (high-quality) sequence is aligned here.
      return offset >= cs && offset <= ce;
    } else {
      // if aligned at all
      return offset >= ace_seq.asm_start_padded && offset <= ace_seq.asm_end_padded;
    }
  }

  public void map_quality (ArrayList<Integer> quals_raw) {
    // map quality data to alignment
    ArrayList<Integer> quals = quals_raw;

    if (complemented()) {
      // hack: aligned sequence is reverse-complemented, create a reversed copy
      quals = new ArrayList<Integer>(quals_raw);
      Collections.reverse(quals);
    }
    
    //
    //  map the data into the alignment
    //

    // init array
    String aceseq = ace_seq.sequence.toString();
    int acelen = aceseq.length();
    quality = new int[acelen];

    int i_asm, i_qual;
    int q;

    // map
    int qs = quals.size();
    //    System.err.println(ace_seq.name + " alen="+acelen + " qs:"+qs);  // debug
    for (i_asm=0, i_qual=0; i_qual < qs; i_asm++) {
      //      System.err.println("i:"+i_asm + " " + aceseq.charAt(i_asm));  // debug

      while (aceseq.charAt(i_asm) == '*') {
	// move past alignment pads
	quality[i_asm++] = -1;
      }
      quality[i_asm] = quals.get(i_qual++);
    }
  }

  // begin AssemblySequence interface
  public String get_name() {
    return ace_seq.name;
  }

  public int get_asm_start() {
    return ace_seq.asm_start_padded;
  }

  public int get_asm_end() {
    return ace_seq.asm_end_padded;
  }

  public int get_clip_start() {
    return cs;
  }

  public int get_clip_end() {
    return ce;
  }

  public boolean is_complemented() {
    return ace_seq.complemented;
  }

  public void get_visible_sequence(char[] buf, int cpos) {
    ace_seq.get_visible_sequence(buf, cpos);
  }
  // end AssemblySequence interface

  public SAMRecord get_samrecord() {
    return null;
  };

  public int get_padded_index (int cpos) {
    return cpos - get_asm_start();
  }

}
