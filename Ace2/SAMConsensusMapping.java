package Ace2;

import net.sf.samtools.*;
import java.io.*;
import java.util.*;
import java.net.*;

public class SAMConsensusMapping implements AssemblySequence {
  //
  // supplemental info for a SAMRecord: alignments adjusted for padded consensus sequence
  //
  byte[] padded_sequence;
  byte[] padded_quality;

  int asm_start_padded, asm_end_padded;
  int clip_start_padded, clip_end_padded;
  String suffix;
  // append to readName to make unique within assembly

  SAMRecord sr;

  public SAMConsensusMapping (SAMRecord sr) {
    padded_sequence = padded_quality = null;
    this.sr=sr;
  }

  public String get_name() {
    return sr.getReadName() + "." + suffix;
  }

  public int get_asm_start() {
    return asm_start_padded;
  }

  public int get_asm_end() {
    return asm_end_padded;
  }

  public int get_clip_start() {
    return clip_start_padded;
  }
  
  public int get_clip_end() {
    return clip_end_padded;
  };

  public boolean is_complemented() {
    return sr.getReadNegativeStrandFlag();
  }

  public byte[] get_sequence_buffer() {
    return padded_sequence == null ? sr.getReadBases() : padded_sequence;
  }

  public byte[] get_quality_buffer() {
    return padded_quality == null ? sr.getBaseQualities() : padded_quality;
  }

  public void get_visible_sequence (char [] buf, int csv) {
    // fill given buffer with visible sequence starting at given
    // consensus position

    int chunk_size = buf.length;
    // visible area of assembly

    byte[] seq_buf = get_sequence_buffer();

    Arrays.fill(buf, ' ');

    // leading (empty) region
    int leading_pads = asm_start_padded - csv;
    if (leading_pads < 0) leading_pads = 0;

    // indices in raw aligned string (not consensus):
    int start_index = csv - asm_start_padded;
    if (start_index < 0) start_index = 0;
    int end_index = start_index + (chunk_size - leading_pads);
    if (end_index > seq_buf.length) end_index = seq_buf.length;

    //    System.out.println(name + " lp:" + leading_pads + " si:" + start_index + " ei:" + end_index + " len:" + sequence.length());  // debug

    int si = start_index;
    int ti = leading_pads;
    for (si=start_index; si < end_index; si++) {
      // copy visible region of sequence into aligned buffer
      buf[ti++] = (char) seq_buf[si];
    }
  }

  public int get_quality (int cpos) {
    // get quality score at a particular consensus position
    int i = cpos - asm_start_padded;
    int qual;
    byte[] qual_buf = get_quality_buffer();
    //    if (i < 0 || i >= padded_sequence.length) {
    if (i < 0 || i >= qual_buf.length) {
      qual = -1;
    } else {
      qual = qual_buf[i];
    }
    return qual;
  }

  public char get_base (int cpos) {
    byte[] seq_buf = get_sequence_buffer();
    int i = cpos - asm_start_padded;
    char result;
    if (i < 0 || i >= seq_buf.length) {
      result = ' ';
    } else {
      result = (char) seq_buf[i];
    }
    return result;
  }

  public SAMRecord get_samrecord() {
    return sr;
  }

  public int get_padded_index (int cpos) {
    //    System.err.println("cs="+get_clip_start() + " as=" + get_asm_start());  // debug
    return cpos - get_asm_start();
  }

  public void apply_alignment_shift (int shift_bases) {
    //
    // experimental: used w/intron trimming
    //
    // need to deal with PadMap??  Probably not since the region being deleted
    // is purely skip characters
    //
    //    System.err.println("before: " + sr.getReadName() + " as=" + asm_start_padded + " ae=" + asm_end_padded + " cs=" + clip_start_padded + " ce=" + clip_end_padded + " shift:"+shift_bases);  // debug

    asm_start_padded += shift_bases;
    clip_start_padded += shift_bases;
    apply_end_shift(shift_bases);
  }

  public void apply_end_shift (int shift_bases) {
    //
    // experimental: used w/intron trimming
    //
    asm_end_padded += shift_bases;
    clip_end_padded += shift_bases;
  }


  public void set_sequence_buffer (byte[] buf) {
    // experimental: used w/intron trimming
    padded_sequence = buf;
  }

  public void set_quality_buffer(byte[] buf) {
    // experimental: used w/intron trimming
    padded_quality = buf;
  }


  
}