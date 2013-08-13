package Ace2;

import java.util.*;
import java.awt.*;
import javax.swing.*;

public class CDSChangePredictor {
  //
  // predict protein coding changes from SNPs
  //

  private Assembly assembly;
  private ArrayList<RefGene> refgenes;
  private boolean in_codon;
  private ArrayList<CDSChangeInfo> changes;
  private String consensus_tag;
  private char cons_nt;

  public CDSChangePredictor (AceViewerConfig config) {
    assembly = config.assembly;
    refgenes = config.refgenes;
    consensus_tag = config.CONSENSUS_TAG;
  }

  public CDSChangePredictor (Assembly assembly, ArrayList<RefGene> refgenes, String consensus_tag) {
    this.assembly = assembly;
    this.refgenes = refgenes;
    this.consensus_tag = consensus_tag;
  }

  public ArrayList<CDSChangeInfo> get_changes() {
    return changes;
  }

  public boolean has_mapping_error() {
    boolean error = false;
    for (RefGene rg : refgenes) {
      if (rg.is_broken()) {
	error = true;
	break;
      }
    }
    return error;
  }
  
  public boolean predict_cds_changes (int cpos, char variant_nt) {
    changes = new ArrayList<CDSChangeInfo>();

    char[] cons = assembly.get_consensus_sequence();
    cons_nt = Character.toUpperCase(cons[cpos - 1]);

    in_codon = false;

    //
    //  find affected codon:
    //
    for (RefGene rg : refgenes) {
      if (!rg.is_initialized()) {
	// init if necessary
	rg.consensus_setup(assembly.get_padmap());
      }

      if (rg.is_broken()) {
	//	System.err.println("ERROR: can't use broken-parsed refgene");  // debug
	continue;
      }

      for (Exon exon : rg.get_visible_exons(cpos, cpos)) {
	for (Codon codon : exon.get_codons()) {
	  if (codon.c_start_offset() == cpos ||
	      codon.c_center_offset() == cpos ||
	      codon.c_last_offset() == cpos) {
	    // this codon contains the variant base
	    //	    System.err.println("start="+codon.c_start_offset() + " center=" + codon.c_center_offset() + " last=" + codon.c_last_offset());  // debug
	    in_codon = true;

	    CDSChangeInfo info = new CDSChangeInfo();
	    info.cpos = cpos;
	    info.variant_nt = variant_nt;
	    info.codon_orig = codon;
	    info.codon_altered = codon.generate_altered(cpos, variant_nt);
	    info.refgene = rg;
	    changes.add(info);
	  }
	}
      }
    }
    return in_codon;
  }

  public char get_variant_nt (int cpos, Component c) {
    char[] cons = assembly.get_consensus_sequence();
    char cons_nt = Character.toUpperCase(cons[cpos - 1]);
    // FIX ME: use some centralized call for this

    BaseCounter3 bc = new BaseCounter3(assembly);
    bc.add_sequences(cpos);
    bc.add_base(cons_nt);
    HashSet<Character> nts = bc.get_bases();
    int nt_count = nts.size();

    char variant_nt = 0;
    if (nt_count == 1) {
      // no variation
      JOptionPane.showMessageDialog(c,
				    "No sequence variation at this position.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
    } else if (refgenes == null) {
      JOptionPane.showMessageDialog(c,
				    "Can't predict protein changes: no reference sequence annotations loaded.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
    } else {
      if (nt_count == 2) {
	//
	// exactly 2 alleles visible
	//
	for (Character c2 : nts) {
	  if (c2 != cons_nt) {
	    variant_nt = c2;
	  }
	}
      } else {
	//
	// multiple alleles visible: prompt for which to use
	//
	Character[] others = new Character[nts.size() - 1];
	int oi=0;

	Base[] by_freq = bc.get_bases_by_frequency();
	// sorted list of nucleotides by frequency (highest to lowest)

	for (int j=0; j < by_freq.length; j++) {
	  if (!by_freq[j].equals(cons_nt)) {
	    others[oi++] = by_freq[j].charValue();
	  }
	}

	Character response = (Character) JOptionPane.showInputDialog(
								     c,
								     //								       "Specify alternative allele for protein change prediction (" + config.CONSENSUS_TAG + ": " + cons_nt + ")",
								     "Specify alternative allele for protein change prediction" + "\n(" + consensus_tag + ": " + cons_nt + ")",
								     "SNP protein change prediction",
								     JOptionPane.QUESTION_MESSAGE,
								     null,
								     others,
								     null);
	if (response != null) {
	  variant_nt = response;
	}
      }
    }

    return variant_nt;
  }

  public char get_consensus_nt () {
    return cons_nt;
  }

  public String toString() {
    return Funk.Str.join("\n", changes);
  }

  public String toString(String delimiter) {
    return Funk.Str.join(delimiter, changes);
  }

  public String get_any_change() {
    String label;
    if (changes.size() == 0) {
      label = "no";
    } else {
      label = "silent";
      for (CDSChangeInfo change : changes) {
	if (!change.is_silent()) {
	  label = "yes";
	  break;
	}
      }
    }
    return label;
  }

  public int get_any_change_int() {
    int result;
    if (changes.size() == 0) {
      result = 0;
    } else {
      result = 1;
      for (CDSChangeInfo change : changes) {
	if (!change.is_silent()) {
	  result = 2;
	  break;
	}
      }
    }
    return result;
  }


  

}