package TCGA;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.text.NumberFormat;

public class FrequencyReporter extends Observable implements Runnable {
  private HeatmapConfiguration config;
  private JTable table;
  private Vector marker_list;
  private ChromDecoratorPanel cdp;

  public FrequencyReporter (HeatmapConfiguration config, ChromDecoratorPanel cdp) {
    this.config = config;
    this.cdp = cdp;
    new Thread(this).start();
  }

  public void run() {
    //    ByteComparator bc = new ByteComparatorNE((byte) 0);
    ByteComparator bc = new ByteComparatorGT((byte) 0);

    HashMapArrayList percent_altered = new HashMapArrayList();
    // bucket by percent

    String[] headers = config.gm.get_headers();
    int i;
    
    int[] counts = new int[headers.length];
    int row_count = config.gm.get_row_count();

    // for each marker, get counts matching given criteria:
    for (GenomicSample gs : config.gm.get_rows()) {
      for (i=0; i < headers.length; i++) {
	if (bc.compare(gs.copynum_data[i])) counts[i]++;
      }
    }

    for (i=0; i < headers.length; i++) {
      percent_altered.put(Float.valueOf((float) counts[i] / row_count), headers[i]);
    }

    ArrayList<Float> percents = new ArrayList<Float>(percent_altered.keySet());
    Collections.sort(percents);
    Collections.reverse(percents);

    int count=0;

    GeneDatabase db = GeneDatabase.get_gene_database();

    Vector row;
    Vector rows = new Vector();
    Vector columns = new Vector();
    marker_list = new Vector();
    columns.add("percent");
    columns.add("marker");
    columns.add("description");
    columns.add("Entrez");

    NumberFormat nf = NumberFormat.getInstance();
    //    nf.setMinimumIntegerDigits(2);
    nf.setMaximumFractionDigits(2);
    nf.setMinimumFractionDigits(2);
    
    for (Float percent : percents) {
      if (percent > 0) {
	ArrayList<String> markers = percent_altered.get(percent);
	Collections.sort(markers);
	for (String marker : markers) {
	  rows.add(row = new Vector());
	  GeneInfo gi = db.get_gene_by_symbol(marker);
	  String desc = gi == null ? "UNKNOWN" : gi.description;

	  // label as HTML so rendered as link
	  String html = "<html><a href=\"http://www.bogus.com/\">" + marker + "</html>";

	  row.add(nf.format(percent * 100));
	  row.add(html);
	  row.add(desc);
	  row.add(html);

	  marker_list.add(marker);
	}
	count++;
      }
    }

    //    System.err.println("count="+count);  // debug

    JFrame jf = new JFrame("Frequency report");
    jf.setLayout(new BorderLayout());

    //    table = new JTable(rows, columns);
    table = new JTable(new DefaultTableModelReadOnly(rows, columns));

    //    ta.setEditable(false);

    TableTools.calcColumnWidths(table);

    table.addMouseListener(new MouseListener() {
	// begin MouseListener stubs
	public void mousePressed(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {
	  int col = table.getSelectedColumn();
	  int row = table.getSelectedRow();
	  String marker = (String) marker_list.get(row);
	  
	  if (col == 1) {
	    BinIndex bi = new BinIndex(config.gm);
	    int wanted = bi.find(marker);
	    if (wanted != -1) {
	      RubberBandSelection rbs = cdp.get_selection();
	      rbs.set_selection(config.gm.generate_selection(wanted));
	      cdp.zoom_to_selection();
	    }
	  } else if (col == 3) {
	    URLLauncher.launch_url(WebTools.entrez_gene_link(marker), "eg");
	  }

	};
	public void mouseReleased(MouseEvent e) {};
	public void mouseEntered(MouseEvent e) {};
	public void mouseExited(MouseEvent e) {};
	// end MouseListener stubs
      });

    jf.add("Center", new JScrollPane(table));

    //    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    //    table.setPreferredScrollableViewportSize(table.getPreferredSize());

    jf.pack();
    jf.setVisible(true);

    
    
  }

  
}

