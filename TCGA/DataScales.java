package TCGA;

import java.util.*;

public class DataScales {
  private ArrayList<DataScale> scales;
  
  public DataScales() {
    scales = new ArrayList<DataScale>();
  }

  public void add(DataScale ds) {
    scales.add(ds);
  }

  public String translate_amount (int amount) {
    DataScale ds = get_scale_for(amount);
    String result = ds.lower_comparator + " " + ds.lower_bound;
    if (ds.upper_bound != null) result = result.concat(" and " + ds.upper_comparator + " " +ds.upper_bound);
    return result;
  }

  public DataScale get_scale_for(int amount) {
    DataScale result = null;
    for (DataScale ds : scales) {
      if (ds.value == amount) result = ds;
    }
    return result;
  }

}

