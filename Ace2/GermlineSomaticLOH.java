package Ace2;

import java.util.*;

public class GermlineSomaticLOH {

  private static final int MIN_ALLELE_COVERAGE_TO_CALL = 4;

  private static final float LOH_WIGGLE_FRACTION = 0.05f;
  private static final float MIN_GERMLINE_NORMAL_ALLELE_FREQUENCY = 0.15f;
  // when using "broad" read counts to call, a good idea to set this
  // a bit higher than you otherwise might as they are noisier.

  private boolean too_low, somatic, germline;
  private float alt_n_freq, alt_t_freq, ref_n_freq, ref_t_freq, alt_tn_freq_diff;
  private int loh;

  public static final int LOH_NO_LOH = 0;
  public static final int LOH_LOSS_OF_REFERENCE_ALLELE = 1;
  public static final int LOH_LOSS_OF_VARIANT_ALLELE = 2;

  public boolean call (GermlineSomaticTestCase tcase) {
    return call(tcase.ref_n_count,
		tcase.ref_t_count,
		tcase.alt_n_count,
		tcase.alt_t_count);
  }

  public boolean call (int ref_n_count, int ref_t_count,
		    int alt_n_count, int alt_t_count) {

    //
    //  reset
    //
    too_low = somatic = germline = false;
    loh = LOH_NO_LOH;

    int n_total = ref_n_count + alt_n_count;
    int t_total = ref_t_count + alt_t_count;

    alt_n_freq = n_total == 0 ? 0 : (float) alt_n_count / n_total;
    alt_t_freq = t_total == 0 ? 0 : (float) alt_t_count / t_total;

    ref_n_freq = n_total == 0 ? 0 : (float) ref_n_count / n_total;
    ref_t_freq = t_total == 0 ? 0 : (float) ref_t_count / t_total;

    alt_tn_freq_diff = alt_t_freq - alt_n_freq;

    
    if ((ref_n_count >= MIN_ALLELE_COVERAGE_TO_CALL &&
	 alt_t_count >= MIN_ALLELE_COVERAGE_TO_CALL) ||
	(n_total > MIN_ALLELE_COVERAGE_TO_CALL &&
	 t_total > MIN_ALLELE_COVERAGE_TO_CALL)) {
      //      System.err.println("fix me: germline check: n="+n_total + " t="+t_total);  // debug

      //
      //  sufficient observations of reference in normal and alternative in tumor
      //  to attempt a call
      //

      boolean somatic_criteria_high = (alt_tn_freq_diff >= 0.50 &&
				       alt_n_freq <= 0.01 &&
				       ref_n_count >= 4 &&
				       alt_t_count >= 5
				       );
      // high difference observed between tumor and normal:
      // low coverage required to make call

      boolean somatic_criteria_medium = (alt_tn_freq_diff >= 0.20 &&
					 alt_tn_freq_diff < 0.50 &&
					 alt_n_freq <= 0.01 &&
					 ref_n_count >= 20 &&
					 alt_t_count >= 4
					 );
      // medium difference observed between tumor and normal:
      // a bit more coverage required to make call

      boolean somatic_criteria_low = (alt_tn_freq_diff >= 0.10 &&
				      alt_tn_freq_diff < 0.20 &&
				      alt_n_freq <= 0.01 &&
				      ref_n_count >= 30 &&
				      alt_t_count >= 4
				      );
      // small difference observed between tumor and normal:
      // high coverage required to make call

      somatic = somatic_criteria_high || somatic_criteria_medium || somatic_criteria_low;

      //
      //  germline call:
      // 
      germline = alt_n_freq >= MIN_GERMLINE_NORMAL_ALLELE_FREQUENCY;
      // for variant to be considered germline, variant allele must be observed at a
      // minimum frequency in normal data.
      // FIX ME: anything more sophisticated?

      //      System.err.println("alt_n_freq="+alt_n_freq + " alt_t_freq=" + alt_t_freq + " germline=" + germline);
      //
      //  LOH call:
      //
      // - FIX ME: what if we observe 2 in normal, but only REFERENCE in tumor?
//       loh = ref_n_freq >= LOH_WIGGLE_FRACTION && alt_n_freq >= LOH_WIGGLE_FRACTION &&
// 	// in normal, observe both reference and variant alleles
// 	alt_t_freq >= LOH_WIGGLE_FRACTION && ref_t_freq <= LOH_WIGGLE_FRACTION;
      // in tumor, observe the variant allele but NOT the reference allele
      // (allowing a little wiggle room for sequencing noise)
      // FIX ME: what if observe only reference allele in tumor??

      if (ref_n_freq >= MIN_GERMLINE_NORMAL_ALLELE_FREQUENCY &&
	  alt_n_freq >= MIN_GERMLINE_NORMAL_ALLELE_FREQUENCY) {
	///
	// variant is heterozygous in normal
	// (both reference and variant alleles observed at minimum frequency in normal)
	//
	if (alt_t_freq >= MIN_GERMLINE_NORMAL_ALLELE_FREQUENCY &&
	    ref_t_freq <= LOH_WIGGLE_FRACTION) {
	  // variant allele observed in tumor, reference allele lost
	  loh = LOH_LOSS_OF_REFERENCE_ALLELE;
	} else if (ref_t_freq >= MIN_GERMLINE_NORMAL_ALLELE_FREQUENCY &&
		   alt_t_freq <= LOH_WIGGLE_FRACTION) {
	  // reference allele observed in tumor, variant allele lost
	  loh = LOH_LOSS_OF_VARIANT_ALLELE;
	}
      }
    } else {
      //
      //  insufficient data
      //
      too_low = true;
    }

    return !too_low;
  }

