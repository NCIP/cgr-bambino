package TCGA;

import java.awt.Color;
import java.util.*;

public class AnnotationColorMapper2 {
  private Color max_color;
  private AnnotationFlatfile2 af;
  //  private static Hashtable maps_keyed = null;
  //  private static Hashtable maps_scale;
  private Hashtable maps_keyed = null;
  private Hashtable maps_scale;
  private int max_r, max_g, max_b;
  private HashSet integer_columns = null;

  private static HashSet undef_cell_values = init_undef_values();

  public static Color UNDEF_COLOR = Color.gray;

  public AnnotationColorMapper2 (AnnotationFlatfile2 af, Color max_color) {
    this.af = af;
    max_r = max_color.getRed();
    max_g = max_color.getGreen();
    max_b = max_color.getBlue();
    setup();
  }

  private void setup () {
    //
    //  scan annotation data to determine column formatting
    //

    integer_columns = new HashSet();
    maps_scale = new Hashtable();
    maps_keyed = new Hashtable();
    
    Vector rows = af.get_rows();
    Hashtable first = (Hashtable) rows.get(0);

    // foreach column, determine whether int or string
    // for string sets, determine if set of fixed values (YES/NO, etc)

    ArrayList columns = af.get_sorted_keys();
    
    for (int i=0; i<columns.size(); i++) {
      //
      // foreach column in flatfile...
      //
      String column = (String) columns.get(i);
      Funk.Str.trim_whitespace(column);
      if (column.length() == 0) continue;

      //      System.err.println("COLUMN at " +i + "=" + column);  // debug

      // scan row values...
      boolean is_integer = true;
      HashSet all_values = new HashSet();

      int v;
      int max_value = 0;
      for (int j = 0; j < rows.size(); j++) {
	Hashtable h = (Hashtable) rows.get(j);
	String value = (String) h.get(column);
	if (value != null) value = Funk.Str.trim_whitespace(value);
	if (is_usable_value(value)) {
	  all_values.add(value);
	  if (is_integer) {
	    try {
	      //	      v = Integer.parseInt(value);
	      v = Math.abs(Integer.parseInt(value));
	      if (v > max_value) max_value = v;
	    } catch (NumberFormatException e) {
	      //	      System.err.println("col " + column + " not int:" + value);  // debug
	      is_integer = false;
	    }
	  }
	}
      }
      
      //      System.err.println("Column " + column + ": integer?:" + is_integer);  // debug


      if (is_integer) {
	//	System.err.println("max value for " + column + " = " + max_value);  // debug
	integer_columns.add(column);
	maps_scale.put(column, new Float(1.0f / max_value));
      } else {
	Hashtable ht = null;

	ArrayList<String> unique = new ArrayList<String>(all_values);
	Collections.sort(unique);

	boolean yesno_only = true;
	for (String val : unique) {
	  String lc = val.toLowerCase();
	  if (!(lc.equals("yes") || lc.equals("no"))) {
	    yesno_only = false;
	    break;
	  }
	}

	ht = new Hashtable();

	if (yesno_only) {
	  for (String v2 : unique) {
	    ht.put(v2, v2.toLowerCase().equals("yes") ? 1.0f : 0f);
	  }
	} else {
	  float fv,step;
	  int size = unique.size();

	  if (size <= 1) {
	    // only one value, WTF?
	    fv = size;
	    step = size;
	  } else {
	    fv = 0;
	    step = 1.0f / (size - 1);
	  }
	  //	System.err.println("values:" + unique + " step:"+step);  // debug
	  for (int k=0; k < size; k++, fv += step) {
	    ht.put(unique.get(k), new Float(fv));
	    //	  System.err.println("key:"+unique.get(k) + " val:"+fv);  // debug
	  }
	}

	maps_keyed.put(column, ht);
      }
    }
  }

