package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.font.*;
import java.util.*;
import java.awt.geom.*;
import java.awt.event.*;

public class AnnotationGridPanel2 extends JPanel implements AdjustmentListener,MouseListener,ActionListener,Observer {
  private GenomicMeasurement gm;
  private ChromDecoratorPanel isp = null;
  private HashSet<String> visible_annotations = null;

  private float ROW_HEIGHT = 6.0f;

  private int column_width;
  private int column_y_buf;
  private AnnotationDisplayControl adc;

  private static Color annotation_color = Color.yellow;
  private int grid_height = 0;
  private static final String LABEL_HELP = "Help: annotations";

  private static final int FONT_SIZE = 12;

  ArrayList<AnnotationGridData> annotations;
  
  public AnnotationGridPanel2 (AnnotationFlatfile2 af, AnnotationFlatfile2 vasari, GenomicMeasurement gm, ChromDecoratorPanel isp) {
    
    annotations = new ArrayList<AnnotationGridData>();

    if (af != null) annotations.add(new AnnotationGridData(af, annotation_color));
    if (vasari != null) annotations.add(new AnnotationGridData(vasari, annotation_color));

    this.gm = gm;
    this.isp = isp;

    reset_fields();

    ROW_HEIGHT = isp == null ? 6 : isp.get_vertical_scale_level();
    // use calculated default vertical zoom level

    Font font = get_font();
    FontMetrics fm = getFontMetrics(font);
    column_width = (int) (fm.getHeight() * 1.3);

    column_y_buf = (column_width - fm.getHeight()) / 2;

    if (true) {
      // initially set only preferred width, for use with JSplitPanel.
      // avoid setting preferred height now because we want the heatmap panel's
      // preferred size to be the only determinant of the main window's height.
      // In many cases due to long annotation labels this panel would have
      // a larger height than the heatmap panel, resulting in initial
      // "dead space" at the bottom of the heatmap panel.
      //      System.err.println("col count="+get_column_count() + " width=" + column_width);  // debug
      Dimension pref = new Dimension(get_column_count() * column_width, getPreferredSize().height);
      //      System.err.println("AGP2 pref="+pref);  // debug
      setPreferredSize(pref);
    } else if (false) {
      System.err.println("no pref size for grid panel");  // debug
    } else if (true) {
      System.err.println("fake small size for grid panel");
      setPreferredSize(new Dimension(get_column_count() * column_width, 200));
    } else {
      int pref_height = (int) (gm.get_row_count() * ROW_HEIGHT);
      setPreferredSize(new Dimension(get_column_count() * column_width, pref_height));
      // need this for JScrollPane to work
    }
    addMouseListener(this);
    setToolTipText("field name");

    JPopupMenu jpm = new JPopupMenu();

    JMenuItem jmi = new JMenuItem("Choose annotations to display...");
    jpm.add(jmi);
    jmi.addActionListener(this);

    jpm.add(new JSeparator());
    
    jpm.add(HelpLauncher.generate_jmenuitem(LABEL_HELP, HelpLauncher.ANCHOR_ANNOTATIONS));
    new PopupListener(this, jpm);
  }

  public void set_grid_height (int h) {
    grid_height = h;
  }