  public boolean is_loh() {
    return loh != LOH_NO_LOH;
  }

  public int get_loh_type() {
    return loh;
  }

  public boolean is_somatic() {
    return somatic;
  }

  public boolean is_germline() {
    return germline;
  }

  public static void main (String[] argv) {
    ArrayList<GermlineSomaticTestCase> cases = new ArrayList<GermlineSomaticTestCase>();

    if (argv.length == 4) {
      cases.add(new GermlineSomaticTestCase(
					    "command-line specified",
					    Integer.parseInt(argv[0]),
					    Integer.parseInt(argv[1]),
					    Integer.parseInt(argv[2]),
					    Integer.parseInt(argv[3])
					    ));
    } else {
      System.err.println("specify: ref_count_normal ref_count_tumor alt_count_normal alt_count_tumor");  // debug
      System.err.println("running tests:");  // debug
      cases.add(new GermlineSomaticTestCase("LOH (loss of reference)", 5, 0, 5, 10));
      cases.add(new GermlineSomaticTestCase("LOH (loss of variant)", 5, 10, 5, 0));
    }
      
    for (GermlineSomaticTestCase tcase : cases) {
      System.err.println(
			 "desc:" + tcase.description + " " + 
			 "ref_n:" + tcase.ref_n_count + " " +
			 "ref_t:" + tcase.ref_t_count + " " +
			 "alt_n:" + tcase.alt_n_count + " " +
			 "alt_t:" + tcase.alt_t_count 
			 );  // debug

      GermlineSomaticLOH gsl = new GermlineSomaticLOH();
      boolean call = gsl.call(tcase);
      System.err.println("     callable: "+call);  // debug
      System.err.println("  is_germline: " + gsl.is_germline());  // debug
      System.err.println("  is_somatic: " + gsl.is_somatic());  // debug
      System.err.println("      is_loh: " + gsl.is_loh() + " type=" + gsl.get_loh_type());  // debug
      System.err.println("  alt_freq_n: " + gsl.get_alt_freq_normal());  // debug
      System.err.println("  alt_freq_t: " + gsl.get_alt_freq_tumor());  // debug
      System.err.println("        diff: " + gsl.get_alt_tn_freq_diff());  // debug
      System.err.println("");  // debug
    }
  }

  public float get_alt_freq_normal() {
    return alt_n_freq;
  }

  public float get_alt_freq_tumor() {
    return alt_t_freq;
  }

  public float get_alt_tn_freq_diff() {
    return alt_tn_freq_diff;
  }

}