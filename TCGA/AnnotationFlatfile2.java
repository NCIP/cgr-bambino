package TCGA;
// second, simpler format

import java.util.*;
import java.io.*;

import Funk.DelimitedFile;

public class AnnotationFlatfile2 extends Observable implements Runnable {

  private boolean is_loaded = false;
  private InputStream input_stream = null;
  private Vector rows = null;
  private ArrayList<String> filtered_columns = null;
  private DelimitedFile df;
  private String local_filename = null;
  private InputStream ins = null;
  private Exception parse_exception = null;
  private String barcode_column = null;

  private ArrayList sorted_keys = null;
  private HashMap<String,ArrayList> by_barcode;

  private ArrayList<CommentSection> sections = null;
  private HashMap<String,String> column2section = null;
  
  public AnnotationFlatfile2 (String fn) throws Exception {
    local_filename=fn;
    new Thread(this).start();
  }
  
  public AnnotationFlatfile2 (String fn, boolean async) throws Exception {
    local_filename = fn;
    if (async) {
      new Thread(this).start();
    } else {
      run();
    }
  }

  public AnnotationFlatfile2 (InputStream ins, boolean async) throws Exception {
    this.ins = ins;
    if (async) {
      new Thread(this).start();
    } else {
      run();
    }
  }

  public AnnotationFlatfile2 (InputStream ins) throws Exception {
    this.ins = ins;
    new Thread(this).start();
  }

  public boolean is_loaded () {
    return is_loaded;
  }

  public Vector get_rows () {
    return rows;
  }

  public ArrayList get_sorted_keys () {
    if (sorted_keys == null && is_loaded) {
      sorted_keys = new ArrayList(filtered_columns);
      Collections.sort(sorted_keys);
    }
    return sorted_keys;
  }

  public Exception get_parse_exception() {
    return parse_exception;
  }

  public int get_row_count () {
    return rows == null ? -1 : rows.size();
  }

  public void run () {
    //
    //  load/parse annotations in background
    //
    //    try {Thread.sleep(5000);} catch (Exception e) {}
    df = null;
    parse_exception = null;
    try {
      df = new DelimitedFile();
      df.set_filter_unix_comments(true);
      df.set_trim_whitespace(true);
      df.set_standardize_dates(true);
      //      df.set_standardize_labels(true);
      // NO: breaks linkage between columns and annotation section names
      df.set_standardize_labels(false);

      if (local_filename != null) {
	df.parse(local_filename);
      } else if (ins != null) {
	df.parse(ins);
      } else {
	System.err.println("ERROR: no annotation flatfile");  // debug
	System.exit(1);
      }
    } catch (Exception e) {
      parse_exception = e;
      System.err.println(e);  // debug
    }

    if (parse_exception == null) {
      rows = df.get_rows();
      filtered_columns = column_setup();
      section_setup();
    } else {
      filtered_columns = new ArrayList<String>();
    }

    is_loaded = true;
    setChanged();
    notifyObservers();
  }

  private ArrayList<String> column_setup() {
    HashMap<String,HashSet<String>> col_values = new HashMap<String,HashSet<String>>();
    // track unique values for each column
    ArrayList<String> columns = new ArrayList<String>();
    for (Object o : df.get_labels()) {
      String col = (String) o;
      col_values.put(col, new HashSet<String>());
      columns.add(col);
    }
    for (Object r : rows) {
      Hashtable row = (Hashtable) r;
      for (String col : columns) {
	String v = (String) row.get(col);
	if (v == null) v = "";
	col_values.get(col).add(v);
      }
    }

    HashSet undefs = AnnotationColorMapper2.get_undef_values();

    ArrayList<String> rejected = new ArrayList<String>();

    for (String col : columns) {
      HashSet<String> bucket = col_values.get(col);
      if (bucket.size() == 1) {
	// only one value ever observed
	String value = "";
	for (String x : bucket) {
	  value = x;
	}
	if (undefs.contains(value)) {
	  System.err.println("rejecting static annotation column " + col + ": only value is " + value);  // debug
	  rejected.add(col);
	} else {
	  System.err.println("keeping single-value column " + col + ": value is \"" + value+ "\"");  // debug
	}
      } else {
	//	System.err.println("col ok: " + col );  // debug
      }
    }

    columns.removeAll(rejected);

    //    System.err.println("columns="+columns);  // debug

    return columns;
  }