  protected void paintComponent(Graphics gr) {
    ArrayList<GenomicSample> gm_rows = gm.get_visible_rows();

    super.paintComponent(gr);

    Dimension d = getSize();

    Graphics2D g = (Graphics2D) gr;

    int row = 0;

    int start;
    if (isp == null) {
      start = 0;
      grid_height = 300;
    } else {
      start = isp.get_unscaled_y_start();
      grid_height = isp.getSize().height;
    }
    int end = gm_rows.size();
    
    float yf = 0;
    int y,y2;
    int height;
    
    for (int i = start; i < end; i++) {
      y = (int) yf;
      y2 = (int) (yf + ROW_HEIGHT);
      height = (y2 - y) + 1;
      if (y2 >= grid_height) {
	//	System.err.println("STOPPING");  // debug
	break;
      }
      
      GenomicSample gs = gm_rows.get(i);
      //
      //  draw grid graphic
      //
      //      System.err.println("sample:"+gs.sample_id);  // debug
      int screen_col = 0;
      for (AnnotationGridData agd : annotations) {
	int flen = agd.visible_fields.size();
	ArrayList annot = agd.af.find_annotations(gs);
	if (annot == null) {
	  // no data, gray out entire row
	  g.setColor(AnnotationColorMapper2.UNDEF_COLOR);
	  g.fillRect(screen_col * column_width, y, flen * column_width, height);
	  screen_col += flen;
	} else {
	  // have annotations for this row
	  Hashtable ht = (Hashtable) annot.get(0);
	  for (int col=0; col < flen; col++, screen_col++) {
	    String field = agd.visible_fields.get(col);
	    g.setColor(agd.acm.get_color(field, (String) ht.get(field)));
	    g.fillRect(screen_col * column_width, y, column_width, height);
	  }
	}
      }

      yf += ROW_HEIGHT;
    }

    //    System.err.println("grid:"+grid_height + " yf="+yf);  // debug

    //
    //  draw column labels
    //
    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
    Font font = get_font();
    FontMetrics fm = getFontMetrics(font);
    
    int buffer = 5;
    int base_y = ((int) yf) + buffer;
    // don't use grid_height, but rather actual space consumed
    // in heatmap view

    int max_y = 0;
    AffineTransform at = new AffineTransform();
    at.rotate(-Math.PI/2, 0, 0);
    Font derived = font.deriveFont(at);
    if (derived == null) System.err.println("ERROR deriving vertical font!");  // debug
    g2.setFont(derived);
    g2.setPaint(Color.blue);
    
    int screen_col = 0;

    boolean has_sections = false;

    HashMap<String,Range> section2cols = new HashMap<String,Range>();
    int max_x = 0;

    for (AnnotationGridData agd : annotations) {
      HashMap<String,String> column2section = agd.get_af().get_column2section();
      //      System.err.println("c2s="+column2section);  // debug

      if (column2section != null) has_sections = true;

      agd.index2field = new HashMap<Integer,String>();
      for (String label : agd.visible_fields) {
	int x_raw = screen_col * column_width;
	int x = x_raw + column_y_buf + fm.getAscent();
	max_x = x;

	if (column2section != null) {
	  String section = column2section.get(label);

	  if (section == null) {
	    System.err.println("WTF: " + label);  // debug
	  } else {
	    Range r = section2cols.get(section);
	    if (r == null) section2cols.put(section, r = new Range(-1, -1));
	    if (r.start == -1 || x_raw < r.start) r.start = x_raw;
	    if (r.end == -1 || x_raw > r.end) r.end = x_raw;
	  }
	}

	y = base_y + fm.stringWidth(label);
	if (y > max_y) max_y = y;
	//      System.err.println("Y:" + getSize() + " " + y);  // debug
	g2.drawString(label, x, y);
	agd.index2field.put(screen_col, label);
	screen_col++;
      }
    }

    //
    //  draw section labels
    //
    //    Font f = new Font("SansSerif", Font.BOLD, FONT_SIZE);

    g2.setColor(AnnotationColorMapper2.UNDEF_COLOR);
    g2.fillRect(0, max_y + 1,
		max_x + column_width, max_y + (fm.getHeight() * 2));

    Font f = get_font();
    fm = getFontMetrics(f);
    g2.setFont(f);
    for (String section : section2cols.keySet()) {
      Range r = section2cols.get(section);

      g2.setColor(new Color(100,100,100));
      //      g2.drawLine(r.start, base_y, r.start, max_y + (fm.getHeight() * 2));
      g2.drawLine(r.start, base_y, r.start, max_y);

      int width = (r.end - r.start) + column_width;
      // extra column width to reflect screen width of last column (as opposed to start position)
      int x_center = r.start + (width / 2);
      int sw = fm.stringWidth(section);
      int sy = max_y + fm.getHeight();

      g2.setColor(Color.white);
      g2.drawString(section, x_center - (sw / 2), sy);
      //      System.err.println("sec " + section + " " + r.start + "-" + r.end);  // debug
    }

    //    System.err.println("sec2cols:"+section2cols);  // debug

    max_y += buffer;
    if (has_sections) max_y += fm.getHeight();

    if (false) {
      System.err.println("DEBUG, not resizing");  // debug
    } else if (max_y > d.height) {
      //
      // delay claiming required vertical space until now (see constructor)
      // 
      d.height = max_y;
      //      System.err.println("size:" + getSize() + " resizing to:" + d);  // debug
      setSize(d);
      setPreferredSize(d);
      invalidate();
      // fix me: any way of doing this only as many times as absolutely necessary?
    }

  }

  
  public static void main (String [] argv) {
    try {
      AnnotationFlatfile2 af = new AnnotationFlatfile2("GBM_sample_data.tab",false);

      //      String gm_fn = "broad_snp6_genomicmeasurement.txt";
      if (argv.length == 0) {
	System.err.println("ERROR: specify gm file");  // debug
      } else {
	String gm_fn = argv[0];
	GenomicMeasurement gm = new GenomicMeasurement(gm_fn, false);

	JFrame jf = new JFrame();
	jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	jf.setLayout(new BorderLayout());
	AnnotationGridPanel2 agp = new AnnotationGridPanel2(af,null,gm,null);
	jf.add("Center", agp);
	jf.pack();
	jf.setVisible(true);
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }
  
  public void set_vertical_scale_level (float v) {
    ROW_HEIGHT = v;
    repaint();
  }

  public float get_vertical_scale_level () {
    return ROW_HEIGHT;
  }

  // begin AdjustmentListener stub
  public void adjustmentValueChanged(AdjustmentEvent e) {
    repaint();
  }
  // end AdjustmentListener stub

    // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {
    Point p = e.getPoint();
    int index = (int) (p.x / column_width);

    for (AnnotationGridData agd : annotations) {
      String field = agd.index2field.get(index);
      if (field != null) {
	agd.acm.sort_samples_by(field, gm, agd.af);
	if (isp != null) isp.set_tooltip_field(field);
	repaint();
      }
    }

  };

  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

  public String getToolTipText(MouseEvent e) {
    Point mp = e.getPoint();
    int index = (int) (mp.x / column_width);
    String result = "";
    int col_count = get_column_count();

    ArrayList<GenomicSample> gm_rows = gm.get_visible_rows();
    int sample_index = isp == null ? 0 : isp.get_unscaled_y_start();
    sample_index += (int) (mp.y / ROW_HEIGHT);

    String field = null;

    for (AnnotationGridData agd : annotations) {
      field = agd.index2field.get(index);
      if (field != null) {
	// found
	if (sample_index < gm_rows.size()) {
	  GenomicSample gs = gm_rows.get(sample_index);
	  String value_label = "(no data)";
	  ArrayList annot = agd.af.find_annotations(gs);
	  if (annot != null) {
	    Hashtable ht = (Hashtable) annot.get(0);
	    String value = (String) ht.get(field);
	    if (AnnotationColorMapper2.is_usable_value(value)) {
	      value_label = value;
	    }
	  }
	  result = field + ": " + value_label;
	} else {
	  result = field;
	}
	result = result.concat(" (click to sort)");
	break;
      }
    }

    return result;
  }

  private Font get_font() {
    //    return new Font("Serif", Font.PLAIN, FONT_SIZE);
    return new Font("SansSerif", Font.PLAIN, FONT_SIZE);
  }

  public void actionPerformed(ActionEvent e) {
    if (adc == null) {
      ArrayList<String> fields = new ArrayList<String>();
      for (AnnotationGridData annot : annotations) {
	fields.addAll(annot.af.get_sorted_keys());
      }
      Collections.sort(fields);
      adc = new AnnotationDisplayControl(fields);
      adc.addObserver(this);
    } else {
      adc.setVisible(true);
    }
  }

  // begin Observer stub
  public void update (Observable o, Object arg) {
    visible_annotations = new HashSet<String>((ArrayList<String>) arg);
    reset_fields();
    repaint();
  }
  // end Observer stub

  private void reset_fields() {
    for (AnnotationGridData agd : annotations) {
      reset_field_set(agd);
    }
  }

  private void reset_field_set (AnnotationGridData agd) {
    String[] all = agd.af.get_annotation_columns();
    agd.visible_fields = new ArrayList<String>();
    for (int i = 0; i < all.length; i++) {
      if (visible_annotations == null ? true : visible_annotations.contains(all[i]))
	agd.visible_fields.add(all[i]);
    }
  }


  private String[] reset_field_set(AnnotationFlatfile2 ff, boolean is_vasari) {
    ArrayList<String> fields_all = new ArrayList<String>();
    String[] all = ff.get_annotation_columns();
    for (int i = 0; i < all.length; i++) {
      fields_all.add(all[i]);
    }
    if (is_vasari) {
      // hack: Vasari annotation file has multiple index columns for
      // use with different heatmap formats, ensure none are displayed
      fields_all.remove("Sample_3");
      fields_all.remove("Sample_4");
      fields_all.remove("Sample_5");
    }

    HashSet<String> vis = visible_annotations;

    if (vis == null) {
      vis = new HashSet<String>();
      for (int i = 0; i < all.length; i++) {
	vis.add(all[i]);
      }
    }

    String[] fields;
    // fields = new String[visible_annotations.size()];
    // FIX ME: for some reason size() is not trustworthy (WTF? duplicates?)
    // ...this reports 1 more than actual count, which winds up creating
    // a null entry in the list, wreaking havok later...

    int visible = 0;
    // so rather than figuring out why, just lazily count manually
    for (String field : fields_all) {
      if (vis.contains(field)) visible++;
      // field enabled for viewing
    }
    fields = new String[visible];

    int j=0;
      
    for (String field : fields_all) {
      if (vis.contains(field)) fields[j++] = field;
    }
    return fields;
  }

  private int get_column_count() {
    int count=0;
    for (AnnotationGridData agd : annotations) {
      count += agd.visible_fields.size();
    }
    return count;
  }


}
