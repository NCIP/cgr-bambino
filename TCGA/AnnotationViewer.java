package TCGA;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.*;

public class AnnotationViewer implements MouseListener,MouseMotionListener {
  private AnnotationFlatfile2 af;
  private GenomicMeasurement gm;
  private ImageScalePanel2 isp;
  private JTable table;
  
  //  private static int ROW_COUNT = 115;
  private static int ROW_COUNT = 115;
  // so, so wrong

  public AnnotationViewer (AnnotationFlatfile2 af, GenomicMeasurement gm, ImageScalePanel2 isp) {
    this.af = af;
    this.gm = gm;
    this.isp = isp;
    setup();
  }

  private void setup () {
    JFrame jf = new JFrame("Annotation Viewer");
    JPanel jp = new JPanel(new GridLayout(1,0));

    //    table = new JTable(ROW_COUNT, 2);
    table = new JTable(af.get_row_count(), 2);
    table.setPreferredScrollableViewportSize(new Dimension(500, 70));
    //    table.setFillsViewportHeight(true);

    DefaultTableModel tm = (DefaultTableModel) table.getModel();
    String[] labels = {"Label","Value"};
    tm.setColumnIdentifiers(labels);

    JScrollPane scrollPane = new JScrollPane(table);

    jp.add(scrollPane);

    isp.addMouseListener(this);

    //    isp.addMouseMotionListener(this);
    // kind of nice but then can't switch to widget to scroll through!!

    jf.add(jp);

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    try {
      jf.setPreferredSize(new Dimension((int) (screen.width * 0.30),
					(int) (screen.height * 0.95)));
    } catch (NoSuchMethodError e) {
      System.err.println("can't set pref JFrame size on this JVM");  // debug
    } 

    jf.pack();
    jf.setVisible(true);
  }

  public void display_row (Hashtable row) {
    ArrayList<String> keys = af.get_sorted_keys();
    int ri = 0;
    for (String key : keys) {
      table.setValueAt(key, ri, 0);
      table.setValueAt(row.get(key), ri++, 1);
    }
  }

  public void clear_table (String id) {
    table.setValueAt("ERROR: no annotations found!", 0, 0);
    table.setValueAt(id, 0, 1);
    //    for (int ri=1; ri < ROW_COUNT; ri++) {
    for (int ri=1; ri < af.get_row_count(); ri++) {
      table.setValueAt("", ri, 0);
      table.setValueAt("", ri, 1);
    }
  }

  private void show_annotations (Point mp) {
    if (af.is_loaded() && gm.is_loaded()) {
      Point p = isp.get_unscaled_point(mp);
      int index = p.y;

      ArrayList<GenomicSample> gm_rows = gm.get_visible_rows();
      // set of data painted vertically in component
      if (index >= gm_rows.size()) index=gm_rows.size() - 1;
      GenomicSample gs = gm_rows.get(index);
      ArrayList<Hashtable> annot = af.find_annotations(gs);
      if (annot != null) {
	Hashtable row = annot.get(0);
	display_row(row);
      } else {
	clear_table(gs.patient_id);
      }

      //      Vector rows = af.get_rows();
      //      Hashtable row = (Hashtable) rows.elementAt(index);
      //      display_row(row);
    }
  }

  // begin MouseMotionListener stubs
  public void mouseDragged(MouseEvent e) {}
  public void mouseMoved(MouseEvent e) {
    show_annotations(e.getPoint());    
  };
  // begin MouseMotionListener stubs


  // begin MouseListener stubs
  public void mouseClicked(MouseEvent e) {
    show_annotations(e.getPoint());
  };
  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

}
