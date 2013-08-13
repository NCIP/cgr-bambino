// given a Vector of Numbers,
// produce mean, median, variance, standard deviation.
//
// NOTE: given vector will be sorted numerically!
// FIX ME:
//  - sort/calculate median only if we NEED median
//  - breaks if given a vector of size < 2

package Funk;

import java.util.*;

public class Stats {

  private double mean,median,variance,standard_deviation;

  public Stats (int[] set) {
    ArrayList al = new ArrayList(set.length);
    for (int i = 0; i < set.length; i++) {
      al.add(new Integer(set[i]));
    }
    setup(al);
  }

  //  public Stats (Vector v) {
  //    setup(v);
  //  }

  public Stats (List l) {
    setup(l);
  }

  private void setup (List l) {
    Collections.sort(l);
    // FIX ME: this sorts the ORIGINAL LIST!

    int count = l.size();

    double total = 0;
    for (int i = 0; i < count; i++) {
      total += ((Number) l.get(i)).doubleValue();
    }
    mean = total / count;
    median = ((Number) l.get(count / 2)).doubleValue();
    
    double squared_diffs = 0;
    for (int i = 0; i < count; i++) {
      squared_diffs += 
	Math.pow(mean - ((Number) l.get(i)).doubleValue(), 2);
    }
    variance = squared_diffs / count;
    // variance is the mean of the sum of the squared differences from the mean

    standard_deviation = Math.sqrt(variance);

    if (false) {
      // debug values
      System.err.println("items:" + count);
      for (int i = 0; i < count; i++) {
	System.err.println("  " + ((Number) l.get(i)).doubleValue());  // debug
      }    
      System.err.println(
			 " mean:" + mean + " median:" + median +
			 " variance:" + variance + 
			 " stddev:" + standard_deviation);  // debug
    }
  }

  public double mean () {
    return mean;
  }

  public double median () {
    return median;
  }

  public double variance () {
    return variance;
  }

  public double standard_deviation () {
    return standard_deviation;
  }

  public static void main (String[] argv) {
    int[] values = {1,2,3,4,10,20,300};
    Stats s = new Stats(values);
  }

  public double get_95_percent_confidence_interval (boolean above) {
    // http://bmj.bmjjournals.com/collections/statsbk/4.shtml
    //
    // We have seen that when a set of observations have a Normal
    // distribution multiples of the standard deviation mark certain
    // limits on the scatter of the observations. For instance, 1.96
    // (or approximately 2) standard deviations above and 1.96
    // standard deviations below the mean mark the points within
    // which 95% of the observations lie.
    double delta = standard_deviation * 1.96;
    if (above == false) delta *= -1;
    return mean + delta;
  }


}
