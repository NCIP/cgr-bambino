package TCGA;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Cursor;

public class GenomicMeasurement extends Observable implements Runnable,Cloneable {

  private String[] headers;
  private ArrayList<GenomicSample> rows,rows_raw,visible_rows,visible_rows_raw;
  //  private int[] abs_counts;

  private boolean is_loaded = false;
  private boolean is_binary = false;
  private boolean combined = false;

  private Exception parse_error = null;
  //  private BufferedReader br;
  private int lines_read;
  private CommentOptions options;

  private ColorManager cm;
  private DividerManager dm;
  private boolean wants_patient_dividers = false;

  private InputStream ins;
  private SampleSubsets sample_subsets;
  private ArrayList<GenomicSample> unordered_samples;
  private SampleSummaryInfo ssi;
  
  private DoubleHashMap patient2sample_subset = null;

  public static final int MAX_DATAPOINT_VALUE = 98;

  private static final int REPORT_PROGRESS_LINES = 5;
  // private static final int REPORT_PROGRESS_LINES = 20;
  private static final int REPORT_PROGRESS_CELLS = 250000;

  private static final int BLOCK_ID_COMMENT = 1;
  private static final int BLOCK_ID_HEADERS = 2;
  private static final int BLOCK_ID_SAMPLE_NAME = 3;
  private static final int BLOCK_ID_SAMPLE_DATA_BYTE = 4;
  private static final int BLOCK_ID_SAMPLE_DATA_BYTE_RLE = 5;
  
  private static final String DELIMITER = "\t";

  public GenomicMeasurement (String fn) throws FileNotFoundException,IOException {
    detect_file_format(fn);
    ins = new FileInputStream(fn);
    if (fn.lastIndexOf(".gz") == fn.length() - 3) ins = new GZIPInputStream(ins);
    new Thread(this).start();
  }

  public GenomicMeasurement (String fn, boolean async) throws FileNotFoundException,IOException {
    detect_file_format(fn);
    ins = new FileInputStream(fn);
    if (fn.lastIndexOf(".gz") == fn.length() - 3) ins = new GZIPInputStream(ins);
    if (async) {
      new Thread(this).start();
    } else {
      run();
    }
  }

  public GenomicMeasurement(ArrayList<GenomicSample> rows, GenomicMeasurement gm_src) {
    // instantiate a preconstructed set of GenomicSample records
    this.rows = rows;
    headers = gm_src.get_headers();
    run();
  }

  public SampleSummaryInfo get_sample_summary_info() {
    return ssi;
  }

  private void detect_file_format (String fn) throws IOException {
    ins = new FileInputStream(fn);
    if (fn.lastIndexOf(".gz") == fn.length() - 3) ins = new GZIPInputStream(ins);
    DataInputStream dis = new DataInputStream(ins);
    byte b = dis.readByte();
    is_binary = b != '#';
    // detect formatting
    ins.close();
  }



  //   public GenomicMeasurement (String fn, boolean async, boolean is_binary) throws FileNotFoundException,IOException {
  //     ins = new FileInputStream(fn);
  //     if (fn.lastIndexOf(".gz") == fn.length() - 3) ins = new GZIPInputStream(ins);
  //     this.is_binary = is_binary;
  //     if (async) {
  //       new Thread(this).start();
  //     } else {
  //       run();
  //     }
  //   }

  public GenomicMeasurement (InputStream ins) throws IOException {
    this.ins = ins;
    new Thread(this).start();
  }

  public GenomicMeasurement (InputStream ins, boolean async, boolean is_binary) throws IOException {
    this.ins = ins;
    this.is_binary = is_binary;
    if (async) {
      new Thread(this).start();
    } else {
      run();
    }
  }

  public boolean is_loaded () {
    return is_loaded;
  }

  public int get_lines_read() {
    return lines_read;
  }

  public ArrayList<GenomicSample> get_rows () {
    return rows;
  }

  public ArrayList<GenomicSample> get_visible_rows () {
    return visible_rows;
  }

  public ArrayList<GenomicSample> get_visible_rows_raw () {
    // visible rows in ORIGINAL order; useful when downstream code 
    //   (a) doesn't care about current sort order and
    //   (b) wants to cache complex data (i.e. clustering distance matrix)
    return visible_rows_raw;
  }

  public ArrayList<GenomicSample> get_rows_raw () {
    // rows in ORIGINAL order
    return rows_raw;
  }

  public int set_order_by_patient_id (ArrayList<String> ordered, boolean separate_null_leftovers) {
    // set global sample order based on given ordering of patient IDs.
    // excluded IDs will be added at the end.
    SampleSortTools sst = new SampleSortTools(this);
    HashMap<String,ArrayList<GenomicSample>> patient2sample = sst.get_patient2samples();

    ArrayList<GenomicSample> rows_new = new ArrayList<GenomicSample>();
    HashSet<String> saw_patients = new HashSet<String>();

    for (String patient_id : ordered) {
      // given sort order
      if (!saw_patients.contains(patient_id)) {
	rows_new.addAll(patient2sample.get(patient_id));
	saw_patients.add(patient_id);
      }
    }

    int visible_given_rows = 0;
    for (GenomicSample gs : rows_new) {
      if (gs.visible_in_display) visible_given_rows++;
    }
    
    unordered_samples = new ArrayList<GenomicSample>();

    if (separate_null_leftovers) {
      //
      // we want to put leftover samples with only null VISIBLE data at the end of the sort order.
      // This initial pass adds only leftovers with some visible data to the order.
      //
      for (GenomicSample gs : rows) {
	// find leftovers
	if (!saw_patients.contains(gs.patient_id)) {
	  ArrayList<GenomicSample> samples = patient2sample.get(gs.patient_id);
	  boolean all_null = true;
	  for (GenomicSample gs2 : samples) {
	    if (gs2.visible_in_display && gs2.all_null == false) {
	      all_null = false;
	      break;
	    }
	  }
	  if (!all_null) {
	    unordered_samples.addAll(samples);
	    rows_new.addAll(samples);
	    saw_patients.add(gs.patient_id);
	  }
	}
      }
    }

    for (GenomicSample gs : rows) {
      // find leftovers
      if (!saw_patients.contains(gs.patient_id)) {
	unordered_samples.addAll(patient2sample.get(gs.patient_id));
	rows_new.addAll(patient2sample.get(gs.patient_id));
	saw_patients.add(gs.patient_id);
      }
    }
    set_rows(rows_new, true);
    return visible_given_rows;
  }

