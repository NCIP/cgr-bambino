package Funk;
// import delimited text file into vector of hashrefs where keys are
// column labels from first row
// mne 3/03

import java.util.*;
import java.io.*;

import java.text.*;

public class DelimitedFile {
  private Vector labels, rows;
  private boolean uppercase_labels = false;
  private boolean trim_whitespace = false;
  private boolean standardize_dates = false;
  private boolean standardize_labels = false;
  private boolean filter_unix_comments = false;
  private boolean delimiter_mandatory = true;
  private ArrayList<String> comments;
  private String delimiter = null;

  public DelimitedFile () {
    // bare constructor in case we want to customize options
  }

  public DelimitedFile (InputStream is) throws IOException {
    parse(is);
  }

  public DelimitedFile (String filename) throws FileNotFoundException,IOException {
    parse(filename);
  }

  public DelimitedFile (File fn) throws FileNotFoundException,IOException {
    parse(fn);
  }

  public void set_trim_whitespace (boolean t) {
    trim_whitespace = t;
  }
  
  public void set_delimiter (String d) {
    delimiter = d;
  }

  public void set_delimiter_mandatory (boolean v) {
    delimiter_mandatory = v;
  }

  public void parse (InputStream is) throws IOException {
    process_file(new BufferedReader(new InputStreamReader(is)));
  }

  public void parse (String filename) throws FileNotFoundException,IOException {
    process_file(new BufferedReader(new FileReader(filename)));
  }

  public void parse (File f) throws FileNotFoundException,IOException {
    process_file(new BufferedReader(new FileReader(f)));
  }

  public Vector get_labels () {
    return labels;
  }

  public Vector get_rows () {
    return rows;
  }

  public Enumeration get_rows_enumeration () {
    return rows.elements();
  }
  
  public void uppercase_labels (boolean option) {
    uppercase_labels = option;
  }

  public void set_standardize_labels (boolean option) {
    standardize_labels = option;
  }

  public void set_standardize_dates (boolean option) {
    standardize_dates = option;
  }

  public void set_filter_unix_comments (boolean option) {
    filter_unix_comments = option;
  }

  private void process_file (BufferedReader br) throws IOException {
    String line;
    boolean first = true;
    labels = new Vector();
    rows = new Vector();
    comments = new ArrayList<String>();
    int i;

    line = br.readLine();
    if (line != null) {
      if (delimiter == null) delimiter = detect_delimiter(line);

      String[] l;
      if (delimiter.equals("")) {
	if (delimiter_mandatory) {
	  throw new IOException("parse error: no delimiter detected in file header line!: " + line);
	} else {
	  l = new String[1];
	  l[0] = line;
	}
      } else {
	l = line.split(delimiter);
      }

      //      System.err.println("delimiter='" + delimiter + "'");  // debug

      for (i = 0; i < l.length; i++) {
        String label = l[i];
        if (uppercase_labels) label = label.toUpperCase();
        if (standardize_labels && label.length() >= 2) {
	  String upper = label.toUpperCase();
	  String lower = label.toLowerCase();
	  label = upper.substring(0,1) + lower.substring(1);
	}
        labels.addElement(label);
	//	System.err.println("label at " + i + " = " + label);  // debug
      }
      
      while (true) {
        line = br.readLine();
        if (line == null) break;

	if (filter_unix_comments && line.length() > 0 && line.charAt(0) == '#') {
	  comments.add(line);
	  continue;
	}
	
	if (delimiter.equals("")) {
	  l = new String[1];
	  l[0] = line;
	} else {
	  l = line.split(delimiter);
	}

        Hashtable row = new Hashtable();
	for (i = 0; i < l.length; i++) {
	  if (trim_whitespace && l[i] != null) {
	    l[i] = Funk.Str.trim_whitespace(l[i]);
	  }
	  if (standardize_dates && l[i] != null) {
	    try {
	      DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
	      Date d = df.parse(l[i]);
	      StringBuffer sb = new StringBuffer();
	      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	      // reformat to year/month/date so alphanumerically sortable
	      sb = sdf.format(d, sb, new FieldPosition(DateFormat.YEAR_FIELD));
	      l[i] = sb.toString();
	    } catch (ParseException e) {
	    }
	  }

          row.put(labels.elementAt(i), l[i]);
        }
        rows.addElement(row);
      }

      // old version using StringTokenizer; bugs parsing some files??

//       st = new StringTokenizer(line, delimiter);
//       while (st.hasMoreElements()) {
//         String label = (String) st.nextElement();
//         if (uppercase_labels) label = label.toUpperCase();
//         labels.addElement(label);
//       }

//       while (true) {
//         line = br.readLine();
//         if (line == null) break;
//         st = new StringTokenizer(line, delimiter);
//         Hashtable row = new Hashtable();
//         i = 0;
//         while (st.hasMoreElements()) {
//           row.put(labels.elementAt(i++), st.nextElement());
//         }
//         rows.addElement(row);
//       }

    }
  }

  private String detect_delimiter (String line) {
    // detect how file is delimited: tab or comma
    // (add others)
    String [] delims = new String[2];
    delims[0] = ",";
    delims[1] = "\t";
    int best_count = -1;
    String best_delim = delims[0];

    int i;
    int this_count;

    for (i=0; i < delims.length; i++) {
      StringTokenizer st = new StringTokenizer(line, delims[i]);
      this_count = st.countTokens();
      if (this_count > best_count) {
        best_count = this_count;
        best_delim = delims[i];
      }
    }

    //    System.err.println("best_count:" + best_count);  // debug

    return best_count > 1 ? best_delim : "";
  }

  public static void main (String []argv) {
    try {
      //      DelimitedFile df = new DelimitedFile("liver_binary.tab");
      
      DelimitedFile df = new DelimitedFile();
      df.set_standardize_dates(true);
      df.parse("../TCGA/updated_clinical_data.tab");
      Vector rows = df.get_rows();
      System.out.println(((Hashtable) rows.elementAt(0)).get("BCRPATIENTBARCODE"));  // debug

    } catch (Exception e) {
      System.out.println("exception: " + e);  // debug
    }
  }

  public ArrayList<String> get_unix_comments() {
    return comments;
  }

}
