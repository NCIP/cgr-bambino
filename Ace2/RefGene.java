package Ace2;

import java.util.*;
import static Ace2.Constants.ALIGNMENT_GAP_CHAR;
import static Ace2.Constants.ALIGNMENT_DELETION_CHAR;

public class RefGene {
  //
  // Golden Path gene/exon mapping data from refGene table
  // - positions will be altered for fragment of chromosome in viewer
  //
  private String symbol,accession,strand,protein;
  private int cds_start,cds_end;
  private ArrayList<Exon> exons;
  private boolean is_initialized=false;
  private boolean is_broken=true;
  private int consensus_adjust = 0;

  public static boolean VERBOSE = false;

  public RefGene (String[] fields) {
    // constructor for flatfile data
    setup(fields);
  }
  
  public RefGene (HashMap<String,String> row) {
    String[] fields = new String[9];
    fields[0] = "refGene";
    fields[1] = row.get("name2");
    fields[2] = row.get("name");
    fields[3] = row.get("strand");
    fields[4] = row.get("cdsStart");
    fields[5] = row.get("cdsEnd");
    fields[6] = row.get("exonStarts");
    fields[7] = row.get("exonEnds");
    fields[8] = row.get("exonFrames");
    setup(fields);
  }

  private void setup (String[] fields) {
    if ((fields.length == 10 || fields.length == 9) && fields[0].equals("refGene")) {
      // 0. "refGene" tag
      // 1. gene symbol
      // 2. GenBank accession
      // 3. strand
      // 4. CDS start
      // 5. CDS end
      // 6. exon starts
      // 7. exon ends
      // 8. exon frame offsets
      //    Not really clear how these work (???)
      //    http://genome.ucsc.edu/FAQ/FAQformat
      // 9. CDS protein translation from refseq
      //    [ since String.split() doesn't preserve trailing null fields this may be missing ]
      symbol = new String(fields[1]);
      accession = new String(fields[2]);
      strand = new String(fields[3]);
      cds_start = Integer.parseInt(fields[4]);
      cds_end = Integer.parseInt(fields[5]);

      exons = new ArrayList<Exon>();
      String[] starts = fields[6].split(",");
      String[] ends = fields[7].split(",");
      String[] frames = fields[8].split(",");
      protein = fields.length == 10 ? new String(fields[9]) : null;
      if (starts.length == ends.length) {
	int ec = 1;
	for (int i = 0; i < starts.length; i++) {
	  Exon ex = new Exon(Integer.toString(ec++), Integer.parseInt(starts[i]), Integer.parseInt(ends[i]), Integer.parseInt(frames[i]));
	  exons.add(ex);
	}
      } else {
	System.err.println("WTF: starts/ends size mismatch");  // debug
      }
      
    } else {
      System.err.println("error instantiating refGene data: flen="+ fields.length);  // debug
    }
  }

  public boolean is_initialized () {
    return is_initialized;
  }

  public boolean is_broken () {
    // parsing problem
    return is_broken;
  }

  public boolean is_rc() {
    return strand.equals("-");
  }

  public String get_strand() {
    return strand;
  }

  public void consensus_adjust (int i) {
    // adjust raw coordinates to consensus position
    // if called, must be BEFORE consensus_setup()!
    consensus_adjust = i;
    cds_start -= i;
    cds_end -= i;
    for (Exon exon : exons) {
      exon.start -= i;
      exon.end -= i;
    }
  }

