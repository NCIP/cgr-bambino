package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import layout.SpringUtilities;

public class ClusterControl implements ActionListener,VisibilityToggle {
  // controls for frequency-based cluster
  private JFrame jf;
  private JComboBox jc_multi, jc_subsets, jc_cluster_values;
  private JCheckBox jx_rearrange;
  private JTextField tf_threshold;

  private GenomicMeasurement gm;
  private GenomicSet gs;
  private SampleSubsets sample_subsets;

  private static final String LABEL_OK = "OK";
  private static final String LABEL_CANCEL = "Cancel";
  
  public ClusterControl() {
    // debug only
    setup();
  }
  
  public ClusterControl(GenomicMeasurement gm, GenomicSet gs) {
    this.gm = gm;
    this.gs = gs;
    this.sample_subsets = gm.get_sample_subsets();
    setup();
  }
  
  private void setup() {
    jf = new JFrame();
    jf.setTitle("Cluster control");
    JPanel jp_main = new JPanel();
    jp_main.setLayout(new BoxLayout(jp_main, BoxLayout.PAGE_AXIS));

    JPanel jp_controls = new JPanel();
    jp_controls.setLayout(new SpringLayout());

    Vector subsets = new Vector();
    subsets.add(SampleSortTools.CLUSTER_SUBSET_ALL);
    if (sample_subsets != null) {
      subsets.addAll(sample_subsets.get_visible_subsets());
    }
    jc_subsets = new JComboBox(subsets);
    jp_controls.add(new JLabel("subset to base clustering on:", JLabel.TRAILING));
    jp_controls.add(jc_subsets);

    jp_controls.add(new JLabel("values to cluster by:", JLabel.TRAILING));
    Vector cluster_values = new Vector();
    cluster_values.add(SampleSortTools.CLUSTER_VALUE_ANY);
    cluster_values.add(SampleSortTools.CLUSTER_VALUE_POSITIVE);
    cluster_values.add(SampleSortTools.CLUSTER_VALUE_NEGATIVE);
    jc_cluster_values = new JComboBox(cluster_values);
    jp_controls.add(jc_cluster_values);

    jp_controls.add(new JLabel("minimum value to cluster:", JLabel.TRAILING));
    jp_controls.add(tf_threshold = new JTextField("1.0"));

    jp_controls.add(new JLabel("if multiple rows per sample, use:", JLabel.TRAILING));
    Vector multi = new Vector();
    multi.add(SampleSortTools.AVERAGE_VALUE);
    multi.add(SampleSortTools.HIGHEST_ABSOLUTE_VALUE);
    jc_multi = new JComboBox(multi);
    jp_controls.add(jc_multi);

    jp_controls.add(new JLabel("rearrange columns after clustering?:", JLabel.TRAILING));
    jp_controls.add(jx_rearrange = new JCheckBox());
    jx_rearrange.setSelected(true);

    SpringUtilities.makeCompactGrid(jp_controls,
				    5, 2,
				    // rows, columns
				    6,6,
				    6,6);

    //
    // control buttons:
    //
    JPanel jp_buttons = new JPanel();
    JButtonGenerator jbg = new JButtonGenerator(this, jp_buttons);
    
    jbg.generate_jbutton(LABEL_OK);
    jbg.generate_jbutton(LABEL_CANCEL);
    
    jp_main.add(jp_controls);
    jp_main.add(jp_buttons);

    jf.getContentPane().add(jp_main);
    
    jf.pack();
    //    jf.setVisible(true);
  }

  public void setVisible (boolean v) {
    jf.setVisible(v);
  }

  
  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src instanceof AbstractButton) {
      String label = ((AbstractButton) src).getText();
      if (label.equals(LABEL_OK)) {
	float threshold = Float.parseFloat(tf_threshold.getText());
	// FIX ME: parse exceptions...

	SampleSortTools sst = new SampleSortTools(gm, gs);
	sst.set_minimum_value_to_cluster(threshold);
	sst.set_rearrange_columns(jx_rearrange.isSelected());
	sst.set_multi_sample_representative_method((String) jc_multi.getSelectedItem());
	sst.set_subset_filter((String) jc_subsets.getSelectedItem());
	sst.set_value_filter((String) jc_cluster_values.getSelectedItem());

	sst.sort_by_cluster();
      }
    }
    jf.setVisible(false);
    jf.dispose();
  }
  // end ActionListener stub

  public static void main (String [] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    ClusterControl cc = new ClusterControl();
  }


}