  public ArrayList<GenomicSample> get_unordered_samples() {
    return unordered_samples;
  }

  public int set_order_by_cluster (ArrayList<Cluster> cluster_list, boolean separate_null_leftovers) {
    // set global sample order based on given ordering.
    // excluded IDs will be added at the end.
    ArrayList<String> patients = new ArrayList<String>();
    for (Cluster c : cluster_list) {
      // sorted
      ArrayList<GenomicSample> sd = c.get_sample_data();
      for (GenomicSample gs : sd) {
	patients.add(gs.patient_id);
      }
    }
    int visible_in_clusters = set_order_by_patient_id(patients, separate_null_leftovers);
    dm.set_cluster_list(cluster_list);
    return visible_in_clusters;
  }

  private void rebuild_visible_rows() {
    visible_rows = new ArrayList<GenomicSample>();
    for (GenomicSample gs : rows) {
      if (gs.visible_in_display) visible_rows.add(gs);
    }
    visible_rows_raw = new ArrayList<GenomicSample>();
    for (GenomicSample gs : rows_raw) {
      if (gs.visible_in_display) visible_rows_raw.add(gs);
    }
  }


  public int get_row_count () {
    return rows == null ? -1 : rows.size();
  }

  public void run() {
    //
    //  load/parse annotations in background
    //
    //    try {Thread.sleep(5000);} catch (Exception e) {}

    dm = new DividerManager(this);

    if (rows == null) {
      options = Options.COMMENT_OPTIONS = new CommentOptions();
      parse_stream();
      if (Options.STARTUP_RESTRICT_COLUMNS) prefilter_data();
    }

    rows_raw = new ArrayList<GenomicSample>(rows);

    rebuild_visible_rows();

    for (GenomicSample gs : rows) {
      // initialize null/empty flags
      gs.set_null_flags();
    }

    // FIX ME: convert ArrayList to array??

    //
    //  determine patient_id for each sample
    //
    sample_subsets = new SampleSubsets(this);
    for (GenomicSample gs : rows) {
      String pid = (new IdentifierMunger(gs.sample_id)).get_patient();
      if (pid == null) {
	// not a TCGA ID variant
	pid = sample_subsets.strip_id(gs.sample_id);
      }
      gs.patient_id = pid;
    }

    ssi = new SampleSummaryInfo(this);

    patient2sample_subset = new DoubleHashMap();
    if (!sample_subsets.isEmpty()) {
      for (GenomicSample gs : rows) {
	patient2sample_subset.put(gs.patient_id, gs.subset_id, gs);
      }
    }

    //
    //  set initial visibility / sample dividers
    //
    if (!sample_subsets.isEmpty()) {
      sample_subsets.set_visibility_flags();
      rebuild_visible_rows();
      rebuild_dividers();
    }

    //    result = Options.sample_subsets.strip_id(raw_id);


    //
    //  apply options if necessary
    //
    byte neutral = 0;
    if (options.has_option("neutral")) {
      //
      //  a "neutral" (center, "zero"-level) value for the data has been specified
      //
      neutral = (byte) options.get_single_neutral_level();
      //
      // HACK: for now, just normalize the input data based on the average
      // of the specified neutral values.
      //
      int i;
      //      System.err.println("rows="+rows.size());  // debug

      for (GenomicSample gs : rows) {
	for (i=0; i < gs.copynum_data.length; i++) {
	  gs.copynum_data[i] -= neutral;
	}
      }
    }

    cm = new ColorManager(this);

    String data_type = options.get("data_type");
    String title = options.get("title");
    if (data_type != null) {
      Options.DATA_TYPE = data_type;
    } else if (title != null) {
      //
      //  try to detect data type from title, if given
      //
      if (title != null) {
	title = title.toLowerCase();
	if (title.indexOf("gene expression") > -1) {
	  Options.DATA_TYPE = "Gene expression";
	} else if (title.indexOf("methylation") > -1) {
	  Options.DATA_TYPE = "Methylation";
	} else if (title.indexOf("copy number") > -1) {
	  Options.DATA_TYPE = "Copy number";
	}
      }
    }

    is_loaded = true;

    setChanged();
    notifyObservers();
  }

