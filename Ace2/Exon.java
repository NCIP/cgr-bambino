package Ace2;

import java.util.*;

public class Exon {
  String id;
  int start,end;
  ArrayList<Codon> codons;
  int consensus_start,consensus_end,frame_offset;
  boolean is_rc=false;

  public Exon (String id, int start, int end, int frame_offset) {
    this.id=id;
    this.start=start;
    this.end=end;
    this.frame_offset=frame_offset;
    codons = new ArrayList<Codon>();
  }

  public void add_codon (Codon c) {
    codons.add(c);
  }

  public int get_first_start() {
    // consensus start position of first mappable codon
    int size = codons.size();
    int result = 0;
    for (int i = 0; i < size; i++) {
      Codon c = codons.get(i);
      if (!c.is_unmappable()) {
	result = c.c_start_offset();
	break;
      }
    }
    return result;
  }

  public int get_last_end() {
    // consensus end position of last mappable codon
    int size = codons.size();
    int result = 0;
    for (int i = size - 1; i >= 0; i--) {
      Codon c = codons.get(i);
      if (!c.is_unmappable()) {
	result = c.c_last_offset();
	break;
      }
    }
    return result;
  }

  public void build_consensus_ranges() {
    //    System.err.println("codons=" + codons);

    if (codons == null || codons.size() == 0) {
      //      System.err.println("no codons available");  // debug
      consensus_start = consensus_end = 0;
    } else {
      if (is_rc) {
	//	consensus_start = codons.get(codons.size() - 1).c_last_offset();
	//	consensus_end = codons.get(0).c_start_offset();
	consensus_start = get_last_end();
	consensus_end = get_first_start();
      } else {
	//	consensus_start = codons.get(0).c_start_offset();
	//	consensus_end = codons.get(codons.size() - 1).c_last_offset(); 
	consensus_start = get_first_start();
	consensus_end = get_last_end();
      }
      // FAILS for rc'd exons
      //      System.err.println("start="+consensus_start + " end="+consensus_end);  // debug
    }
    // this is a limited measurement because a codon might start
    // in the PREVIOUS exon and continue into this one

    //    System.err.println("bcr: exon " + start + "-" + end + " cons:" + consensus_start + "-" + consensus_end);
  }

  public boolean intersects (int csv, int cev) {
    //    System.err.println("intersect: " + consensus_start + "," + consensus_end + " q:" + csv + "," + cev + " status:" + !(consensus_start > cev || consensus_end < csv));
    //    return !(consensus_start >= cev || consensus_end <= csv);
    return !(consensus_start > cev || consensus_end < csv);
    // sequence is visible unless it starts after end of visible area,
    // or ends before visible area.
  }

  public ArrayList<Codon> get_codons() {
    return codons;
  }

  public String get_id() {
    return id;
  }

  public void set_id(String id) {
    this.id=id;
  }
  
  public void set_rc(boolean is_rc) {
    this.is_rc=is_rc;
  }

  public String get_protein_sequence() {
    StringBuffer sb = new StringBuffer();
    for (Codon co : get_codons()) {
      if (!co.is_unmappable()) sb.append(co.to_code());
    }
    return sb.toString();
  }

  public String toString() {
    //    if (is_rc) sb = sb.reverse();
    return "exon " + id + ": " + get_protein_sequence();
  }

  public void intron_splice_adjust (IntronCompressor ic, PadMap pm) {
    //
    // for intron splicing hack
    //
    int offset = ic.get_start_shift(start, false);
    start -= offset;
    end -= offset;

    for (Codon c : codons) {
      c.intron_splice_adjust(ic, pm);
    }
    
  }

}
