// A single sequence in an assembly.
//
// Created from Ace objects as sequences are loaded from the .ace file.
// Supplementary related datafiles may be loaded directly via methods
// from this class.

// MNE July 1997

package Ace2;
import java.io.*;
import java.util.*;

public class AceSequence implements Cloneable {
  // a single sequence in a contig.
  String name;
  // ID for the sequence (or consensus)
  public String sequence;
  // bases of the sequence [padded]
  public boolean is_contig, complemented;

  public int asm_start_padded, asm_end_padded;
  // consensus positions

  int clip_start_padded, clip_end_padded;
  // these are sequence offsets, NOT consensus positions
  
  String description;

  AceSequence (String s) {
    name = s;
    complemented = false;
  }

  public void reverse_complement () {
    // reverse-complement the sequence and status flag only.
    // RC-ing offsets requires external data.
    StringBuffer seq = new StringBuffer(sequence);
    StringBuffer rc = seq.reverse();
    complemented = !complemented;

    int len = rc.length();
    char c;
    for (int i=0; i < len; i++) {
      switch (rc.charAt(i)) {
      case 'a':
	c = 't'; break;
      case 'A':
	c = 'T'; break;
      case 'c':
	c = 'g'; break;
      case 'C':
	c = 'G'; break;
      case 'g':
	c = 'c'; break;
      case 'G':
	c = 'C'; break;
      case 't':
	c = 'a'; break;
      case 'T':
	c = 'A'; break;
      case '*':
	c = '*'; break;
      case 'n':
	c = 'n'; break;
      case 'N':
	c = 'N'; break;
      case 'x':
	c = 'x'; break;
      case 'X':
	c = 'X'; break;
      default:
	c = rc.charAt(i);
	System.out.println("reverse: warning, don't know how to rc " + c);
	break;
      }
      rc.setCharAt(i, c);
    }
    this.sequence = rc.toString();
  }

  public StringBuffer unpadded_sequence () {
    StringBuffer result = new StringBuffer();
    int length = sequence.length();
    char c;
    for (int i=0; i < length; i++) {
      c = sequence.charAt(i);
      if (c != '*') result.append(c);
    }
    return result;
  }

  public void get_visible_sequence (char [] buf, int csv) {
    // fill given buffer with visible sequence starting at given
    // consensus position

    int chunk_size = buf.length;
    // visible area of assembly

    int i;
    for (i=0; i < chunk_size; i++) {
      // possible optimization: only do this for leading/trailing seq
      buf[i] = ' ';
    }

    // leading (empty) region
    int leading_pads = asm_start_padded - csv;
    if (leading_pads < 0) leading_pads = 0;

    // indices in raw aligned string (not consensus):
    int start_index = csv - asm_start_padded;
    if (start_index < 0) start_index = 0;
    int end_index = start_index + (chunk_size - leading_pads);

    //    int max = sequence.length() - 1;
    int max = sequence.length();
    if (end_index > max) end_index = max;

    //    System.out.println(name + " lp:" + leading_pads + " si:" + start_index + " ei:" + end_index + " len:" + sequence.length());  // debug

    sequence.getChars(start_index, end_index, buf, leading_pads);
    // copy visible region of sequence into aligned buffer

    //    System.out.println(name + " " + buf);  // debug
  }

  public char get_base (int consensus_pos) {
    // return base aligned at specified consensus position.
    int i = consensus_to_index(consensus_pos);
    if (i > -1) {
      return sequence.charAt(i);
    } else {
      return ' ';
    }
  }

  public int consensus_to_index (int consensus_pos) {
    // given consensus offset, return string index to raw data, or -1
    int i = consensus_pos - asm_start_padded;
    if (i < 0 || i >= sequence.length()) {
      return -1;
    } else {
      return i;
    }
  }
}