  private void parse_stream() {
    //
    //  parse and load data from input stream.
    //
    String line = null;
    long start_time = System.currentTimeMillis();
    
    BufferedReader br = is_binary ?
      null : new BufferedReader(new InputStreamReader(ins));

    try {
      DividerSet dividers = dm.get_patient_dividers();
      Vector out_of_range = new Vector();
      int out_of_range_count = 0;
      lines_read = 0;
      int cells_read = 0;
      rows = new ArrayList<GenomicSample>();
      int progress_checkpoint = REPORT_PROGRESS_CELLS;

      if (is_binary) {
	//
	//  binary file format.
	//  obviously this should be in a different class, but this was
	//  added later.
	//
	DataInputStream dis = new DataInputStream(ins);
	try {
	  GenomicSample gs = null;

	  while (true) {
	    int block_type = dis.readInt();
	    int block_length = dis.readInt();
	    //	    System.err.println("type=" + block_type + " len=" + block_length);  // debug
	    byte[] buf = new byte[block_length];
	    dis.readFully(buf);

	    if (block_type == BLOCK_ID_COMMENT) {
	      String s = new String(buf);
	      if (s.equals("#divider")) {
		dividers.add(lines_read);
	      } else {
		options.parse_line(s);
	      }
	    } else if (block_type == BLOCK_ID_HEADERS) {
	      parse_headers(new String(buf));
	    } else if (block_type == BLOCK_ID_SAMPLE_NAME) {
	      gs = new GenomicSample();
	      gs.sample_id = new String(buf);
	      rows.add(gs);
	      lines_read++;
	    } else if (block_type == BLOCK_ID_SAMPLE_DATA_BYTE_RLE) {
	      ByteArrayInputStream bis = new ByteArrayInputStream(buf);
	      DataInputStream dis2 = new DataInputStream(bis);
	      byte b;
	      int count;
	      int si = 0;
	      int i;
	      gs.copynum_data = new byte[headers.length];

	      try {
		while (true) {
		  b = dis2.readByte();
		  count = dis2.readInt();
		  for (i=0; i < count; i++) {
		    gs.copynum_data[si++] = b;
		  }
		}
	      } catch (EOFException eof) {}

	      if (si != headers.length) {
		throw new IOException("data vs. header length mismatch: " + si + " vs " +headers.length);
	      }


	    } else if (block_type == BLOCK_ID_SAMPLE_DATA_BYTE) {
	      // values simply packed into signed bytes
	      gs.copynum_data = buf;
	      // "that was easy."

	      byte b;
	      int i;

	      for (i = 0; i < gs.copynum_data.length; i++) {
		b = gs.copynum_data[i];
		if (b != GenomicSample.NULL_VALUE) {
		  if (b < - MAX_DATAPOINT_VALUE || b > MAX_DATAPOINT_VALUE) {
		    if (out_of_range.size() < 5) out_of_range.add(Integer.valueOf(b));
		    out_of_range_count++;
		    gs.copynum_data[i] = b < 0 ? (byte) (- MAX_DATAPOINT_VALUE) : MAX_DATAPOINT_VALUE;
		  }
		}
	      }

	      cells_read += gs.copynum_data.length;
	      //	      if (lines_read % REPORT_PROGRESS_LINES == 0) {
	      if (cells_read >= progress_checkpoint) {
		progress_checkpoint += REPORT_PROGRESS_CELLS;

		if (false) {
		  try {
		    System.out.println("DEBUG: delaying loading...");
		    Thread.sleep(100);
		  } catch (InterruptedException e) {}
		}
		setChanged();
		notifyObservers();
	      }

	    } else {
	      throw new Exception("parse error: unknown block type " + block_type);
	    }
	  }
	} catch (EOFException eof) {
	  // parse done
	}

      } else {
	//
	//  flatfile parser...SLOW!
	//

	//      String header_line = br.readLine();
	//      String[] h = header_line.split(delimiter);
	//      column_count = h.length;
	while (true) {
	  //
	  // parse optional parameters until we reach header line
	  //
	  line = br.readLine();
	  //	  System.err.println("last char="+line.substring(line.length() - 1));  // debug
	  // FIX ME: do Windows line-end characters (^M) appear in Unix runtimes??

	  if (!options.parse_line(line)) {
	    parse_headers(line);
	    break;
	  }
	}

	String[] l;
	int i;
	int v = 0;
	float vf = 0;

	boolean float_mode = false;

	int expected_cols = headers.length + 1;
	boolean done = false;

	String encoding = options.get("sample_data_encoding");
	boolean rle_mode = false;
      
	if (encoding != null) {
	  if (encoding.equals("rle")) {
	    rle_mode = true;
	  } else {
	    parse_error = new Exception("unknown encoding " + encoding);
	    done = true;
	  }
	}

	while (!done) {
	  line = br.readLine();
	  if (line == null) break;
	  l = line.split(DELIMITER);
	  //	System.err.println("new sample: " + gs.sample_id);  // debug

	  if (l[0].equals("#divider")) {
	    dividers.add(lines_read);
	    continue;
	  }

	  if (lines_read++ == 0) {
	    //	    float_mode = l[1].indexOf('.') >= 0;
	    for (i=1; i < l.length; i++) {
	      // scan entire first row for float-formatted numbers
	      // 4/21/2010: 1st cell is an int (0) but data is actually float!
	      if (l[i].indexOf('.') >= 0) {
		float_mode = true;
		break;
	      }
	    }
	    //	  System.err.println("FM="+float_mode + " " + l[1]);  // debug
	  }

	  if (l.length != expected_cols && !rle_mode) {
	    throw new IOException("line " + lines_read + " has " + l.length + " columns, we expected " + headers.length + " from header line");
	  }

	  GenomicSample gs = new GenomicSample();
	  gs.sample_id = new String(l[0]);
	  gs.copynum_data = new byte[expected_cols - 1];

	  if (rle_mode) {
	    //
	    //  sample data is RLE-encoded (value,count[,value,count])
	    //
	    int cdi = 0;
	    int count;
	    int j;
	  
	    int rle_size = l.length / 2;
	    gs.rle_values = new byte[rle_size];
	    gs.rle_lengths = new int[rle_size];
	    int rle_i = 0;
	    for (i=1; i < l.length; i += 2) {
	      v = Integer.parseInt(l[i]);
	      count = Integer.parseInt(l[i+1]);

	      if (v != GenomicSample.NULL_VALUE) {
		if (v < - MAX_DATAPOINT_VALUE) {
		  if (out_of_range.size() < 5) out_of_range.add(new Integer(v));
		  out_of_range_count++;
		  v = - MAX_DATAPOINT_VALUE;
		} else if (v > MAX_DATAPOINT_VALUE) {
		  if (out_of_range.size() < 5) out_of_range.add(new Integer(v));
		  out_of_range_count++;
		  v = MAX_DATAPOINT_VALUE;
		}
	      }
	      if (true) {
		gs.rle_values[rle_i] = (byte) v;
		gs.rle_lengths[rle_i] = count;
		rle_i++;
	      }
	      for (j=0; j < count; j++) {
		gs.copynum_data[cdi++] = (byte) v;
	      }
	    }
	  } else if (float_mode) {
	    //
	    //  only parse as float if necessary:
	    //    - raw parsing is slower
	    //    - rounding is required
	    //  sorry for the hideous duplicated code.
	    //
	    for (i=1; i < l.length; i++) {
	      try {
		vf = Float.parseFloat(l[i]);
		if (vf != GenomicSample.NULL_VALUE) {
		  if (vf < - MAX_DATAPOINT_VALUE) {
		    if (out_of_range.size() < 5) out_of_range.add(new Integer(v));
		    out_of_range_count++;
		    vf = - MAX_DATAPOINT_VALUE;
		  } else if (vf > MAX_DATAPOINT_VALUE) {
		    if (out_of_range.size() < 5) out_of_range.add(new Integer(v));
		    out_of_range_count++;
		    vf = MAX_DATAPOINT_VALUE;
		  }
		}
		gs.copynum_data[i-1] = (byte) Math.round(vf);
	      } catch (Exception e) {
		if (Options.VERBOSE_ERRORS && !l[i].equals("NA")) System.err.println("ERROR: datapoint parse of " + e);  // debug
		gs.copynum_data[i-1] = GenomicSample.NULL_VALUE;
	      }
	    }
	  } else {
	    for (i=1; i < l.length; i++) {
	      try {
		v = Integer.parseInt(l[i]);
		if (v != GenomicSample.NULL_VALUE) {
		  if (v < - MAX_DATAPOINT_VALUE) {
		    if (out_of_range.size() < 5) out_of_range.add(new Integer(v));
		    out_of_range_count++;
		    v = - MAX_DATAPOINT_VALUE;
		  } else if (v > MAX_DATAPOINT_VALUE) {
		    if (out_of_range.size() < 5) out_of_range.add(new Integer(v));
		    out_of_range_count++;
		    v = MAX_DATAPOINT_VALUE;
		  }
		}
		gs.copynum_data[i-1] = (byte) v;
	      } catch (Exception e) {
		if (Options.VERBOSE_ERRORS && !l[i].equals("NA")) System.err.println("ERROR: datapoint parse of " + e);  // debug
		gs.copynum_data[i-1] = GenomicSample.NULL_VALUE;
	      }
	    }
	  }

	  rows.add(gs);

	  if (lines_read % REPORT_PROGRESS_LINES == 0) {
	    // progress report
	    setChanged();
	    notifyObservers();
	  }

	  //	System.gc();
	}  // !done

      } // is_binary

      setChanged();
      notifyObservers();
      // ensure we notify of progress at least once with original headers
      // (i.e. if preformatting)

      long end_time = System.currentTimeMillis();
      //      System.err.println("data parsing time:" + (end_time - start_time) + "ms");  // debug

      GISTIC gistic = options.get_gistic();
      if (gistic != null) {
	gistic.header_setup(this);
      }

      if ((Options.VERBOSE_ERRORS) && out_of_range_count > 0) {
	System.err.println(out_of_range_count + " values out of range: " + 
			   Funk.Str.join(out_of_range.elements()) + "..."
			   );  // debug
      }

      String di = options.get("divider_interval");
      if (di != null) {
	//
	//  dataset requests a divider be placed every "di" rows
	//
	int divider_interval = Integer.parseInt(di);
	for (int i=divider_interval; i < rows.size(); i += divider_interval) {
	  dividers.add(i);
	}
      }
      wants_patient_dividers = !dividers.isEmpty();
    } catch (Exception e) {
      parse_error = e;
      System.err.println(e);  // debug
      new ErrorReporter(e);
      //      System.err.println("last line:" + line);  // debug
    }
  }

