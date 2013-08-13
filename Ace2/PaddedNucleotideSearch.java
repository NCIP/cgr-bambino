package Ace2;

import java.util.*;

public class PaddedNucleotideSearch {
  // search padded (eg consensus/phrap) sequence for a string of
  // nucleotides.
  private String source, stripped;
  private Hashtable hits;

  PaddedNucleotideSearch (String str) {
    source = str.toLowerCase();
    stripped = Funk.Str.unpadded(source);
  }
  
  public Enumeration find (String str) {
    str = str.toLowerCase();
    //    System.out.println(stripped);  // debug
    hits = new Hashtable();
    find_it(str);
    find_it(Funk.Str.reverse_complement(str));
    return hits.keys();
  }

  public Enumeration get_matches () {
    return hits.keys();
  }

  public int get_end (int start) {
    Integer key = new Integer(start);
    if (hits.containsKey(key)) {
      return ((Integer) hits.get(key)).intValue();
    } else {
      return 0;
    }
  }

  private void find_it (String str) {
    int index = 0;
    int i2;
    while ((i2 = stripped.indexOf(str, index)) > -1) {
      //      System.out.println("hit at " + i2);  // debug
      unpadded_to_padded(str, i2);
      index = i2 + 1;
    }
  }

  private void unpadded_to_padded(String str, int offset) {
    // translate hit in unpadded sequence to padded sequence
    int i = 0;
    int len = source.length();
    char c;
    int need = offset;
    int start, end, left;
    start=end=left=0;
    for (i=0; i < len; i++) {
      c = source.charAt(i);
      if (c != '*') {
	if (--need <= 0) {
	  if (need == -1) {
	    // first
	    start = i;
	    left = str.length() - 1;
	  } else {
	    if (--left == 0) {
	      // done
	      end = i;
	      break;
	    }
	  }
	}
      }
    }

    hits.put(new Integer(start), new Integer(end));

    //    System.out.println("start:" + start + " end:" + end);  // debug
  }

  public static void main (String [] argv) {
    PaddedNucleotideSearch pnc = new PaddedNucleotideSearch("AAGGA*ATT**CGGC*ACG*AGG*CCT*CGTCGACG*CCG*GACGAAAGACACGG*GCC*TGATTCGTCGAGTCT*C*ACTGAGCCTTA*GTC*GTCGGCAG*GTCCCAGGCG*C*GAAGTT*TCTCGGC*CT*GGA*GGAGGGGGTCGCGC*GAAGTCCAGATGCAG*GCGGGGAAGCCATC**CTCTATTCCTA*TTTCCGAAGCTCCTGCTCATGGA*GAGTTCGAATTGC*TCTGGCCTTGA*AAGGCATCGAC*TACGAGAC");
    //    pnc.find("GCAGGCG");
    pnc.find("CGCCTGC");
  }

}  



