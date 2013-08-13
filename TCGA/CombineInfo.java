package TCGA;

import java.util.*;

public class CombineInfo {
  public static final String TAG_PREFIX = "combine_resource_";
  // all tags for this set start w/this prefix
  // FIX ME: generic class for this

  public static final String TAG_LABEL = TAG_PREFIX + "label";
  public static final String TAG_URL = TAG_PREFIX + "url";
  
  private HashMap<String,String> values;

  public CombineInfo () {
    values = new HashMap<String,String>();
  }

  public void add (String label, String value) {
    if (label.equals(TAG_LABEL) ||
	label.equals(TAG_URL)) {
      values.put(label, value);
      //      System.err.println("combine " + label + " " + value);  // debug
    } else {
      System.err.println("WARNING: unknown tag " + label);  // debug
    }
  }

  public String get_label () {
    return values.get(TAG_LABEL);
  }

  public String get_url () {
    return values.get(TAG_URL);
  }
  

}