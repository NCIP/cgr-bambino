package TCGA;
// track GISTIC data
// MNE 3/2009

import java.util.*;
import java.math.BigDecimal;

public class GISTIC {
  private GenomicMeasurement gm;

  private static boolean INVERT = true;
  private static boolean USE_LOG10_SCALE = true;
  private static int LOG_SCALE_LIMIT = (int) ((SummaryPanelCNSI.IMAGE_HEIGHT - 1) / 2);
  // in log-scaling mode, maximum precision value to allow
  // (should be related to # of pixels available for display)
  // 
  // FIX ME: maybe better to determine this dynamically???
  // i.e. some percentile of observed values, and also capped to screen dynamic range?
  // (could be a lower value if distribution values are lower)

  public static final String GISTIC_TAG = "gistic_data";
  public static final String TYPE_AMPLIFICATION = "+";
  public static final String TYPE_DELETION = "-";
  
  public static int VERBOSITY = 1;

  private static final int PEAK_MAPPING_BIN_FUDGE_COUNT = 5;


  private static ArrayList<String> DATA_TYPES;

  private HashMap<String,HashMap<String,GISTICPeak>> sym2peak;
  // category -> symbol -> peak

  private HashMap <String,ArrayList<BigDecimal>> q_cooked;
  // these contain GISTIC q-values in header array order

  private HashMap <String,ArrayList<GISTICPeak>> peaks_ordered;
  // category -> ordered peaks

  private HashSet<String> all_subtypes;
  private ArrayList<String> all_subtypes_sorted;

  private BigDecimal max_value;
  // this is a cooked/display value

  private CommentOptions co;
  private ArrayList<GISTICPeak> peaks;

  public GISTIC (CommentOptions co) {
    this.co = co;
    sym2peak = new HashMap<String,HashMap<String,GISTICPeak>>();
    // map (data subtype + data type) ==> GISTICPeak
    q_cooked = new HashMap <String,ArrayList<BigDecimal>>();
    peaks_ordered = new HashMap<String,ArrayList<GISTICPeak>>();

    all_subtypes = new HashSet();

    if (DATA_TYPES == null) {
      // singleton
      DATA_TYPES = new ArrayList<String>();
      DATA_TYPES.add(TYPE_AMPLIFICATION);
      DATA_TYPES.add(TYPE_DELETION);
    }

    peaks = new ArrayList<GISTICPeak>();
    max_value = new BigDecimal(0);
  }

  private String get_key (String sub_type, String data_type) {
    return sub_type + "_" + data_type;
  }

  private synchronized ArrayList<BigDecimal> get_arraylist (String key, HashMap<String,ArrayList<BigDecimal>> hash) {
    ArrayList<BigDecimal> result = hash.get(key);
    if (result == null) hash.put(key, result = new ArrayList<BigDecimal>());
    return result;
  }

  private synchronized ArrayList<GISTICPeak> get_arraylist2 (String key, HashMap<String,ArrayList<GISTICPeak>> hash) {
    ArrayList<GISTICPeak> result = hash.get(key);
    if (result == null) hash.put(key, result = new ArrayList<GISTICPeak>());
    return result;
  }

  private HashMap<String,GISTICPeak> get_sym2peak (String sub_type, String data_type) {
    String key = get_key(sub_type, data_type);
    HashMap<String,GISTICPeak> result = sym2peak.get(key);
    if (result == null) sym2peak.put(key, result = new HashMap<String,GISTICPeak>());
    return result;
  }

  public void add (String s) {
    //
    // initial parsing: bucket q values by marker symbol, find max bounds for amplification/deletion
    //
    String[] f = s.split(",");

    GISTICPeak gp = new GISTICPeak(s);
    peaks.add(gp);
    
    all_subtypes.add(gp.sub_type);

    if (DATA_TYPES.contains(gp.data_type)) {
      HashMap<String,GISTICPeak> g2p = get_sym2peak(gp.sub_type, gp.data_type);
      for (String marker : gp.markers) {
	g2p.put(marker, gp);
      }

      if (USE_LOG10_SCALE) {
	if (INVERT) {
	  BigDecimal invert_log = new BigDecimal(gp.q.scale());
	  if (invert_log.compareTo(max_value) > 0) max_value = invert_log;
	} else {
	  System.err.println("ERROR: noninverted log10");  // debug
	}
      } else {
	if (gp.q.compareTo(max_value) > 0) max_value = gp.q;
      }
    } else {
      System.err.println("GISTIC parse error: unknown type " + f[0]);  // debug
    }
  }

