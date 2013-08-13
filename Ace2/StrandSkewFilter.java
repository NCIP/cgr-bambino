package Ace2;

public class StrandSkewFilter {
  SNPConfig config;
  private int v_plus, v_minus;
  // count of variant reads on + and - strand
  private int cov_plus, cov_minus;
  // total coverage on + and - strand

  public static boolean VERBOSE = false;

  boolean EVALUATABLE;
  boolean FILTER_FAIL;
  double VARIANT_PLUS_STRAND_FREQUENCY;

  private int UNEVALUATABLE_REASON;

  public static final int REASON_LOW_VARIANT_COUNT = 1;
  public static final int REASON_LOW_PLUS_COVERAGE = 2;
  public static final int REASON_LOW_MINUS_COVERAGE = 3;

  public StrandSkewFilter (SNPConfig config, TumorNormalReferenceTracker tnrt) {
    this.config = config;
    v_plus = tnrt.get_variant_fwd_count();
    v_minus = tnrt.get_variant_rev_count();
    cov_plus = tnrt.get_coverage_fwd();
    cov_minus = tnrt.get_coverage_rev();
  }

  public static void main (String[] argv) {
    test_skew();
  }

  public static void test_skew() {
    SNPConfig snp_config = new SNPConfig();
    
    //
    // skew example
    //
    TumorNormalReferenceTracker t = new TumorNormalReferenceTracker();
    t.add_set(ReferenceOrVariant.REFERENCE, Strand.STRAND_POSITIVE, TumorNormal.NORMAL, 9);
    t.add_set(ReferenceOrVariant.REFERENCE, Strand.STRAND_NEGATIVE, TumorNormal.NORMAL, 14);
    t.add_set(ReferenceOrVariant.REFERENCE, Strand.STRAND_POSITIVE, TumorNormal.TUMOR, 5);
    t.add_set(ReferenceOrVariant.REFERENCE, Strand.STRAND_NEGATIVE, TumorNormal.TUMOR, 24);

    t.add_set(ReferenceOrVariant.VARIANT, Strand.STRAND_POSITIVE, TumorNormal.NORMAL, 5);
    t.add_set(ReferenceOrVariant.VARIANT, Strand.STRAND_NEGATIVE, TumorNormal.NORMAL, 0);
    t.add_set(ReferenceOrVariant.VARIANT, Strand.STRAND_POSITIVE, TumorNormal.TUMOR, 7);
    t.add_set(ReferenceOrVariant.VARIANT, Strand.STRAND_NEGATIVE, TumorNormal.TUMOR, 0);

    t.analyze();
    // needed?

    StrandSkewFilter ssk = new StrandSkewFilter(snp_config, t);

    boolean ok = ssk.skew_test();
    System.err.println("skew passed?: " + ok);  // debug
  }

  public boolean skew_test() {
    FILTER_FAIL = false;

    //
    // Normalize counts by strand:
    // if one strand has much higher coverage than the other, this
    // will distort/overwhelm comparisons.
    //

    VARIANT_PLUS_STRAND_FREQUENCY = -1;
    // normalized frequency of variant allele on + strand
    int v_total = v_plus + v_minus;
    EVALUATABLE = false;
    UNEVALUATABLE_REASON = 0;
    if (v_total < config.STRAND_SKEW_CALL_MIN_VARIANT_COUNT) {
      // insufficient variant calls to make a skew call
      UNEVALUATABLE_REASON = REASON_LOW_VARIANT_COUNT;
    } else if (cov_plus < config.STRAND_SKEW_CALL_MIN_STRAND_COVERAGE) {
      // insufficient coverage on + to make a skew call
      UNEVALUATABLE_REASON = REASON_LOW_PLUS_COVERAGE;
    } else if (cov_minus < config.STRAND_SKEW_CALL_MIN_STRAND_COVERAGE) {
      // insufficient coverage on - to make a skew call
      UNEVALUATABLE_REASON = REASON_LOW_MINUS_COVERAGE;
    } else {
      EVALUATABLE = true;
      double v_plus_normalized, v_minus_normalized;
      if (cov_plus > cov_minus) {
	v_plus_normalized = v_plus;
	v_minus_normalized = v_minus * ((double) cov_plus / (double) cov_minus);
      } else {
	v_plus_normalized = v_plus * ((double) cov_minus / (double) cov_plus);
	v_minus_normalized = v_minus;
      }

      double v_total_normalized = v_plus_normalized + v_minus_normalized;
      VARIANT_PLUS_STRAND_FREQUENCY = v_plus_normalized / v_total_normalized;
      //      SKEW = (0.50f - (1 - freq)) * 2;
      // +1 = skewed 100% towards + strand
      // -1 = skewed 100% towards - strand

      if (VERBOSE) System.err.print("c+=" + cov_plus + " c-=" + cov_minus + 
				    " v+=" + v_plus + " v-=" + v_minus + 
				    " v+n="+v_plus_normalized + " v-n=" + v_minus_normalized + 
				    " freq=" + VARIANT_PLUS_STRAND_FREQUENCY + 
				    " evaluatable=" + EVALUATABLE + " " +
				    " uneval_reason=" + explain_unevaluatable()
				    );

    }

    double min_skew_minus = 1 - config.STRAND_SKEW_FILTER_MIN_STRAND_PERCENT_TO_CONSIDER_SKEWED;

    if (
	config.STRAND_SKEW_FILTER_ENABLE &&
	EVALUATABLE &&
	(v_total >= config.STRAND_SKEW_FILTER_MIN_VARIANT_COUNT) &&
	(cov_plus >= config.STRAND_SKEW_FILTER_MIN_STRAND_COVERAGE) &&
	(cov_minus >= config.STRAND_SKEW_FILTER_MIN_STRAND_COVERAGE) &&
	(
	 VARIANT_PLUS_STRAND_FREQUENCY >= config.STRAND_SKEW_FILTER_MIN_STRAND_PERCENT_TO_CONSIDER_SKEWED ||
	 VARIANT_PLUS_STRAND_FREQUENCY <= min_skew_minus
	 )
	) {
      //
      // (normalized) variant is observed mostly on one strand or the other.
      //
      FILTER_FAIL = true;
    }

    if (EVALUATABLE && VERBOSE) System.err.println(" passed="+!FILTER_FAIL);  // debug

    return !FILTER_FAIL;
  }

  public double get_normalized_variant_plus_strand_frequency () {
    return VARIANT_PLUS_STRAND_FREQUENCY;
  }

  public boolean is_evaluatable() {
    return EVALUATABLE;
  }

  public String explain_unevaluatable() {
    String reason = "unknown";
    if (UNEVALUATABLE_REASON == 0) {
      reason = "none";
    } else if (UNEVALUATABLE_REASON == REASON_LOW_VARIANT_COUNT) {
      reason = "low_variant_count";
    } else if (UNEVALUATABLE_REASON == REASON_LOW_PLUS_COVERAGE) {
      reason = "low_plus_strand_coverage";
    } else if (UNEVALUATABLE_REASON == REASON_LOW_MINUS_COVERAGE) {
      reason = "low_minus_strand_coverage";
    } else {
      reason = "unhandled code " + UNEVALUATABLE_REASON;
    }

    return reason;
  }
  
}
