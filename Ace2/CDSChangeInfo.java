package Ace2;

public class CDSChangeInfo {
  int cpos;
  char variant_nt;
  Codon codon_orig, codon_altered;
  RefGene refgene;

  public String toString() {
    return refgene.get_accession() + ": " + codon_orig + "->" + codon_altered + " = " + codon_orig.to_name() + Integer.toString(codon_orig.get_id()) + codon_altered.to_name() + 
      (is_silent() ? " (silent)" : " (protein change)");
  }

  public boolean is_silent() {
    return codon_orig.is_silent(codon_altered);
  }

  
}

