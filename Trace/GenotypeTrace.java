package Trace;

public class GenotypeTrace {
  // trace-level genotype information
  public String allele_1,allele_2;
  public String trace_id,trace_label;
  public int trace_offset,sequence_position;
  public String orientation;
  public int genotype_quality;

  public boolean contains_allele (String allele) {
    return allele.equals(allele_1) || allele.equals(allele_2);
  }

  public boolean is_heterozygous() {
    return (allele_1 != null &&
            allele_2 != null &&
            !(allele_1.equals(allele_2)));
  }

}