  public static void main (String [] argv) {
    try {
      //      try {Thread.sleep(500000);} catch (Exception e) {}

      String fn = "broad_snp6_genomicmeasurement.txt";
      if (argv.length > 0) fn = argv[0];

      //      double now = System.currentTimeMillis();
      // System.err.println(now);  // debug

      GenomicMeasurement gm = new GenomicMeasurement(fn, false);
      //      System.err.println("elapsed:" + (System.currentTimeMillis() - now));  // debug


      System.gc();
      System.err.println("done 1");  // debug
      //      GenomicMeasurement gm2 = new GenomicMeasurement(fn, false);
      //      System.err.println("done 2");  // debug

      System.gc();
      try {Thread.sleep(500000);} catch (Exception e) {}
      System.exit(1);


      AnnotationFlatfile2 af = new AnnotationFlatfile2("intgen.org_GBM.biotab.1.0.0.tab", false);

      for (GenomicSample gs : gm.get_rows()) {
	ArrayList annot = af.find_annotations(gs);
	System.err.println(gs.sample_id + " " + (annot == null ? "undef" : "ok"));  // debug
      }

    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public CommentOptions get_options () {
    return options;
  }

  public String[] get_headers() {
    return headers;
  }

  public void reorder_headers (ArrayList<Integer> new_order) {
    if (new_order.size() == headers.length) {
      String[] headers_new = new String[headers.length];
      int ti=0;
      for (Integer si : new_order) {
	headers_new[ti++] = headers[si];
      }
      headers = headers_new;
    } else {
      System.err.println("ERROR: invalid length in header reorder attempt");  // debug
    }
  }

  public void set_rows (ArrayList<GenomicSample> rows, boolean reset_clusters) {
    this.rows = rows;
    rebuild_visible_rows();
    if (reset_clusters) dm.clear_cluster_dividers();
    rebuild_dividers();
    setChanged();
    notifyObservers();
  }

  public GenomicMeasurement clone () {
    GenomicMeasurement gm_new=null;
    try {
      //      gm_new = (GenomicMeasurement) super.clone();
      gm_new = (GenomicMeasurement) super.clone();
      // create shallow copy

      gm_new.clone_setup(this);
      
    } catch (Exception e) {
      System.err.println("clone error!:"+e);
    }
    return gm_new;
  }

  public void clone_setup (GenomicMeasurement gm_parent) {
    // clone setup: init clone variables as deep copy as required from parent

    dm = new DividerManager(this);
    rebuild_dividers();
    // hack

    sample_subsets = new SampleSubsets(this, gm_parent.get_sample_subsets());
  }

  public void generate_subset (ArrayList<Integer> bin_list) {
    // cloned instance:
    // limit data to just the bins in specified list
    int count = bin_list.size();
    int[] bins = new int[count];
    int i;
    for (i=0; i < count; i++) {
      bins[i] = bin_list.get(i).intValue();
    }

    //
    // header list for subset:
    //
    String[] h2 = new String[count];
    for (i=0; i < count; i++) {
      h2[i] = headers[bins[i]];
    }

    //
    // sample data:
    //
    ArrayList<GenomicSample> rows2 = new ArrayList<GenomicSample>();
    int j;
    for (i=0; i < rows.size(); i++) {
      //      System.err.println("row:"+i);  // debug
      GenomicSample from = rows.get(i);
      GenomicSample to = from.clone();
      to.copynum_data = new byte[count];
      for (j=0; j < count; j++) {
	to.copynum_data[j] = from.copynum_data[bins[j]];
      }
      to.set_null_flags();
      rows2.add(to);
    }

    System.err.println("fix me: rebuild SampleSummaryInfo");  // debug
    // FIX ME: SampleSummaryInfo!!!


    headers = h2;
    rows = rows2;
    // replace shallow copies
    rows_raw = new ArrayList<GenomicSample>(rows);

    rebuild_visible_rows();
    rebuild_dividers();
  }

  public void generate_subset (Rectangle sel) {
    // cloned instance:
    // limit data to just the data in the given selection

    //
    // copy headers subset:
    //
    String[] h2 = new String[sel.width];
    System.arraycopy(headers, sel.x, h2, 0, sel.width);
    
    //
    // copy data rows / columns:
    //
    ArrayList<GenomicSample> rows2 = new ArrayList<GenomicSample>();
    int rend = sel.y + sel.height;
    int rend2 = rows.size();
    for (int ri = sel.y; ri < rend && ri < rend2; ri++) {
      // foreach row in selection...
      GenomicSample from = rows.get(ri);
      GenomicSample to = from.clone();
      to.copynum_data = new byte[sel.width];
      System.arraycopy(from.copynum_data, sel.x,
		       to.copynum_data, 0,
		       sel.width);
      rows2.add(to);
    }

    headers = h2;
    rows = rows2;
    // replace shallow copies
    rows_raw = new ArrayList<GenomicSample>(rows);

    rebuild_visible_rows();
    rebuild_dividers();
  }

  public Rectangle generate_selection_start_end(int start_x, int end_x) {
    return generate_selection(start_x, (end_x - start_x) + 1);
  }

  public Rectangle generate_selection(int start_x, int width) {
    Rectangle r = new Rectangle();
    r.x = start_x;
    r.width = width;
    r.y = 0;
    r.height = get_row_count();
    return r;
  }

  public Rectangle generate_selection(GenomicBin gb) {
    return generate_selection(gb.start - 1,
			      // GenomicBin indices are 1-based, we use 0-based
			      (gb.end - gb.start) + 1);

  }

  public Rectangle generate_selection(int bin) {
    return generate_selection(bin, 1);
  }

  public Rectangle generate_selection(ClusterTool ct) {
    // generate selection for cluster results
    Rectangle r = new Rectangle();
    r.x = 0;
    r.width = headers.length;
    r.y = 0;
    r.height = ct.get_visible_in_clusters();
    return r;
  }


  public Rectangle generate_selection(String patient_id) {
    // generate selection for a given patient ID
    // (multiple rows if multiple rows grouped w/same sample ID)
    Rectangle r = new Rectangle();
    r.x = 0;
    r.width = headers.length;
    r.y = 0;
    r.height = 0;
    
    int start = -1;
    int height = 0;
    int y = 0;
    String this_id;
    for (GenomicSample gs : visible_rows) {
      if (gs.patient_id.equals(patient_id)) {
	height++;
	if (start == -1) start = y;
      } else if (start != -1) {
	break;
      }
      y++;
    }
    
    r.y = start;
    r.height = height;

    return r;

  }

  private void rebuild_dividers() {
    if (wants_patient_dividers) dm.rebuild_patient_dividers();
    //
    // rebuild dividers between patient IDs
    // - don't simply check if dividers.isEmpty(), as
    //   dividers may be suppressed entirely if only one sample subset is visible
    //
    dm.rebuild_cluster_dividers();
  }

  private void parse_headers (String s) {
    String[] h = s.split(DELIMITER);
    headers = new String[h.length - 1];
    for (int i=1; i < h.length; i++) {
      // strip first (meaningless) column for sample label
      headers[i - 1] = new String(h[i]);
    }
    //    System.err.println("header count:" +headers.length);  // debug

  }

  public float sort_by_bin (int index, String subset) {
    // foreach sample:
    // - record highest observed value for each patient ID for given bin
    // - sort unique list of highest observed values
    // - iterate through and rebuild list
    // - rebuild dividers
    SampleSortTools sst = new SampleSortTools(this);
    if (subset != null) sst.set_subset_filter(subset);
    ArrayList<GenomicSample> sorted = sst.sort_by_bin(index);
    set_rows(sorted, true);
    return sst.get_average();
  }

  public int get_column_count() {
    if (is_loaded()) {
      return rows.get(0).copynum_data.length;
      // or: headers.length???
    } else {
      return -1;
    }
  }

  public ArrayList<String> get_visible_sample_ids() {
    // return unique alpha-sorted list of currently visible PATIENT IDs
    HashSet<String> ids = new HashSet<String>();
    for (GenomicSample gs : visible_rows) {
      ids.add(gs.patient_id);
    }
    ArrayList<String> unique_ids = new ArrayList<String>();
    unique_ids.addAll(ids);
    Collections.sort(unique_ids);
    return unique_ids;
  }

  public ArrayList<String> get_unique_patient_ids() {
    // return unique list of patient IDs, in current order
    HashSet<String> ids = new HashSet<String>();
    ArrayList<String> unique_ids = new ArrayList<String>();
    for (GenomicSample gs : visible_rows) {
      if (!ids.contains(gs.patient_id)) unique_ids.add(gs.patient_id);
      ids.add(gs.patient_id);
    }
    return unique_ids;
  }

  public DividerManager get_divider_manager() {
    return dm;
  }

  public ColorManager get_color_manager() {
    return cm;
  }

  public SampleSubsets get_sample_subsets() {
    return sample_subsets;
  }

  public GenomicSample get_sample_for_patient_subset (String patient_id, String subset_id) {
    return (GenomicSample) patient2sample_subset.get(patient_id, subset_id);
  }

  public boolean is_genome_formatted () {
    String genome = get_options().get("genome");
    return genome != null && (genome.equalsIgnoreCase("on") ||
			      genome.equalsIgnoreCase(GenomeScaler.GENOME_VERSION_HG18) ||
			      genome.equalsIgnoreCase(GenomeScaler.GENOME_VERSION_HG19)
			      );
  }

  public String get_genome_version() {
    //
    // for genomically-formatted heatmaps, get genome version to use 
    //
    String version = null;
    String genome = get_options().get("genome");
    if (genome != null) {
      if (genome.equalsIgnoreCase("on")) {
	// default/historical behavior is hg18
	version = GenomeScaler.GENOME_VERSION_HG18;
      } else if (genome.equalsIgnoreCase("off")) {
	// explicitly not genomically formatted
      } else if (genome.equalsIgnoreCase(GenomeScaler.GENOME_VERSION_HG18)) {
        version = GenomeScaler.GENOME_VERSION_HG18;
      } else if (genome.equalsIgnoreCase(GenomeScaler.GENOME_VERSION_HG19)) {
        version = GenomeScaler.GENOME_VERSION_HG19;
      }
    }
    return version;
  }


  private void prefilter_data() {
    //
    //  start up showing only specified columns
    //
    //    System.err.println("prefilter");  // debug

    Options.NAVIGATION_DEFAULT_ZOOM = false;

    HashSet<Integer> bin_set = new HashSet<Integer>();
    HashSet<String> hdrs = new HashSet<String>();
    for (int i = 0; i < headers.length; i++) {
      String[] h2 = headers[i].split(",");
      for (int j = 0; j < h2.length; j++) {
	hdrs.add(h2[j]);
	if (Options.RESTRICT_COLUMNS.contains(h2[j])) {
	  bin_set.add(i);
	}
      }
    }

    for (String col : Options.RESTRICT_COLUMNS) {
      if (!hdrs.contains(col)) {
	System.err.println("ERROR: no column named " + col);  // debug
      }
    }

    ArrayList<Integer> bin_list = new ArrayList<Integer>(bin_set);
    Collections.sort(bin_list);

    if (bin_list.size() > 0) {
      generate_subset(bin_list);
      // reduce dataset as if opening a subwindow for a cloned instance

      GISTIC gistic = options.get_gistic();
      if (gistic != null) {
	// remap GISTIC data
	GISTIC.VERBOSITY = 0;
	// hack to disable warning messages when we can't map GISTIC data for a given marker
	gistic.header_setup(this);
      }

      for (TrackInfo ti : options.get_tracks()) {
	ti.collapse_to(bin_list);
      }
    }
  }

  public void combine_datasets (ArrayList<GenomicMeasurement> gm_supplemental, Heatmap6 parent_ref) {
    // mux supplemental data files
    //    System.err.println("combine_datasets()");  // debug
    if (!combined) { 
      combined = true;
      if (gm_supplemental.size() == 0) {
	System.err.println("no data to combine");  // debug
	return;
      }

      //
      //  wait until all supplemental files are loaded
      //
      new Hacktastic(parent_ref, gm_supplemental);
      //      try {Thread.sleep(1);} catch (InterruptedException e) {}
      while (true) {
	boolean all_loaded = true;
	for (GenomicMeasurement gms : gm_supplemental) {
	  //	  System.err.println("loaded: " + gms.is_loaded());  // debug
	  if (!gms.is_loaded()) all_loaded = false;
	}
	if (all_loaded) {
	  break;
	} else {
	  try {
	    //	    System.out.println("waiting for supplemental GM to load...");
	    Thread.sleep(50);
	  } catch (InterruptedException e) {}
	}
      }
      //      try {Thread.sleep(1);} catch (InterruptedException e) {}

      String parent_data_type = options.get("data_type");
      //      System.err.println("raw parent type:"+parent_data_type);  // debug

      if (parent_data_type == null) parent_data_type = "copy number";

      ArrayList<String> all_data_types = new ArrayList<String>();
      all_data_types.add(parent_data_type);
      for (GenomicMeasurement gms : gm_supplemental) {
	String dtype = gms.get_options().get("data_type");
	System.err.println("supplemental type="+dtype);  // debug
	all_data_types.add(dtype);
      }

      if (Options.COMBINE_DATASETS_ADD_ROWS_MODE) {
	String adt = Funk.Str.join(",", all_data_types);
	options.put("sample_subsets", adt);
	//      System.err.println("ADT="+adt + " for " + this + " opts="+options);  // debug

	wants_patient_dividers = true;
	// horrible
      }

      ArrayList<GenomicSample> rows_combined = new ArrayList<GenomicSample>();
      HashSet<String> used_ids = new HashSet<String>();

//       System.err.println("src headers:");  // debug
//       for (int i=0; i < headers.length;i++) {
// 	System.err.println("  " + headers[i]);  // debug
//       }
//       for (GenomicMeasurement gms : gm_supplemental) {
// 	String supp_type = gms.get_options().get("data_type");
// 	String[] h = gms.get_headers();
// 	System.err.println("headers for " + supp_type);  // debug
// 	for (int i=0; i <h.length;i++) {
// 	  System.err.println("  " + h[i]);  // debug
// 	}
//       }

      HashMap<GenomicMeasurement,GenomicSampleReformatter> reformatters = new HashMap<GenomicMeasurement,GenomicSampleReformatter>();
      for (GenomicMeasurement gms : gm_supplemental) {
	reformatters.put(gms, new GenomicSampleReformatter(this, gms));
      }

      if (Options.COMBINE_DATASETS_ADD_ROWS_MODE) {
	// combine datasets by adding rows
	for (GenomicSample gs : rows_raw) {
	  String raw_id = gs.sample_id;
	  gs.sample_id = gs.sample_id + " " + parent_data_type;
	  // parent record: modify sample ID to include data type
	  rows_combined.add(gs);
	
	  for (GenomicMeasurement gms : gm_supplemental) {
	    String supp_type = gms.get_options().get("data_type");
	    ArrayList<GenomicSample> samples = gms.get_rows_raw();
	    FuzzyTCGAIDMatcher m = new FuzzyTCGAIDMatcher(raw_id, samples.get(0).sample_id);
	    // use partial barcode matching.
	    // can't just combine all rows and do an alpha sort because:
	    //   1. TCGA-01-2345
	    //   2. TCGA-01-2346
	    //   3. TCGA-03-2345-A (WRONG)
	    for (GenomicSample sample : samples) {
	      if (m.matches(raw_id, sample.sample_id)) {
		rows_combined.add(copy_gs(sample, supp_type, reformatters.get(gms)));
		used_ids.add(gms + sample.sample_id);
		// hacky but easier than the 50 lines of Java required
		// to create and populate a 2-level HashMap  :/
		// viva la perl!

		// TO DO:
		// - reformat column entries!
	      }
	    }
	  }
	}
	for (GenomicMeasurement gms : gm_supplemental) {
	  // find "leftover" rows in supplemental data which don't match
	  // any sample IDs in parent data file
	  String supp_type = gms.get_options().get("data_type");
	  for (GenomicSample sample : gms.get_rows_raw()) {
	    if (!used_ids.contains(gms + sample.sample_id)) {
	      // data
	      //	    System.err.println("miss: "+ sample.sample_id);  // debug
	      rows_combined.add(copy_gs(sample, supp_type, reformatters.get(gms)));
	    }
	  }
	}
      } else {
	// combine datasets by adding columns

	//
	//  create combined headers:
	//

	int combined_column_count = headers.length * (gm_supplemental.size() + 1);

	String[] headers_new = new String[combined_column_count];
	int i,j;

	for (i=0; i < all_data_types.size(); i++) {
	  // standardize data type labels and modify to abbreviated 
	  // versions to save column space
	  String label = all_data_types.get(i).toLowerCase();
	  if (label.equals("copy number")) {
	    label = Options.ABBREVIATION_COPY_NUMBER;
	  } else if (label.equals("gene expression")) {
	    label = Options.ABBREVIATION_GENE_EXPRESSION;
	  } else {
	    System.err.println("ERROR: unhandled label " + label);  // debug
	  }
	  all_data_types.set(i, label);
	}

	for (i=0, j=0; i < headers.length; i++) {
	  for (String dt : all_data_types) {
	    headers_new[j] = headers[i] + " " + dt;
	    //	    System.err.println("set header " + i + " to " + dt +  " => " + headers_new[j]);  // debug
	    j++;
	  }
	}

	//
	//  create combined rows:
	//
	for (GenomicSample gs : rows_raw) {
	  rows_combined.add(gs);

	  //
	  // for each supplemental dataset, get matching GenomicSample:
	  //
	  GenomicSample[] gs_map = new GenomicSample[gm_supplemental.size()];
	  int mi=0;
	  for (GenomicMeasurement gms : gm_supplemental) {
	    ArrayList<GenomicSample> samples = gms.get_rows_raw();
	    FuzzyTCGAIDMatcher m = new FuzzyTCGAIDMatcher(gs.sample_id, samples.get(0).sample_id);
	    ArrayList<GenomicSample> hits = new ArrayList<GenomicSample>();
	    for (GenomicSample sample : samples) {
	      if (m.matches(gs.sample_id, sample.sample_id)) {
		hits.add(sample);
	      }
	    }
	    int hit_count = hits.size();
	    GenomicSample match = null;
	    if (hit_count == 0) {
	      //	      System.err.println("no matches for sample ID " + gs.sample_id);  // debug
	    } else {
	      if (hit_count > 1) {
		System.err.println("ERROR: multiple sample hits!!");  // debug
	      }
	      match = hits.get(0);
	      used_ids.add(gms + match.sample_id);
	    }
	    gs_map[mi++] = match;
	  }

	  //
	  //  generate new combined row
	  //
	  int hi,ci;
	  byte[] combined_data = new byte[combined_column_count];

	  for (ci=0, hi=0; hi < headers.length; hi++) {
	    combined_data[ci++] = gs.copynum_data[hi];
	    // copy entry for this column from primary dataset row

	    mi = 0;
	    for (GenomicMeasurement gms : gm_supplemental) {
	      GenomicSample match = gs_map[mi++];
	      if (match == null) {
		// no equivalent sample data for this subtype
		combined_data[ci++] = GenomicSample.NULL_VALUE;
	      } else {
		GenomicSampleReformatter reformatter = reformatters.get(gms);
		combined_data[ci++] = reformatter.get_mapped(match, hi);
		// copy data for this column from this subtype
	      }
	    }
	  }

	  gs.copynum_data = combined_data;
	  // save reformatted data
	}

	//
	//  add orphaned rows:
	//
	HashMap<String,HashMap<GenomicMeasurement,GenomicSample>> orphans = new HashMap<String,HashMap<GenomicMeasurement,GenomicSample>>();
	for (GenomicMeasurement gms : gm_supplemental) {
	  for (GenomicSample sample : gms.get_rows_raw()) {
	    if (!used_ids.contains(gms + sample.sample_id)) {
	      //	      System.err.println("orphan " + gms + " " + sample.sample_id + " " + gms.get_options().get("data_type")+ " " + sample.patient_id);  // debug
	      String track_id = sample.patient_id;
	      if (track_id == null) track_id = sample.sample_id;
	      // bucket by patient portion of barcode, so that supplemental
	      // datasets using different barcode formats will be tracked together
	      HashMap<GenomicMeasurement,GenomicSample> bucket = orphans.get(track_id);
	      if (bucket == null) orphans.put(track_id, bucket = new HashMap<GenomicMeasurement,GenomicSample>());
	      bucket.put(gms, sample);
	      // save just 1 GenomicSample per patient barcode->GenomicMeasurement
	    }
	  }
	}

	ArrayList<String> sample_names = new ArrayList<String>(orphans.keySet());
	Collections.sort(sample_names);

	for (String track_id : sample_names) {
	  // add a new row for each orphaned sample ID
	  HashMap<GenomicMeasurement,GenomicSample> bucket = orphans.get(track_id);
	  int ci,hi;
	  GenomicSample gs_new = new GenomicSample();
	  gs_new.sample_id = null;
	  gs_new.copynum_data = new byte[combined_column_count];
	  rows_combined.add(gs_new);
	  for (ci=0, hi=0; hi < headers.length; hi++) {
	    gs_new.copynum_data[ci++] = GenomicSample.NULL_VALUE;
	    // primary dataset has no data for this sample (hence orphaned)
	    for (GenomicMeasurement gms : gm_supplemental) {
	      GenomicSample gs = bucket.get(gms);
	      if (gs == null) {
		gs_new.copynum_data[ci++] = GenomicSample.NULL_VALUE;
	      } else {
		if (gs_new.sample_id == null) {
		  gs_new.sample_id = gs.sample_id;
		}
		GenomicSampleReformatter reformatter = reformatters.get(gms);
		gs_new.copynum_data[ci++] = reformatter.get_mapped(gs, hi);
	      }
	    }

	    
	  }
	  
	}


	//
	//  finish:
	//
	headers = headers_new;
	//	System.err.println("new headers:"+headers.length);  // debug


	//	System.err.println("exiting"); 	System.exit(1);
      }

      rows = new ArrayList<GenomicSample>(rows_combined);

      //
      //  generate simple title from combined data
      //
      ArrayList<GenomicMeasurement> all_gms = new ArrayList<GenomicMeasurement>();
      all_gms.add(this);
      all_gms.addAll(gm_supplemental);

      String title = null;
      if (false) {
	// list unique values for each element
	title = "combined " + 
	Funk.Str.join("/", build_unique_list(all_gms, "data_type")) +
	": " +
	Funk.Str.join("/", build_unique_list(all_gms, "project")) + 
	" " + 
	Funk.Str.join("/", build_unique_list(all_gms, "submitter")) + 
	" " +
	Funk.Str.join("/", build_unique_list(all_gms, "platform")) 
	;
      } else {
	// iterate
	title = "combined " + Funk.Str.join("/", build_unique_list(all_gms, "project")) + ": ";
	ArrayList<String> pieces = new ArrayList<String>();
	for (GenomicMeasurement gm : all_gms) {
	  ArrayList<String> stuff = new ArrayList<String>();
	  stuff.add(gm.get_options().get("submitter"));
	  stuff.add(gm.get_options().get("data_type"));
	  stuff.add(gm.get_options().get("platform"));
	  pieces.add(Funk.Str.join(" ", stuff));
	}
	title = title.concat(Funk.Str.join(" / ", pieces));
      }
      get_options().put("title", title);

      run();

      // - create sample_subsets list
      // - modify original file, adding sample subset to ID
      // - iterate through raw file, adding reformatted rows from supplemental files
      // - subsets setup 
    }
  }

  private GenomicSample copy_gs (GenomicSample sample, String subset_type, GenomicSampleReformatter gsr) {
    GenomicSample gs_new = new GenomicSample();
    gs_new.sample_id = sample.sample_id + " " + subset_type;
    gs_new.copynum_data = gsr.remap(sample);
    return gs_new;
  }

  private ArrayList<String> build_unique_list (ArrayList<GenomicMeasurement> gms, String field) {
    HashSet<String> saw = new HashSet<String>();
    ArrayList<String> results = new ArrayList<String>();
    for (GenomicMeasurement gm : gms) {
      String value = gm.get_options().get(field);
      if (value != null && value.length() > 0) {
	if (!saw.contains(value)) {
	  saw.add(value);
	  results.add(value);
	}
      }
    }
    return results;
  }

}
