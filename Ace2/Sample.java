package Ace2;

import java.util.*;
import java.util.regex.*;

public class Sample {
  SampleMatchInfo smi = null;

  private static HashMap<String,Sample> samples;
  // for singletons

  public Sample() {
    smi = new SampleMatchInfo();
  }

  public Sample (String sample_id) {
    smi = new SampleMatchInfo();
    set_sample_id(sample_id);
  }

  public void set_sample_id (String sample_id) {
    parse_id(sample_id);
  }
  
  private void parse_id(String sample_id) {
    boolean matches_convention = false;
    for (SampleNamingConvention snc : SampleNamingConvention.get_known_conventions()) {
      if (snc.matches(sample_id)) {
	System.err.println(sample_id + " matches naming convention: " + snc.convention_name);  // debug
	smi = snc.get_match_info();
	matches_convention = true;
	System.err.println("  sample_name: " + smi.sample_name);  // debug
	System.err.print(  "           tn: " + smi.tumor_normal);
	if (smi.tumor_normal.equals(TumorNormal.TUMOR)) {
	  System.err.print(" (recurrent: " + smi.is_recurrent + ")");  // debug
	}
	System.err.println("");  // debug

	System.err.println("         desc: " + smi.description);
	System.err.println("    recurrent: " + smi.is_recurrent);
	break;
      }
    }
    if (!matches_convention) smi.sample_name = sample_id;
  }

  public static Sample get_sample (String sample_id) {
    // create or return singleton
    if (samples == null) samples = new HashMap<String,Sample>();
    Sample result = samples.get(sample_id);
    if (result == null) samples.put(sample_id, result = new Sample(new String(sample_id)));
    // create new String to avoid memory leak in split() buffer, etc.
    return result;
  }

  public String get_sample_name() {
    return smi == null ? null : smi.sample_name;
  }

  public boolean is_normal () {
    return smi != null && smi.tumor_normal != null && smi.tumor_normal.equals(TumorNormal.NORMAL);
  }

  public boolean is_tumor () {
    return smi != null && smi.tumor_normal != null && smi.tumor_normal.equals(TumorNormal.TUMOR);
  }

  public String get_description () {
    return smi.description;
  }

  public void reset_tumor_normal() {
    smi = new SampleMatchInfo();
  }

  public void set_normal (boolean v) {
    smi.tumor_normal = TumorNormal.NORMAL;
  }

  public void set_tumor (boolean v) {
    if (smi == null) smi = new SampleMatchInfo();
    smi.tumor_normal = TumorNormal.TUMOR;
  }

  public TumorNormal get_tumornormal() {
    return smi.tumor_normal;
  }

  public boolean is_recurrent() {
    return smi.is_recurrent;
  }

}
