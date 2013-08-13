package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class ChromScalePanel2 extends ScalePanel2 {
  
  private int BOX_HEIGHT;
  private GenomicSet gs;
  private static Color color_background = Color.white;
  private static Color color_text = Color.black;
  private static Color color_box = Color.black;

  private int raw_preferred_height;
  private ChromScaleClicker clicker;
  private static final String LABEL_HELP = "Help: data layout panel";

  public ChromScalePanel2 (GenomicSet gs, int raw_width) {
    //    super(new Dimension(800,600));
    this.gs = gs;

    Font f = getFont();
    FontMetrics fm = getFontMetrics(f);

    int font_height = fm.getHeight();
    BOX_HEIGHT = (int) (font_height * 0.8);

    int buffer = 0;
    int preferred_h = BOX_HEIGHT + buffer + font_height;

    set_raw_size(new Dimension(raw_width, preferred_h));
    setToolTipText("");

    clicker = new ChromScaleClicker(this);

    JPopupMenu jpm = new JPopupMenu();
    jpm.add(HelpLauncher.generate_jmenuitem(LABEL_HELP, HelpLauncher.ANCHOR_DATA_LAYOUT));
    new PopupListener(this, jpm);

  }
  
  protected void paintComponent(Graphics g) {
    Dimension d = getSize();
    //    Rectangle rd = new Rectangle(0,0,d.width,d.height);

    int start_x = get_unscaled_x_start();
    int end_x = get_unscaled_x_end() + 1;
    // +1: allow extra for scaled bin central values

    Rectangle rd = new Rectangle(start_x, 0, (end_x - start_x) + 1, 1);

    g.setColor(Color.white);
    g.fillRect(0,0,d.width,d.height);

    Font f = getFont();
    FontMetrics fm = getFontMetrics(f);

    float x_scale = get_horizontal_scale_level();

    g.setColor(color_box);
    g.fillRect(0,0,d.width,BOX_HEIGHT);
    int x,y;

    int labels_drawn = 0;
    int X_BUFFER = 2;

    boolean labels_truncated = false;

    if (Options.are_bins_paintable(gs, this)) {
      for (GenomicBin gb : gs.get_bins()) {
	//
	//  draw chr border lines in box
	// 
	g.setColor(color_background);
      
	float bin_center = gb.center - 1;
	// -1 : bin numbers start with 1

	if (gb.end >= start_x && gb.end <= end_x) {
	  // mark end of chromosome if visible
	  x = (int) ((gb.end - start_x) * x_scale);
	  g.drawLine(x,0,x,BOX_HEIGHT);
	  // mark chr boundaries
	}
      
	//      System.err.println("bin " + gb.bin_name + " center_adj=" + bin_center + " start_x:" + start_x + " end_x:" + end_x);  // debug

	x = y = 0;

	Rectangle r_bin = gb.get_rectangle();
	if (rd.intersects(r_bin)) {
	  //
	  //  some portion of this bin is visible
	  //
	  String label = gb.bin_name;
	  if (labels_drawn++ > 0 && label.length() > 3 && label.substring(0,3).equals("chr")) {
	    // chromosome label: trim second and later instances
	    label = label.substring(3);
	  }

	  int lw = fm.stringWidth(label);
	  int x_raw = (int) ((gb.center - 1 - start_x) * x_scale);
	  x = x_raw - (lw / 2);
	  // translate position for label in center

	  if (x < 0) {
	    // label is offscreen to the left
	    int bin_end_x = (int) ((gb.end - start_x) * x_scale);
	    x = X_BUFFER;
	    int label_end = x + lw;
	    if (label_end > bin_end_x) {
	      x -= (label_end - bin_end_x);
	      //	    System.err.println("end_x="+bin_end_x + " label_x="+label_end);  // debug
	    }
	  } else if (x > d.width) {
	    // label is offscreen to the right
	    x = d.width - lw - X_BUFFER;
	    int bin_start_x = (int) ((gb.start - start_x) * x_scale);
	    if (x < bin_start_x) {
	      x = bin_start_x;
	    }
	  }

	  // ensure the label fits within the bin boundaries:
	  int bsx = (int) (((gb.start - start_x) - 1) * x_scale);
	  int bex = (int) ((gb.end - start_x) * x_scale);
	  boolean first = true;
	  int len;
	  while (true) {
	    if (x < bsx || (x + lw) > bex) {
	      //	      System.err.println(label + " " + bsx + "-" + bex + " x=" +x + " lw=" + lw);  // debug
	      len = label.length();
	      if (len <= 1) {
		// can't shrink anymore
		break;
	      } else {
		if (first) {
		  label = label + ">";
		  // dummy character for first truncation
		  first = false;
		}
	      
		label = label.substring(0, len - 2) + ">";
		// ">" indicates the label has been truncated on the right
		labels_truncated = true;
		lw = fm.stringWidth(label);
		x = x_raw - (lw / 2);
	      }
	    } else {
	      break;
	    }
	  }

	  
	  g.setColor(color_text);
	  y = BOX_HEIGHT + fm.getAscent();
	  g.drawString(label, x, y);
	}
      }


    }
  }

  public static void main (String [] argv) {
    try {
      GenomicSet gs = new GenomicSet("CopyNumberGenomicSet.txt");

      JFrame jf = new JFrame();
      jf.setLayout(new BorderLayout());
      jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      //      jf.setSize(800,600);

      ChromScalePanel2 csp = new ChromScalePanel2(gs, 15000);
      JScrollBar jsb_h = new JScrollBar(JScrollBar.HORIZONTAL, 0, 0, 0, 15000);
      csp.set_horizontal_scrollbar(jsb_h);
      csp.setPreferredSize(new Dimension(1024,30));
      
      jf.add("Center", csp);
      jf.add("South", jsb_h);
      jf.pack();
      jf.setVisible(true);
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      e.printStackTrace();
    }
  }

  public String get_chr_label (int chr) {
    String label = Integer.toString(chr);
    if (chr == 23) label = "X";
    if (chr == 24) label = "Y";
    return label;
  }
  
  public GenomicBin get_mouse_bin (Point p) {
    Point up = get_unscaled_point(p);
    GenomicBin result = null;
    for (GenomicBin gb : gs.get_bins()) {
      if (up.x >= gb.start - 1 && up.x <= gb.end - 1) {
	result = gb;
	break;
      }
    }
    return result;
  }

  public String getToolTipText(MouseEvent e) {
    GenomicBin gb = get_mouse_bin(e.getPoint());
    return gb == null ? null : "double-click to zoom to " + gb.bin_name;
  }

  public ChromScaleClicker get_clicker() {
    return clicker;
  }



}
