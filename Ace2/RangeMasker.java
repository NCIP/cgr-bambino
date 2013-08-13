package Ace2;

import java.util.*;

public class RangeMasker {

  private ArrayList<Range> ranges_to_mask;
  private byte[] raw, masked;
  private int offset, masked_bases;
  public static final byte TRIM_CHAR = '#';

  public RangeMasker (ArrayList<Range> ranges_to_mask, byte[] raw) {
    this.ranges_to_mask = ranges_to_mask;
    this.raw = raw;
    masked = null;
    offset = masked_bases = 0;
  }

  public void set_offset (int i) {
    // offset in raw base array aligned to coordinates
    offset = i;
  }

  public void mask (int start, int end) {
    int i;
    for (Range r : ranges_to_mask) {
      if (!(r.end < start || r.start > end)) {
	// overlap
	//	System.err.println("overlap with range " + r);  // debug
	if (masked == null) masked = raw.clone();
	int tend = r.end + offset;
	for (i = r.start + offset; i <= tend; i++) {
	  if (i < 0) {
	    // should never happen
	    System.err.println("ERROR: mask i=" + i);  // debug
	    return;
	  }
	  masked[i] = TRIM_CHAR;
	  masked_bases++;
	}
      }
    }
  }

  public void mask () {
    mask(0, raw.length);
  }

  public byte[] get_masked_sequence() {
    return masked;
  }

  public byte[] get_trimmed_sequence() {
    byte[] result = null;
    if (masked != null) {
      result = new byte[masked.length - masked_bases];
      int ti = 0;
      for (int i=0; i < masked.length; i++) {
	if (masked[i] != TRIM_CHAR) result[ti++] = masked[i];
      }
    }
    return result;
  }

  public byte[] trim_byte_array (byte[] input) {
    // trim an additional array using established pattern
    byte[] result = null;
    if (input.length == raw.length) {
      result = new byte[raw.length - masked_bases];
      int ti = 0;
      for (int i=0; i < masked.length; i++) {
	if (masked[i] != TRIM_CHAR) result[ti++] = input[i];
      }
    } else {
      System.err.println("ERROR: can't trim, array length mismatch");  // debug
    }
    return result;
  }

  public int get_masked_base_count() {
    return masked_bases;
  }


}