  public void consensus_setup (PadMap pm) {
    //    System.err.println("seq="+consensus.sequence);  // debug
    is_broken = false;
    // set unless all QC checks passed

    //    System.err.println("processing refgene " + accession);  // debug
    
    char[] consensus = pm.get_padded_sequence();

    int exon_count = exons.size();
    Exon ex = null;
    int ei;
    for (ei=0; ei < exon_count; ei++) {
      // find exon where CDS starts (or ends, in the case of RC sequences)
      ex = exons.get(ei);
      if (VERBOSE) System.err.println("cds:"+cds_start + " start:" + ex.start + " end:" + ex.end);  // debug

      if (cds_start >= ex.start && cds_start <= ex.end) {
	if (VERBOSE) System.err.println("HEY NOW: found CDS in exon index " + ei);  // debug
	break;
      }
    }
    //    System.err.println("start ei:"+ei);  // debug

    int upcsn;
    // unpadded consensus BASE NUMBER (not index)
    int pcsn;
    // padded consensus BASE NUMBER (not index)

    boolean is_rc = is_rc();

    char c;
    Codon codon = null;
    int codon_counter=0;

    if (VERBOSE) System.err.println("cons setup for " + accession);  // debug

    if (!is_broken) {
      // for first exon in sequence, start at CDS start rather than exon start:
      upcsn = cds_start + 1;
      // +1 = convert string index (annotation) to base number

      int max_unpadded = pm.get_max_unpadded();
      boolean codon_hosed = false;

      while (true) {
	//	System.err.println("upcsn="+upcsn);  // debug
	if (upcsn < 1) {
	  // annotation is before our assembly starts
	  pcsn = 1;
	  c = 'a';  // bogus
	  codon_hosed = true;
	} else if (upcsn > max_unpadded) {
	  // annotation is after our assembly ends
	  pcsn = 1;
	  c = 'a';  // bogus
	  codon_hosed = true;
	} else {
	  pcsn = pm.get_unpadded_to_padded(upcsn);
	  //	  System.err.println("upcsn="+upcsn + " max:" +max_unpadded + " pcsn:" + pcsn);  // debug
	  if (pcsn == PadMap.UNDEF) {
	    // out of range
	    c = 'a';
	    codon_hosed = true;
	    pcsn = 1;

	    break;
	    // bail
	  } else {
	    c = consensus[pcsn - 1];
	  }
	}
	if (c == ALIGNMENT_GAP_CHAR || c == ALIGNMENT_DELETION_CHAR) {
	  System.err.println("wanted upcsn:" + upcsn + " got pcsn:" + pcsn + ", got pad/deletion => That's unpossible!!! --Ralph");  // debug
	  is_broken = is_initialized = true;
	  break;
	} else {
	  if (VERBOSE) System.err.println("at " + upcsn + "/" + pcsn + " nt:" + c + " hosed:" + codon_hosed + " xpadded:" + (pcsn + consensus_adjust));  // debug
	  if (codon == null) codon = new Codon(++codon_counter, is_rc);
	  if (codon_hosed) {
	    codon.set_unmappable(codon_hosed);
	  } else {
	    codon.increment_valid_bases();
	  }

	  if (is_rc) {
	    //	    System.err.print(" rc:" + complement(c));  // debug
	  }
	  //	  System.err.println("");  // debug
	  
	  codon.append(c, upcsn, pcsn);

	  if (codon.complete()) {
	    if (false && codon_counter == 1 && !(codon.to_code() == 'M')) {
	      System.err.println("ERROR: first codon not start codon!");  // debug
	      is_broken = is_initialized = true;
	      break;
	    } else {
	      if (VERBOSE) System.err.println("finished codon " + codon_counter + ": " + codon + " => " + codon.to_code());
	      //	      System.err.println("adding codon:"+codon + " unmappable:"+ codon.is_unmappable());  // debug

	      ex.add_codon(codon);
	      //	      if (codon.is_stop()) System.err.println("STOP2");  // debug
	      codon = null;
	      codon_hosed = false;
	    }
	  }

	  //	  if (upcsn >= (ex.end - 1)) {
	  if (upcsn >= ex.end) {
	    //
	    // reached end of exon 
	    //
	    if (VERBOSE) {
	      System.err.println("jumping to next exon, load_index=" + (codon == null ? "null" : codon.get_load_index()) + " partial=" + codon);
	      System.err.println("current frame offset=" + ex.frame_offset);  // debug

	      //	      System.err.println("so far: " + ex.toString());  // debug
	      if (is_rc) {
		StringBuffer sb = new StringBuffer(ex.toString());
		sb = sb.reverse();
		System.err.println("so far: " + sb.toString() + "<<<");
	      } else {
		System.err.println("so far: " + ex.toString());  // debug
	      }
	    }
	    
	    ei++;
	    if (ei >= exon_count) {
	      // finished last exon
	      System.err.println("out of exons, stopping translation...");  // debug
	      break;
	    } else {
	      ex = exons.get(ei);
	      if (VERBOSE) System.err.println("new exon frame offset: " + ex.frame_offset);  // debug

	      //	      upcsn = ex.start;
	      upcsn = ex.start + 1;
	    }
	    //	    System.err.println("start="+ex.start);  // debug

	    //	    System.err.println(consensus.sequence.substring(ex.start, ex.start+60));  // debug
	  } else {
	    upcsn++;
	  }

	  if (upcsn > cds_end) {
	    //	    System.err.println("STOP");  // debug
	    break;
	  }

	}
      }
    }

    if (is_rc) {
      // reverse exon and codon order
      Collections.reverse(exons);
      // exons are mapped in reverse order, correct
      int exno = 0;
      int cc = 0;
      for (Exon e : exons) {
	e.set_id(Integer.valueOf(++exno).toString());
	e.set_rc(true);
	// renumber exons
	//	System.err.println("exon " + exno);  // debug
	ArrayList<Codon> cos = e.get_codons();
	if (cos != null) {
	  Collections.reverse(cos);
	  for (Codon co : cos) {
	    co.set_codon_number(++cc);
	    //	    System.err.println("pos:" + co.consensus_pos[0] + "," + co.consensus_pos[1] + "," + co.consensus_pos[2]);  // debug
	  }
	}
      }

      //	Collections.reverse(e.get_codons());
      //      }
    }

    boolean all_mappable = true;
    Codon first_codon = null;
    for (Exon e : exons) {
      for (Codon co : e.get_codons()) {
	if (co.is_unmappable()) {
	  all_mappable = false;
	  break;
	}
	if (first_codon == null) first_codon = co;
      }
    }

    if (all_mappable) {
      // sanity check: only perform if all codons can be placed on the assembly
      // (i.e. not for very small subassemblies where only a subset of codons will fit)

      if (protein != null && protein.length() > 0) {
	// full translated protein sequence available, see if it matches
	StringBuffer sb = new StringBuffer();
	for (Exon ex2 : exons) {
	  sb.append(ex2.get_protein_sequence());
	}
	String translated = sb.toString();
	String p2 = protein + ProteinTool.STOP_CODE;
	if (!p2.equals(translated)) {
	  System.err.println("ERROR: CDS map sanity check failed for " + accession + ", translated protein != refseq protein");  // debug
	  is_broken = true;
	}
      } else if (first_codon != null) {
	// lesser check: will only catch corrupt translations
	// on reverse strand, where last codon translated should be start codon
	char code = first_codon.to_code();
	if (code != 'M') {
	  System.err.println("ERROR: CDS map sanity check failed for " + accession + ", first mapped codon is not M");  // debug
	  is_broken = true;
	}
      }
    }

    if (VERBOSE) {
      System.err.println("exon count: " + exons.size());  // debug
      System.err.println("is_broken: " + is_broken);  // debug
      for (Exon e : exons) {
	System.err.println("exon " + e);
      }
    }

    //    System.err.println("FIX ME: after finalization, confirm 1st codon = M!");  // debug
    // don't do this: if viewer is started for a small region (rather than full gene),
    // some codons may be unmapped/truncated

    for (Exon exon : exons) {
      // find consensus start/end offsets for each exon
      exon.build_consensus_ranges();
    }

    is_initialized=true;
  }

