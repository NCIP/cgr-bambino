package Ace2;

import java.util.*;

public class JointPolymorphismScore {
  double reference_probability = -1;
  double variant_probability = -1;

  static final double prior_poly = .001;
  static final double prior_not = .999;
  
  public void set_reference_probability(BaseCountInfo bci) {
    reference_probability = get_probability_product(bci);
  }

  public void set_reference_probability(ArrayList<SNPTrackInfo> stis) {
    reference_probability = -1;
    for (SNPTrackInfo sti : stis) {
      if (reference_probability == -1) {
	// init
	reference_probability = quality_to_probability(sti.get_quality());
      } else {
	reference_probability *= quality_to_probability(sti.get_quality());
      }
    }
  }

  public void set_reference_probability_from_quality(int qual) {
    reference_probability = quality_to_probability(qual);
  }

  public void set_variant_probability(BaseCountInfo bci) {
    variant_probability = get_probability_product(bci);
  }

  public void set_variant_probability(ArrayList<IndelInfo> infos) {
    variant_probability = -1;
    for (IndelInfo ii : infos) {
      if (variant_probability == -1) {
	variant_probability = quality_to_probability(ii.quality);
      } else {
	variant_probability *= quality_to_probability(ii.quality);
      }
    }
  }

  private double quality_to_probability (int qual) {
    return Math.pow(10, (double) (- qual / 10));
    // see phred documentation
  }

  public double calculate_score() {
    double conditional_poly = (1 - reference_probability) * (1 - variant_probability);
    double conditional_not = ((1 - reference_probability) * variant_probability) +
      (reference_probability * (1 - variant_probability));
    double joint_poly = prior_poly * conditional_poly;
    double joint_not = prior_not * conditional_not;
    return joint_poly / (joint_poly + joint_not);
  }

  private double get_probability_product (BaseCountInfo bci) {
    double product = -1;
    for (Object o : bci.sequences) {
      SNPTrackInfo sti = (SNPTrackInfo) o;
      if (product == -1) {
	product = quality_to_probability(sti.get_quality());
      } else {
	product *= quality_to_probability(sti.get_quality());
      }
    }
    return product;
  }

}