  public void header_setup(GenomicMeasurement gm) {
    String[] headers = gm.get_headers();
    this.gm = gm;

    all_subtypes_sorted = new ArrayList<String>(all_subtypes);
    Collections.sort(all_subtypes_sorted);
    //    System.err.println("REVERSING\n");
    //    Collections.reverse(all_subtypes_sorted);
    
    if (USE_LOG10_SCALE) {
      //      System.err.println("max = " + max_value);  // debug
      BigDecimal gate = new BigDecimal(LOG_SCALE_LIMIT);
      if (max_value.compareTo(gate) > 0) max_value = gate;
      //      System.err.println("max2="+max_value);  // debug
      // need to rebuild if using log10
      // log10 = - value.scale()
      if (!INVERT) {
	System.err.println("ERROR: uninverted log10 scale not implemented");  // debug
	System.exit(1);
      }
    }

    //    for (String key : sym2q.keySet()) {
    GISTICPeak peak;

    for (String key : sym2peak.keySet()) {
      //
      //  for each source subtype and datatype (mapped by marker name),
      //  arrange data in header ordering
      //
      //      HashMap<String,BigDecimal> from = sym2q.get(key);
      HashMap<String,GISTICPeak> from = sym2peak.get(key);
      ArrayList<BigDecimal> to_cooked = get_arraylist(key, q_cooked);
      ArrayList<GISTICPeak> binned = get_arraylist2(key, peaks_ordered);

      int i,j;

      for (i = 0; i < headers.length; i++) {
	if (headers[i].indexOf(",") != -1) {
	  // genomic mode: comma-delimited headers contain multiple markers per bin.
	  String[] h = headers[i].split(",");
	  //	  System.err.println("set of " + h.length);  // debug
	  peak = null;
	  for (j=0; j < h.length; j++) {
	    peak = from.get(h[j]);
	    if (peak != null) break;
	    // **** FIX ME: ****
	    // what if multiple different entries for different symbols??
	  }
	  //	  System.err.println("");  // debug
	} else {
	  peak = from.get(headers[i]);
	}
	
	if (peak == null) {
	  to_cooked.add(null);
	  binned.add(null);
	} else {
	  to_cooked.add(peak.q_cooked = cook_value(peak.q));
	  binned.add(peak);
	}
      }
    }

    //    gene_fill_hack(gm);
    // DISABLED until later when GenomicSet is available so can
    // limit inappropriate filling across chromosomes
  }

  public void bin_infill (GenomicSet gs) {
    gene_fill_hack(gs);
  }

  private BigDecimal cook_value (BigDecimal v) {
    BigDecimal result = null;
    BigDecimal zero = new BigDecimal(0);
    if (INVERT && v != null) {
      if (USE_LOG10_SCALE) {
	int v2;
	if (v.equals(zero)) {
	  // display q-values of 0 as highest/best value
	  v2 = LOG_SCALE_LIMIT;
	} else {
	  v2 = v.scale();
	// scale() is inverted log10 value
	  if (v2 > LOG_SCALE_LIMIT) v2 = LOG_SCALE_LIMIT;
	}
	//	System.err.println("scaling " + v + " => " + v2);  // debug
	result = new BigDecimal(v2);
      } else {
	result = max_value.subtract(v);
      }
    } else if (USE_LOG10_SCALE && !INVERT) {
      System.err.println("ERROR: non-inverted log10 scale not implemented");  // debug
      System.exit(1);
    }
    return result;
  }

  public BigDecimal get_max_value() {
    return max_value;
  }

  public ArrayList<BigDecimal> get_display_data (String sub_type, String data_type) {
    // return (possibly cooked) array data in header ordered format
    // could generate dynamically from ordered GISTICPeak data, but this is faster
    return get_arraylist(get_key(sub_type, data_type), q_cooked);
  }

  public BigDecimal get_raw_value (String sub_type, String data_type, int x) {
    // return raw peak quality for a bin
    ArrayList<GISTICPeak> v = get_arraylist2(get_key(sub_type, data_type), peaks_ordered);
    BigDecimal result = null;
    if (x < v.size()) {
      GISTICPeak p = v.get(x);
      result = p == null ? null : p.q;
    }
    return result;
  }

  public GISTICPeak get_peak (String sub_type, String data_type, int x) {
    // return GISTICPeak for a bin
    ArrayList<GISTICPeak> v = get_arraylist2(get_key(sub_type, data_type), peaks_ordered);
    GISTICPeak result = null;
    if (x < v.size()) result = v.get(x);
    return result;
  }

  public ArrayList<String> get_all_subtypes() {
    return all_subtypes_sorted;
  }
  
  public ArrayList<String> get_data_types() {
    return DATA_TYPES;
  }

  public String get_paint_priority() {
    return co.get("gistic_paint_priority");
  }

