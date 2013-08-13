package TCGA;

import javax.swing.*;
import java.awt.event.*;
import java.net.URL;

public class HelpLauncher implements ActionListener {
  private String anchor = null;
  
  public static final String ANCHOR_HEATMAP = "panel_heatmap";
  public static final String ANCHOR_SUMMARY_INCREASE_DECREASE = "panel_increase_decrease";
  public static final String ANCHOR_SUMMARY_ANY = "panel_any_variation";
  public static final String ANCHOR_ANNOTATIONS = "panel_annotations";
  public static final String ANCHOR_DATA_LAYOUT = "panel_data_layout";
  public static final String ANCHOR_COLOR_CONTRAST = "color_contrast";
  public static final String ANCHOR_NAVIGATION = "navigation";
  public static final String ANCHOR_ZOOM = "zoom";
  // HTML anchors in documentation

  public HelpLauncher (String anchor) {
    this.anchor = anchor;
  }

  public HelpLauncher (AbstractButton b, String anchor) {
    this.anchor = anchor;
    b.addActionListener(this);
  }

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    launch_url();
  }
  // end ActionListener stub

  public void launch_url() {
    String spec = "/goldenPath/heatmap/documentation/index.html";
    if (anchor != null) spec = spec.concat("#" + anchor);
    URLLauncher.launch_modified_url(spec, "heatmaps");
  }

  public static JMenuItem generate_jmenuitem(String label, String anchor) {
    // generate and return a JMenuItem with the given label,
    // creating HelpLauncher to monitor the JMenuItem and launch help w/given anchor
    JMenuItem jmi = new JMenuItem(label);
    new HelpLauncher(jmi, anchor);
    return jmi;
  }



}
