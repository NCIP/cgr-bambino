package TCGA;

import java.util.*;

public class BinIndex {
  private GenomicMeasurement gm;
  private HashMap<String,Integer> header2index;
  private HashMap<String,Integer> header2index_lc;
  private ArrayList<String> unique_sorted;
  private boolean is_multi_bin = false;

  public BinIndex (GenomicMeasurement gm) {
    // GenomicMeasurement must be loaded already
    is_multi_bin = false;
    this.gm=gm;
    String[] headers = gm.get_headers();
    header2index = new HashMap<String,Integer>();
    header2index_lc = new HashMap<String,Integer>();

    //
    //  index marker/gene names in bin labels
    //
    for (int i = 0; i < headers.length; i++) {
      String header = headers[i];
      Integer here = new Integer(i);
      if (header.indexOf(",") > 0) {
	is_multi_bin = true;
	String[] things = header.split(",");
	// 3/21/08: Carl's comma-delimited lists of genes
	for (int j = 0; j < things.length; j++) {
	  header2index.put(things[j], here);
	  header2index_lc.put(things[j].toLowerCase(), here);
	}
      } else {
	header2index.put(header, here);
	header2index_lc.put(header.toLowerCase(), here);
      }
    }

    unique_sorted = new ArrayList<String>(header2index.keySet());
    Collections.sort(unique_sorted);
  }

  public int find (String name) {
    Integer index = header2index.get(name);
    if (index == null) index = header2index_lc.get(name.toLowerCase());
    // attempt verbatim lookup first, then case-insensitive lookup
    return index == null ? -1 : index.intValue();
  }

  public boolean is_multi_bin() {
    return is_multi_bin;
  }

  public ArrayList<String> get_unique_list() {
    return unique_sorted;
  }
  
}

