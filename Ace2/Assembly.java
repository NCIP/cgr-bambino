package Ace2;
//
// high-level wrapper for alignment data classes, to aid abstraction
// between .ace and SAM formats
//
// MNE 7/2009

import java.util.*;

public abstract class Assembly {
  protected IntBucketIndex seqs_by_asp;
  protected int leftmost, rightmost, longest_id;
  protected boolean display_sequences_by_position = true;
  protected SNPList snps;
  protected PadMap pm;

  public void build_alignment() {
    //
    // any postprocessing after raw alignment data has been loaded
    //
    boolean first = true;
    int nlen,i,start,end;
    
    //    System.err.println("running build_alignment():");  // debug
    //    (new Exception()).printStackTrace();

    seqs_by_asp = new IntBucketIndex(100);

    for (AssemblySequence as : get_sequences()) {
      nlen = as.get_name().length();

      start = as.get_asm_start();
      end = as.get_asm_end();
      seqs_by_asp.add_range(start, end, as);

      if (first) {
	leftmost = start;
	rightmost = end;
	longest_id = nlen;
	first=false;
      } else {
	if (start < leftmost) leftmost = start;
	if (end > rightmost) rightmost = end;
	if (nlen > longest_id) longest_id = nlen;
      }
    }
  }

  public ArrayList<AssemblySequence> get_aligned_at (int offset) {
    // FIX ME: rename to get_visible_at()?
    ArrayList<AssemblySequence> results = new ArrayList<AssemblySequence>();
    for (Object o : seqs_by_asp.find(offset)) {
      // use bucket index to find sequences mapped in this region
      AssemblySequence as = (AssemblySequence) o;
      if (offset <= as.get_asm_end()) results.add(as);
      // FIX ME: replace get_asm_end() with class variable???  (speed)
    }
    return results;
  }

  public ArrayList<AssemblySequence> get_visible_sequences (int contig_start_view, int contig_end_view) {
    ArrayList<AssemblySequence> fwd = new ArrayList<AssemblySequence>();
    ArrayList<AssemblySequence> rev = new ArrayList<AssemblySequence>();

    for (Object o : get_sequences()) {
      AssemblySequence as = (AssemblySequence) o;
      if (!(as.get_asm_start() >= contig_end_view ||
	    //	    as.get_asm_end() <= contig_start_view)) {
	    as.get_asm_end() < contig_start_view)) {
	if (as.is_complemented()) {
	  rev.add(as);
	} else {
	  fwd.add(as);
	}
      }
    }

    fwd.addAll(rev);
    return fwd;
  }

  public PadMap get_padmap() {
    if (is_loaded()) {
      if (pm == null) pm = new PadMap(get_consensus_sequence());
    }
    return pm;
  }

   public int get_leftmost() {
     return leftmost;
   }

   public int get_rightmost() {
     return rightmost;
   }

   public int get_assembly_width() {
     return (get_rightmost() - get_leftmost()) + 1;
   };

   public SNPList get_snps() {
     return snps;
   }
   public void set_snps(SNPList snps) {
     this.snps = snps;
   }

  public void set_display_by_position (boolean v) {
    display_sequences_by_position = v;
  }

   public int get_max_id_length() {
     // string length of longest sequence ID
     return longest_id;
   }

  public abstract void build_summary_info();
  public abstract void addObserver (Observer o);

  public abstract String get_title();

  public abstract boolean is_loaded();
  public abstract boolean is_empty();
  public abstract boolean has_error();
  public abstract String get_error_message();
  public abstract boolean alignment_is_built();

  //  public abstract String get_consensus_sequence();
  // return padded consensus sequence (for currently selected contig, if applicable)
  // FIX ME: char[] instead???

  public abstract char[] get_consensus_sequence();

  public abstract ArrayList<AssemblySequence> get_sequences();
  public abstract AssemblySequence get_sequence(String id);

  public abstract Sample get_sample_for(String read_name);
  // return a Sample record for a given read, if available
  // (for efficiency reasons might not be stored at the read level)

  public abstract void set_quality(FASTAQualityReader fq);
  // bleh
  public abstract boolean has_quality();

  public abstract boolean supports_contigs();
  public abstract ArrayList<String> get_contig_id_list();
  public abstract String get_contig_id();
  public abstract void set_contig (String id);
  public abstract String get_biggest_contig_id();


}