  private void gene_fill_hack(GenomicSet gs) {
    // for each GISTIC peak, find equivalent region in heatmap using
    // first and last mappable genes in region.  Then populate gaps
    // in coverage due to nonstandard bin/gene names (e.g. LOC*).

    String[] headers = gm.get_headers();
    BinIndex bi = new BinIndex(gm);
    int NULL = -1;

    ArrayList<GenomicBin> layout_bins = gs.get_bins();
    //
    //  map bin numbers to chromosomes:
    //
    ArrayList<String> bin2chr = new ArrayList<String>();
    int i;
    for (GenomicBin bin : layout_bins) {
      int needed = bin.end - bin2chr.size();
      for (i = 0; i < needed; i++) {
	// resize
	bin2chr.add("");
      }
      
      String chr = bin.bin_name;
      if (chr.indexOf("chr") != 0) {
	System.err.println("ERROR: bin " + chr + " not in UCSC chr format");  // debug
      }
      for (i = bin.start - 1; i < bin.end; i++) {
	// 0-based index
	//	System.err.println("set " + i);  // debug
	bin2chr.set(i, chr);
      }
      //      System.err.println("bin: " + bin.bin_name + " " + bin.start + " + " + bin.end);  // debug
    }

    for (GISTICPeak gp : peaks) {
      // foreach GISTIC peak
      String peak_chr = gp.get_chromosome();

      GenomicBinRange gbr = gs.find_bins_for(gp.peak_pos);

      int mc = gp.markers.size();

      HashSet<Integer> bin_set = new HashSet<Integer>();
      //      System.err.println("peak:" + gp.peak_pos);  // debug

      int ix;
      for (String marker : gp.markers) {

	if (marker.indexOf("[") == 0 &&
	    marker.indexOf("]") == marker.length() - 1) {
	  // GISTIC2: peak doesn't overlap a gene directly,
	  // the nearest gene symbol is shown in square brackets.
	  marker = marker.substring(1, marker.length() - 1);
	  // in this case the neighboring gene will be used for placement,
	  // the tooltip will still show the name in brackets.
	}

	ix = bi.find(marker);
	//	System.err.println(gp.markers.get(i) + " " + ix);  // debug
	if (ix != NULL) {
	  String bin_chr = bin2chr.get(ix);
	  //	  System.err.println("bin for " + marker + " = " + ix + " bin_chr:" + bin_chr + " peak_chr:" + gp.peak_pos.chromosome);  // debug
	  boolean ok = true;

	  if (!bin_chr.equals(gp.peak_pos.chromosome)) {
	    ok = false;
	  }
	  
	  if (gbr != null) {
	    if (!gbr.contains(ix)) {
	      // bin not within set calculated for GISTIC-reported peak
	      int distance = gbr.get_distance(ix);
	      if (distance > PEAK_MAPPING_BIN_FUDGE_COUNT) {
		// tolerate a small amount of disagreement.
		// large disagreements result in inappropriate spanning
		// (ambiguous mappings, etc.) and are suppressed.
		if (VERBOSITY >= 2) System.err.println("not mapping GISTIC hit, bin distance skew=" + distance);
		ok = false;
	      }
	    }
	  }

	  if (ok) bin_set.add(ix);

	}
      }
      ArrayList<Integer> bins = new ArrayList<Integer>(bin_set);
      Collections.sort(bins);
      //      System.err.println("bins="+bins);  // debug
      
      int si = NULL;
      int ei = NULL;

      if (bins.size() > 0) {
	si = bins.get(0);
	ei = bins.get(bins.size() - 1);
      }
      //      System.err.println("si="+si + " ei="+ei);  // debug

      if (si != NULL && ei != NULL) {
	//	System.err.println("si:"+si + "/" + headers[si] + " ei:" +ei + "/" + headers[ei] + " q=" + gp.q + " next="+ headers[ei+1]);  // debug
	String key = get_key(gp.sub_type, gp.data_type);

	ArrayList<BigDecimal> to_cooked = get_arraylist(key, q_cooked);
	ArrayList<GISTICPeak> binned = get_arraylist2(key, peaks_ordered);

	BigDecimal cooked_val = cook_value(gp.q);

	for (i=si; i<=ei; i++) {
	  if (to_cooked.get(i) == null) {
	    //	    System.err.println("patching in " + cooked_val + " for " + headers[i] + " raw="+gp.q);  // debug
	    to_cooked.set(i, cooked_val);
	    binned.set(i, gp);
	  }
	}
      } else {
	if (VERBOSITY >= 1) System.err.println("ERROR: can't map GISTIC peak to bins for " + gp.markers);
      }

    }
    
  }
  

}