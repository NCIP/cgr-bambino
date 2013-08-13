package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

import java.util.*;

public class TrackPanel extends SummaryPanel implements MouseListener {
  private static int BUFFER_HEIGHT = 1;
  // HACK
  private TrackInfo ti;
  private int image_height = 0;

  private static Color COLOR_TEXT = Color.black;
  private Color COLOR_BACKGROUND = Color.white;
  private SelectionNotifier sn = null;

  public TrackPanel (TrackInfo ti, JScrollBar jsb_h) {
    this.ti = ti;
    Font f = new Font("Times", Font.BOLD, Options.SUMMARY_PANEL_FONT_HEIGHT);
    FontMetrics fm = getFontMetrics(f);
    image_height = fm.getHeight();
    set_raw_size(new Dimension(ti.get_width(), image_height));
    set_horizontal_scrollbar(jsb_h);
    setToolTipText(ti.get_name());
    addMouseListener(this);
    sn = new SelectionNotifier();
  }

  protected void paintComponent(Graphics g) {
    Dimension d = getSize();

    g.setColor(COLOR_BACKGROUND);
    g.fillRect(0,0,d.width,d.height);

    int avail_y = image_height - (BUFFER_HEIGHT * 2);

    byte max_v = ti.get_max_value();

    float scale_by = (float) avail_y / max_v;
    //    System.err.println("avail_y:"+ avail_y + " scale:" + scale_by + " digesting:");  // debug
    ArrayList<Point> any = digest_array(ti.get_active(),
					max_v,
					scale_by,
					false);
    g.setColor(ti.get_color());
    render_array(g, any, d.height - BUFFER_HEIGHT, false);

    FontMetrics fm = getFontMetrics(get_label_font());
    draw_label(g, ti.get_name(), COLOR_BACKGROUND, COLOR_TEXT, (d.height/2)+fm.getDescent());
  }

  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {
    Point p = get_unscaled_point(e.getPoint());
    if (e.getClickCount() > 1) {
      Rectangle sel = ti.get_range_selection(p.x);
      if (sel != null) sn.set_selection(sel);
    } 
  };
  public void mouseClicked(MouseEvent e) {};
  
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs


  public SelectionNotifier get_selection_notifier() {
    return sn;
  }

  public String getToolTipText(MouseEvent e) {
    Point mp = get_unscaled_point(e.getPoint());
    Range r = ti.get_range(mp.x);
    String name = ti.get_name();
    String text = null;
    if (r != null) {
      text = "double-click to zoom to " + name;
    }
    return text;
  }

}
