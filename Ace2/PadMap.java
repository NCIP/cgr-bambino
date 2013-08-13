package Ace2;

import java.util.Arrays;
import static Ace2.Constants.ALIGNMENT_GAP_CHAR;
import static Ace2.Constants.ALIGNMENT_DELETION_CHAR;

public class PadMap {
  //
  //  map converting padded base number to unpadded base number
  //  NOT a string index: i.e. consensus position 1 refers to base #1, not string index 0
  //
  private char[] padded;
  private int[] pad2unpad;
  // 1-based, NOT 0-based -- i.e. 1st entry is at pad2unpad[1]
  private int[] unpad2pad;
  private int last_upcpos;

  private static final boolean DEBUG = false;

  public static final int UNDEF = -999999;
  
  public PadMap (String padded) {
    this.padded=padded.toCharArray();
    do_map();
  }

  public PadMap (char[] padded) {
    this.padded=padded;
    do_map();
  }

  private void do_map() {
    int pi=0;
    // padded sequence index, 0-based
    int pcpos = 1;
    // padded consensus position (1-based)
    int upcpos = 1;
    // unpadded consensus position (1-based)
    int len = padded.length;

    pad2unpad = new int[len + 1];
    // map of padded consensus position -> unpadded consensus position

    unpad2pad = new int[len + 1];
    // map of unpadded consensus position -> padded
    Arrays.fill(unpad2pad, UNDEF);

    char c;
    for (pi=0, pcpos=1, upcpos=0; pi < len; pi++, pcpos++) {
      c = padded[pi];
      if (c != ALIGNMENT_GAP_CHAR && c != ALIGNMENT_DELETION_CHAR) upcpos++;
      pad2unpad[pcpos] = upcpos;
      if (unpad2pad[upcpos] == UNDEF) unpad2pad[upcpos] = pcpos;
      if (DEBUG) System.err.println(c + " " + pcpos + " => " + upcpos);  // debug
    }
    last_upcpos = upcpos;
    //    System.err.println("last upcpos="+upcpos);  // debug


  }

  public char[] get_padded_sequence () {
    return padded;
  }

  public int get_padded_to_unpadded (int padded) {
    // for a padded offset, return the unpadded base number (1-based)
    int result = -1; 
    try {
      result = pad2unpad[padded];
    } catch (ArrayIndexOutOfBoundsException e) {
      //      System.err.println("ERROR: illegal get_padded_to_unpadded call for " + padded);  // debug
    }
    return result;
  }

  public boolean in_unpadded_range (int unpadded) {
    return unpadded >= 0 && unpadded < unpad2pad.length;
  }

  public int get_unpadded_to_padded (int unpadded) {
    if (unpadded >= unpad2pad.length) {
      System.err.println("ERROR: get_unpadded_to_padded out of range!!");  // debug
      (new Exception()).printStackTrace();
      return 0;
    } else {
      return unpad2pad[unpadded];
    }
  }

  public int get_max_unpadded () {
    // max unpadded base # which can be translated
    //    return unpad2pad.length - 1;
    return last_upcpos;
  }
  
  public static void main (String[] argv) {
    String padded = "ac*gG***t";

    System.err.println("sequence: " + padded);  // debug
    System.err.println("");  // debug

    PadMap pm = new PadMap(padded);

    System.err.println("padded-to-unpadded:");  // debug
    System.err.println("  map 5:" + pm.get_padded_to_unpadded(5));  // debug
    System.err.println("  map 8:" + pm.get_padded_to_unpadded(8));  // debug
    System.err.println("  map 9:" + pm.get_padded_to_unpadded(9));  // debug
    System.err.println("");  // debug

    System.err.println("unpadded-to-padded:");  // debug
    System.err.println("  map 3:" + pm.get_unpadded_to_padded(3));  // debug
    System.err.println("  map 4:" + pm.get_unpadded_to_padded(4));  // debug
    System.err.println("  map 9:" + pm.get_unpadded_to_padded(9));  // debug
  }

}