  public Color get_color (String field, String value) {
    Color result = UNDEF_COLOR;
    Hashtable map = (Hashtable) maps_keyed.get(field);

    if (value != null) value = Funk.Str.trim_whitespace(value);
    if (!is_usable_value(value)) {
      result = UNDEF_COLOR;
    } else if (map != null) {
      //
      //  fixed value based lookup
      // 
      Float modifier = (Float) map.get(value);
      if (modifier != null) {
	Float f = modifier.floatValue();
	int r = (int) (max_r * f);
	int g = (int) (max_g * f);
	int b = (int) (max_b * f);
	result = new Color(r,g,b);
      }
    } else {
      //
      //  scaled by (integer) value
      //
      Float f = (Float) maps_scale.get(field);
      if (f != null) {
	int r = 0;
	int g = 0;
	int b = 0;
	int v = 0;
	try {
	  if (value.equals("")) value = "0";
	  //	  v = Integer.parseInt(value);
	  v = Math.abs(Integer.parseInt(value));
	  Float multiplier = v * f;
	  r = (int) (max_r * multiplier);
	  g = (int) (max_g * multiplier);
	  b = (int) (max_b * multiplier);
	  result = new Color(r,g,b);
	} catch (Exception e) {
	  System.err.println("color mapping exception for field:" + field + " " + e + " val=" + v + " r=" + r + " g=" + g + " b="+b);  // debug
	}
      }
    }

    return result;
  }

  public static void main (String[] argv) {
    try {
      AnnotationFlatfile2 af = new AnnotationFlatfile2("gbm_sample_data.tab", false);
      AnnotationColorMapper2 acm = new AnnotationColorMapper2(af, Color.yellow);
    } catch (Exception e) {
      System.err.println(e);  // debug

    }
  }

  public void sort_samples_by (String field, GenomicMeasurement gm, AnnotationFlatfile2 af) {
    // 
    //  sort GenomicMeasurement samples by specified field.
    //

    //
    // FIX ME: keep even samples with UNUSABLE data together!!!
    // 

    boolean is_integer = integer_columns.contains(field);

    //
    //  get list of unique values observed for this field:
    //
    ArrayList<GenomicSample> gm_rows = gm.get_rows();
    System.err.println("FIX ME: annotation sort; visible rows???");  // debug
    HashSet unique_values = new HashSet();
    ArrayList<GenomicSample> invalid_samples = new ArrayList<GenomicSample>();
    HashMap<String,ArrayList> bucket = new HashMap<String,ArrayList>();

    for (GenomicSample gs : gm_rows) {
      ArrayList annot = af.find_annotations(gs);
      if (annot == null) {
	invalid_samples.add(gs);
      } else {
	Hashtable ht = (Hashtable) annot.get(0);
	String value = (String) ht.get(field);
	if (is_usable_value(value)) {
	  if (is_integer) {
	    // use Integer objects for correct sorting of numeric values
	    unique_values.add(new Integer(Integer.parseInt(value)));
	  } else {
	    unique_values.add(value);
	  }

	  ArrayList<GenomicSample> b2 = bucket.get(value);
	  if (b2 == null) bucket.put(value, b2 = new ArrayList<GenomicSample>());
	  b2.add(gs);
	  // bucket sample rows by value

	} else {
	  invalid_samples.add(gs);
	}
      }
    }

    //
    //  sort unique values
    //    
    ArrayList sorted = new ArrayList(unique_values);
    Collections.sort(sorted);
    
    //
    //  list of sorted buckets, including unsortable (without annotation)
    //
    ArrayList<GenomicSample> results = new ArrayList<GenomicSample>();
    for (int i = 0; i < sorted.size(); i++) {
      String key = sorted.get(i).toString();
      ArrayList<GenomicSample> list = bucket.get(key);
      results.addAll(list);
    }
    results.addAll(invalid_samples);

    gm.set_rows(results, true);
  }

  public static boolean is_usable_value (String value) {
    return (value != null &&
	    value.length() > 0 && 
	    !undef_cell_values.contains(value.toUpperCase()));
  }

  private static HashSet init_undef_values() {
    HashSet undef = new HashSet();
    undef.add("-");
    undef.add("NASS");
    undef.add("UNKNOWN");
    // 5/2008 annotations

    undef.add("NOT COMPUTABLE");
    undef.add("NULL");
    // 2009 Ovarian

    undef.add("null");
    // 2011: BRCA 
    
    return undef;
  }

  public static HashSet get_undef_values() {
    return undef_cell_values;
  }


    
}
