package TCGA;

import java.util.*;

public class AgilentBlock {
  private String name;
  private String[] values;
  private HashMap<String,Integer> index;

  public AgilentBlock (String[] values) {
    name = new String(values[0]);
    this.values = new String[values.length - 1];
    for (int i = 1; i < values.length; i++) {
      this.values[i - 1] = new String(values[i]);
    }
  }

  public String get_name() {
    return name;
  }

  public String[] get_values() {
    return values;
  }

  public String get (int index) {
    return values[index];
  }

  public void build_index() {
    index = new HashMap<String,Integer>();
    for (int i = 0; i < values.length; i++) {
      index.put(values[i], new Integer(i));
    }
  }
  
  public int get_index_of (String field) {
    int result = -1;
    if (index.containsKey(field)) {
      result = index.get(field).intValue();
    }
    return result;
  }

  public void report (AgilentBlock ab_headers) {
    // report data for this block, given header block
    String[] headers = ab_headers.get_values();
    for (int i = 0; i < headers.length; i++) {
      System.err.println(headers[i] + ": " + values[i]);  // debug
    }
    System.err.println("");  // debug
    
  }

}
