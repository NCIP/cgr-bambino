package TCGA;
import java.util.*;
import java.math.BigDecimal;

public class GISTICPeak {
  public String sub_type, data_type;
  public GenomicLocation peak_pos;
  public ArrayList<String> markers;
  public BigDecimal q, z_score;
  public BigDecimal q_cooked = null;

  private static final String MAGIC_ZSCORE = "_Zsc";

  public GISTICPeak (String s) {
    String[] f = s.split(",");
    z_score = null;

    if (f.length >= 5) {
      // 0. subset label (e.g. "broad", "focal")
      // 1. "+" (amplification) or "-" (deletion)
      // 2. GISTIC q-value
      // 3. genomic position of GISTIC peak
      // [revision: optional tag/value pairs here...]
      // 4. (...) marker list
      sub_type = f[0];
      data_type = f[1];
      q = new BigDecimal(f[2]);
      peak_pos = new GenomicLocation(f[3]);

      if (sub_type == null) sub_type = "";
      markers = new ArrayList<String>();
      for (int i=4; i < f.length; i++) {
	// gene symbols and possibly key/value pairs (retrofit)
	String v = new String(f[i]);
	if (v.equals(MAGIC_ZSCORE)) {
	  // key/value pair for z-score
	  z_score = new BigDecimal(f[++i]);
	} else {
	  markers.add(v);
	}
      }
    } else {
      // FIX ME: throw exception here (in a hurry now)
      System.err.println("ERROR: gistic peak must be at least 5");  // debug
    }
  }

  public String get_chromosome() {
    return peak_pos.chromosome;
  }

}