  public String[] get_annotation_columns() {
    
    ArrayList<String> use = new ArrayList<String>();
    use.addAll(filtered_columns);
    use.remove(barcode_column);

    String[] results = new String[use.size()];
    for (int i = 0; i < use.size(); i++) {
      results[i] = use.get(i);
    }
    return results;
  }
  
  public void index_annotations (GenomicMeasurement gm) {
    // detect patient ID column in annotations using patient IDs from
    // currently-loaded dataset, then index annotations by patient ID

    HashSet<String> patient_ids = new HashSet<String>(gm.get_visible_sample_ids());
    //    System.err.println("pids="+patient_ids);  // debug

    Vector columns = df.get_labels();
    barcode_column = null;
    
    for (int ri=0; ri < rows.size(); ri++) {
      // search each row for a column containing a patient ID
      // (might not be in first row -- e.g. Rembrandt annotation file!)

      Hashtable row = (Hashtable) rows.get(ri);

      for (int i = 0; i < columns.size(); i++) {
	String column = (String) columns.get(i);
	String value = (String) row.get(column);
	if (value != null && patient_ids.contains(value)) {
	  barcode_column = column;
	  //	  System.err.println("patient ID column="+column);  // debug
	  break;
	}
      }
      if (barcode_column != null) break;
    }

    if (barcode_column != null) {
      by_barcode = build_index(barcode_column);
    } else {
      System.err.println("ERROR: can't detect sample ID column in annotation file!");  // debug
    }
  }

  private HashMap build_index (String field) {
    Enumeration e = rows.elements();
    Hashtable r;
    String v;
    ArrayList al;
    HashMap map = new HashMap();
    while (e.hasMoreElements()) {
      r = (Hashtable) e.nextElement();
      v = (String) r.get(field);
      al = (ArrayList) map.get(v);
      if (al == null) {
	al = new ArrayList();
	map.put(v, al);
      }
      al.add(r);
    }
    return map;
  }

  public ArrayList find_annotations (GenomicSample gs) {
    // try to find annotations, trying by aliquot, analyte and sample
    return by_barcode.get(gs.patient_id);
  }

  public static void main (String [] argv) {
    try {
      AnnotationFlatfile2 af = new AnnotationFlatfile2("gbm_sample_data.tab", false);
      GenomicSample gs = new GenomicSample();
      gs.patient_id = "TCGA-02-0116";
      ArrayList al = af.find_annotations(gs);
      System.err.println("found: " + al);  // debug
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      new Exception().printStackTrace();
    }
  }

  private void section_setup() {
    ArrayList<String> comments = df.get_unix_comments();
    if (comments != null && comments.size() > 0) {
      for (String comment : comments) {
	String[] chunks = comment.split("=");
	if (chunks.length == 2) {
	  if (chunks[0].equals("#section")) {
	    if (sections == null) sections = new ArrayList<CommentSection>();
	    //	    System.err.println("section");  // debug
	    sections.add(new CommentSection(chunks[1].split(",")));
	  } else {
	    System.err.println("ERROR: " + chunks[0]);  // debug
	  }
	}
      }
    }

    if (sections != null) {
      column2section = new HashMap<String,String>();
      for (CommentSection cs : sections) {
	for (String label : cs.labels) {
	  //	  System.err.println("map " + label + " => " + cs.section_name);  // debug
	  column2section.put(label, cs.section_name);
	}
      }
    }


  }

  public HashMap<String,String> get_column2section() {
    return column2section;
  }

  
}
