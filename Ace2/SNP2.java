package Ace2;

public class SNP2 {
  String name, type;
  
  String reference_sequence_name;
  int base_number;

  int size, coverage;
  float percent_alternative_allele, p_value;

  //  Base chr_allele, alternative_allele;
  //  (insufficient for indel)
  String reference_allele, alternative_allele;

  String text;

  int reference_normal_count, reference_tumor_count;
  int alternative_normal_count, alternative_tumor_count;
  int dbsnp;

  int display_position;
  // external use

  public SNP2 (Reporter rpt) {
    name = rpt.get_value(SAMStreamingSNPFinder.HEADER_NAME);
    reference_sequence_name = rpt.get_value(SAMStreamingSNPFinder.HEADER_CHR);
    base_number = Integer.parseInt(rpt.get_value(SAMStreamingSNPFinder.HEADER_POS));
    type = rpt.get_value(SAMStreamingSNPFinder.HEADER_TYPE);
    size = Integer.parseInt(rpt.get_value(SAMStreamingSNPFinder.HEADER_SIZE));
    coverage = Integer.parseInt(rpt.get_value(SAMStreamingSNPFinder.HEADER_COVERAGE));
    percent_alternative_allele = Float.parseFloat(rpt.get_value(SAMStreamingSNPFinder.HEADER_PCT_ALT_ALLELE));

    reference_allele = rpt.get_value(SAMStreamingSNPFinder.HEADER_CHR_ALLELE);
    alternative_allele = rpt.get_value(SAMStreamingSNPFinder.HEADER_ALT_ALLELE);
    
    p_value = Float.parseFloat(rpt.get_value(SAMStreamingSNPFinder.HEADER_P_VALUE));
    text = rpt.get_value(SAMStreamingSNPFinder.HEADER_TEXT);
    reference_normal_count = Integer.parseInt(rpt.get_value(SAMStreamingSNPFinder.HEADER_REF_NORMAL_COUNT));
    reference_tumor_count = Integer.parseInt(rpt.get_value(SAMStreamingSNPFinder.HEADER_REF_TUMOR_COUNT));
    alternative_normal_count = Integer.parseInt(rpt.get_value(SAMStreamingSNPFinder.HEADER_ALT_NORMAL_COUNT));
    alternative_tumor_count = Integer.parseInt(rpt.get_value(SAMStreamingSNPFinder.HEADER_ALT_TUMOR_COUNT));
  }

}
