package TCGA;

import java.util.*;

public class FuzzyTCGAIDMatcher {
  // TCGA-01-0234-56     -versus-
  // TCGA-01-0234-56-78
  //
  // TCGA-01-0234-56-78  -versus-
  // TCGA-01-0234-56
  //
  // search for smaller of the two
  int field_count_from, field_count_to;
  boolean exact_match_mode;

  static boolean TRIM_TCGA_VIAL_ID = true;

  public FuzzyTCGAIDMatcher (int field_count_from, int field_count_to) {
    setup(field_count_from, field_count_to);
  }

  public FuzzyTCGAIDMatcher (String field_from, String field_to) {
    //    System.err.println("from:"+field_from + " to:"+field_to);  // debug
    String[] ff = field_from.split("-");
    String[] ft = field_to.split("-");
    setup(ff.length, ft.length);
    if (field_from.indexOf("TCGA") != 0 || field_to.indexOf("TCGA") != 0) exact_match_mode = true;
    // always perform exact string match for non-TCGA identifiers
  }

  private void setup (int field_count_from, int field_count_to) {
    this.field_count_from = field_count_from;
    this.field_count_to = field_count_to;
    exact_match_mode = field_count_from == field_count_to;
  }

  public boolean matches (String from_id, String to_id) {
    //    System.err.println("matches " + from_id + " vs " + to_id);  // debug
    boolean result = false;
    if (exact_match_mode) {
      result = from_id.equals(to_id);
    } else if (field_count_from > field_count_to) {
      result = from_id.indexOf(trim_check(to_id, field_count_to)) == 0;
    } else {
      result = to_id.indexOf(trim_check(from_id, field_count_from)) == 0;
    }
    return result;
  }

  public String trim_check (String id, int field_count) {
    String result = id;
    if (TRIM_TCGA_VIAL_ID && field_count == 4) {
      String[] f = id.split("-");
      if (f[3].length() > 2) {
	// strip vial ID code
	result = id.substring(0, id.length() - 1);
	//	System.err.println("hey now " + id + " => " + result);
      }
    }
    return result;
  }
  
  public static void main (String[] argv) {
    ArrayList<String> from_ids = new ArrayList<String>();
    ArrayList<String> to_ids = new ArrayList<String>();

     from_ids.add("TCGA-29-1688-01A-01D");
     to_ids.add("TCGA-12-3456-78B");
     to_ids.add("TCGA-29-1688-01A");

    //    from_ids.add("TCGA-29-1688-01A");
    //    to_ids.add("TCGA-12-3456-78B");
    //    to_ids.add("TCGA-29-1688-01A");

    //    to_ids.add("TCGA-29-1688-01A-01D");
    //    from_ids.add("TCGA-12-3456-78B");
    //    from_ids.add("TCGA-29-1688-01A");

    FuzzyTCGAIDMatcher m = new FuzzyTCGAIDMatcher(from_ids.get(0), to_ids.get(0));

    for (String id_from : from_ids) {
      for (String id_to : to_ids) {
	if (m.matches(id_from, id_to)) {
	  System.err.println(id_from + " matches " + id_to);  // debug
	}
      }
    }
  }

}