  public boolean is_visible(int csv, int cev) {
    //
    // are one or more exons visible in given (padded) consensus range?
    //
    boolean result = false;
    for (Exon exon : exons) {
      if (exon.intersects(csv, cev)) {
	result = true;
	break;
      }
    }
    return result;
  }

  public ArrayList<Exon> get_visible_exons (int csv, int cev) {
    ArrayList<Exon> results = new ArrayList<Exon>();
    for (Exon exon : exons) {
      if (exon.intersects(csv, cev)) {
	results.add(exon);
      }
    }
    return results;
  }

  public String get_accession() {
    return accession;
  }

  public String get_symbol() {
    return symbol;
  }

  public int get_exon_count() {
    return exons.size();
  }

  public Exon get_exon_id (String id) {
    Exon result = null;
    for (Exon e : exons) {
      if (e.id.equals(id)) {
	result = e;
	break;
      }
    }
    return result;
  }

  public ArrayList<Exon> get_exons() {
    return exons;
  }

  public static char complement (char c) {
    // FIX ME: CENTRALIZE
    char result = 0;
    switch (c) {
    case 'a': result = 't'; break;
    case 'A': result = 'T'; break;
    case 'c': result = 'g'; break;
    case 'C': result = 'G'; break;
    case 'g': result = 'c'; break;
    case 'G': result = 'C'; break;
    case 't': result = 'a'; break;
    case 'T': result = 'A'; break;
    case 'n': result = 'n'; break;
    case 'N': result = 'N'; break;
    case ALIGNMENT_GAP_CHAR: result = ALIGNMENT_GAP_CHAR; break;
    case ALIGNMENT_DELETION_CHAR: result = ALIGNMENT_DELETION_CHAR; break;
    default:
      System.err.println("error, don't know to complement nt " + c);  // debug
      result = c;
    }
    return result;
  }

  public void intron_splice_adjust (AceViewerConfig config) {
    //    char[] consensus = pm.get_padded_sequence();
    //    System.err.println("cons = " + new String(consensus));  // debug

    if (config.intron_compressor != null) {
      int offset = config.intron_compressor.get_start_shift(cds_start, false);
      cds_start -= offset;
      cds_end -= offset;

      ArrayList<Exon> filtered = new ArrayList<Exon>();

      for (Exon exon : exons) {
	if (config.intron_compressor.is_completely_trimmed(exon.start, exon.end, false)) {
	  // untested w/padded reference
	  System.err.println("removing completely-trimmed exon " + exon.id);  // debug
	} else {
	  exon.intron_splice_adjust(config.intron_compressor, config.assembly.get_padmap());
	  if (VERBOSE) System.err.println("post-slice range for " +exon.id+"="+exon.start+"-"+exon.end);  // debug

	  filtered.add(exon);
	}
      }
      exons = filtered;
    }
  }


}
