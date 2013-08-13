package TCGA;
//
// utility for generating a simple HTML table with labeled columns and rows
//

import java.util.*;

public class SimpleHTMLGrid {
  private HashMap<String,HashMap<String,String>> data;
  private ArrayList<String> column_labels, row_labels;
  private HashSet<String> row_label_set, column_label_set;
  private boolean use_th_for_headers = true;
  private boolean use_underline_for_headers = false;
  private int border = 0;
  private boolean show_column_labels = true;
  private String row_label_align = null;

  public SimpleHTMLGrid() {
    column_labels = new ArrayList<String>();
    row_labels = new ArrayList<String>();
    row_label_set = new HashSet<String>();
    column_label_set = new HashSet<String>();
    setup();
  }

  public SimpleHTMLGrid(ArrayList<String> column_labels, ArrayList<String> row_labels) {
    this.column_labels = column_labels;
    this.row_labels = row_labels;
    row_label_set = new HashSet<String>(row_labels);
    setup();
  }

  private void setup() {
    data = new HashMap<String,HashMap<String,String>>();
  }

  public void set_row_label_align (String v) {
    row_label_align = v;
  }

  public void set_th_headers (boolean v) {
    use_th_for_headers = v;
  }

  public void set_underline_headers (boolean v) {
    use_underline_for_headers = v;
  }

  public void set_cell_value (String column, String row, String value) {
    add_column(column);
    add_row(row);
    HashMap<String,String> hr = data.get(column);
    if (hr == null) data.put(column, hr = new HashMap<String,String>());
    hr.put(row, value);
  }

  public void set_cell_value (String column, String row, int value) {
    set_cell_value(column, row, Integer.toString(value));
  }

  public String get_cell_value (String column, String row) {
    HashMap<String,String> hr = data.get(column);
    String result = hr == null ? "" : hr.get(row);
    if (result == null) result = "";
    return result;
  }

  public void add_column (String s) {
    // can be called explicitly to predefine order, otherwise
    // added automatically as set_cell_value() is called
    if (!column_label_set.contains(s)) {
      column_labels.add(s);
      column_label_set.add(s);
    }
  }

  public void add_row (String s) {
    // can be called explicitly to predefine order, otherwise
    // added automatically as set_cell_value() is called
    if (!row_label_set.contains(s)) {
      row_labels.add(s);
      row_label_set.add(s);
    }
  }

  private void open_tag (String tag, StringBuffer sb) {
    sb.append("<" + tag + ">");
  }

  private void close_tag (String tag, StringBuffer sb) {
    sb.append("</" + tag + ">");
  }

  public void set_border (int b) {
    border = b;
  }

  public void set_show_column_labels (boolean v) {
    show_column_labels = v;
  }

  public StringBuffer generate_html() {
    StringBuffer sb = new StringBuffer();
    if (border > 0) {
      sb.append("<table border=" + border + ">\n");
    } else {
      sb.append("<table>\n");
    }

    if (show_column_labels) {
      sb.append("<tr>\n");
      String header_tag = use_th_for_headers ? "th" : "td";
      open_tag(header_tag, sb);
      close_tag(header_tag, sb);
      //    sb.append("  <th></th>\n");
      // blank label for column containing row labels
      for (String col : column_labels) {
	open_tag(header_tag, sb);
	if (use_underline_for_headers) open_tag("u", sb);
	sb.append(col);
	if (use_underline_for_headers) close_tag("u", sb);
	close_tag(header_tag, sb);
	sb.append("\n");
      }
      sb.append("</tr>\n");
    }

    for (String row : row_labels) {
      sb.append("<tr>\n");
      sb.append("  <td");
      if (row_label_align != null) sb.append(" align=" + row_label_align);
      sb.append(">" + row + ":\n");
      for (String col : column_labels) {
	sb.append("  <td>" + get_cell_value(col, row) + "</td>\n");
      }
      sb.append("</tr>\n");
    }

//     for (ArrayList<String> row : rows) {
//       sb.append("<tr>\n");
//       for (String s : row) {
// 	sb.append("  <td>" + s + "</td>\n");
//       }
//       sb.append("</tr>\n");
//     }

    sb.append("</table>");
    return sb;
  }

  public static void main (String[] argv) {
    SimpleHTMLGrid ht = new SimpleHTMLGrid();
    ht.set_th_headers(false);
    ht.set_underline_headers(true);

    ht.add_column("broad");
    ht.add_column("focal");
    ht.add_row("q-value");
    ht.add_row("z-score");

    ht.set_cell_value("broad", "q-value", "1.23");
    ht.set_cell_value("focal", "z-score", "xxx");

    StringBuffer sb = ht.generate_html();
    System.err.println(sb);  // debug

    
  }

}
