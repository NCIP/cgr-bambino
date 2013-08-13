package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import layout.SpringUtilities;

public class PearsonClusterControl extends StatClusterControl {
  private JSlider js_minimum_r;
  private JCheckBox jc_cluster_empty;

  public PearsonClusterControl(GenomicMeasurement gm, Observer o) {
    super(gm, o);
  }

  public Cluster get_control_cluster () {
    return new PearsonRCluster();
  }

  public int init_control_components(JPanel jp) {
    //
    // build and return custom components
    //
    Cluster control_cluster = get_control_cluster();
    int rows_used = 0;

    js_minimum_r = new JSlider(0,100,(int) (control_cluster.get_default_cluster_stop_threshold() * 100));
    js_minimum_r.setToolTipText("Higher values produce more tightly-correlated clusters, lower values produce more inclusive clusters.");

    int major_tick_spacing = 10;

    js_minimum_r.setMajorTickSpacing(major_tick_spacing);
    js_minimum_r.setMinorTickSpacing(1);
    js_minimum_r.setPaintTicks(true);
    js_minimum_r.setPaintLabels(true);
    js_minimum_r.setSnapToTicks(true);
    
    Hashtable labels = new Hashtable();
    for (int i = 0; i <= 100; i += major_tick_spacing) {
      double frac = ((double) i) / 100;
      //      String label = frac + "";
      labels.put(new Integer(i), new JLabel(new Double(frac).toString()));
    }
    js_minimum_r.setLabelTable(labels);

    jp.add(new JLabel("Minimum r to cluster sequences:", JLabel.TRAILING));
    jp.add(js_minimum_r);
    rows_used++;

    jp.add(new JLabel("Cluster empty samples:", JLabel.TRAILING));
    jp.add(jc_cluster_empty = new JCheckBox());
    //    jc_cluster_empty.setSelected(true);
    jc_cluster_empty.setSelected(false);

    jc_cluster_empty.setToolTipText("Pearson's r is uncomputable between empty rows; this option preclusters empty samples together.");
    rows_used++;

    return rows_used;
  }

  public void final_panel_setup (JPanel jp) {
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension pref = jp.getPreferredSize();
    pref.width = (int) (screen.width * 0.5);
    jp.setPreferredSize(pref);
  }

  public String cluster_setup(ClusterTool ct) {
    double min_r = ((double) js_minimum_r.getValue()) / 100;
    //    System.err.println("min r = " + min_r);  // debug
    ct.set_cluster_stop_threshold(min_r);

    PearsonRCluster.CLUSTER_EMPTY_SEQUENCES = jc_cluster_empty.isSelected();
    // hacktacular
    return null;
  }

}
