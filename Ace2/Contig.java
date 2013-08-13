package Ace2;
import java.util.*;

public class Contig {
  // a Contig consists of:
  //  - a collection of AceSequences, including one consensus sequence
  //  - snp scores
  AceSequence consensus;
  // the consensus sequence
  Hashtable members;
  String name;
  SNPList snps;
  Vector quality;
  // contig quality; note that this is NOT padded.
  
  private boolean flipped;
  private Hashtable flip_restore = null;
  private int leftmost, rightmost;
  
  private final static int POLY_COUNT = 13;
  private final static String REVERSE_POS_SIGNAL = "TTTATT";
  // "positioning element" (5' to 3') is AAUAAA
  // so if we see TTTATT after poly-T at 5' end, orientation 
  // should be flipped.

  Contig (String name) {
    this.name = name;
    consensus = new AceSequence(name);
    consensus.is_contig = true;
    consensus.asm_start_padded = 1;
    members = new Hashtable();
    quality = new Vector();
    flipped = false;
    snps = null;
  }

  AceSequence create_member (String id) {
    // create a new member in this contig with the specified identifier.
    AceSequence seq = new AceSequence(id);
    members.put(id, seq);
    return(seq);
  }

  AceSequence get_member(String id) {
    // return the requested member from this contig.
    return((AceSequence) members.get(id));
  }

  public int get_size () {
    return members.size();
  }

  public void flip () {
    // reverse-complement the contig.
    // *********** EXPERIMENTAL **************
    flipped = !flipped;

    int consensus_end = consensus.sequence.length() + 1;
    Enumeration e = members.elements();

    // reverse consensus quality:
    int half = quality.size() / 2;
    int end = quality.size() - 1;
    Object temp;
    int there;

    for (int i = 0; i < half; i++) {
      there = end - i;
      temp = quality.elementAt(there);
      quality.setElementAt(quality.elementAt(i), there);
      quality.setElementAt(temp, i);
    }
    // reverse consensus quality end

    if (flipped) flip_restore = new Hashtable();

    while (e.hasMoreElements()) {
      AceSequence seq = (AceSequence) e.nextElement();

      if (flipped) {
	// flipping original values
	AceSequence backup = new AceSequence(seq.name);
	int as = backup.asm_start_padded = seq.asm_start_padded;
	int ae = backup.asm_end_padded = seq.asm_end_padded;
	int cs = backup.clip_start_padded = seq.clip_start_padded;
	int ce = backup.clip_end_padded = seq.clip_end_padded;
	flip_restore.put(seq.name, backup);
	
	// assembly offsets are in consensus space:
	seq.asm_start_padded = consensus_end - ae;
	seq.asm_end_padded = consensus_end - as;

	// clipping offsets are in sequence space:
	int length = seq.sequence.length();
	seq.clip_start_padded = length - ce;
	seq.clip_end_padded = length - cs;
      } else {
	// restoring original values
	AceSequence backup = (AceSequence) flip_restore.get(seq.name);
	seq.asm_start_padded = backup.asm_start_padded;
	seq.asm_end_padded = backup.asm_end_padded;
	seq.clip_start_padded = backup.clip_start_padded;
	seq.clip_end_padded = backup.clip_end_padded;
      }
      
      seq.reverse_complement();
      // RC the raw sequence
    }

    consensus.reverse_complement();
    // RC the consensus sequence

    if (snps != null) {
      // RC the SNP positions
      e = snps.elements();
      while (e.hasMoreElements()) {
	SNP snp = (SNP) e.nextElement();
	snp.position = consensus_end - snp.position;
      }
    }
  }

  public boolean is_rc () {
    return flipped;
  }

  public boolean needs_flip () {
    // is this contig likely in the "wrong" (3' to 5') orientation?
    
    boolean needs = false;
    Enumeration e;

    if (false) {
      //
      // check for poly-a signal in the "wrong" (3' to 5') orientation.
      // Alas, this is easily spoofed by screened-out regions.
      // 
      int cutoff = consensus.sequence.length() / 5;
      if (cutoff > 500) cutoff = 500;

      if ((needs = flip_check(consensus)) == false) {
	// check consensus first
	e = members.elements();
	while (e.hasMoreElements()) {
	  AceSequence seq = (AceSequence) e.nextElement();
	  if (seq.complemented == false &&
	      seq.asm_start_padded < cutoff &&
	      (needs = flip_check(seq))) break;
	}
      }
    } else {
      //
      // simpler check:
      // if the longest genbank-named, >= 1kb sequence in the contig appears 
      // to be aligned in reverse-complement, assume contig is oriented in
      // reverse.
      //
      e = members.elements();
      int longest_len = 0;
      AceSequence longest_seq = null;
      while (e.hasMoreElements()) {
	AceSequence seq = (AceSequence) e.nextElement();
	if (Funk.Str.is_genbank_accession(seq.name)) {
	  int len = seq.unpadded_sequence().length();
	  if (longest_seq == null || len > longest_len) {
	    longest_len = len;
	    longest_seq = seq;
	  }
	}
      }
      
      needs = (longest_seq != null &&
	       longest_len > 1000 &&
	       longest_seq.complemented);
      // System.out.println("hey now: " + needs + " " + longest_len);
    }

    return needs;
  }

  private boolean flip_check (AceSequence seq) {
    boolean needs = false;
    char [] poly = new char[POLY_COUNT];

    String check = seq.unpadded_sequence().toString().toUpperCase();
    int cutoff = check.length() / 4;
    // only search the start of the sequence

    for (int i=0; i < 3; i++) {
      char c = (i == 0) ? 'T' : (i == 1) ? 'N' : 'X';
      // assembly may have been screened for repeats, etc, in which
      // case the poly-T may be show Ns or Xs instead.
      for (int j = 0; j < POLY_COUNT; j++) {
	poly[j] = c;
      }
      String pt = new String(poly);

      int index = -1;
      while (true) {
	index++;
	index = check.indexOf(pt, index);
	if (index == -1 || index > cutoff) break;
	int after = index + pt.length();
	int positioning = check.indexOf(REVERSE_POS_SIGNAL, after);
	int diff = positioning - after;
	if (diff >= 9 && diff <= 35) {
	  // signal should appear within 11-30 nucleotides
	  needs = true;
	  System.out.println("found " + REVERSE_POS_SIGNAL + " after " + pt + " at " + positioning + "; contig likely in 3' to 5' orientation");
	  break;
	}
      }
      if (needs) break;
    }
    return needs;
  }

  public Enumeration get_members () {
    return members.elements();
  }

  public Enumeration get_member_ids () {
    Vector v = new Vector();
    Enumeration e = members.elements();
    while (e.hasMoreElements()) {
      AceSequence as = (AceSequence) e.nextElement();
      v.addElement(as.name);
    }
    return v.elements();
  }

  public void find_bounds () {
    // find leftmost/rightmost offset this contig, fix lengths, etc
    Enumeration e = members.elements();
    leftmost = rightmost = 1;
    while (e.hasMoreElements()) {
      AceSequence s = (AceSequence) e.nextElement();
      if (s.asm_start_padded < leftmost) leftmost = s.asm_start_padded;
      if (s.asm_end_padded > rightmost) rightmost = s.asm_end_padded;
    }
  }

  public int leftmost () {
    return leftmost;
  }

  public int rightmost () {
    return rightmost;
  }
  
  public int consensus_length () {
    return consensus.sequence.length();
  }

}

