package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import layout.SpringUtilities;

public abstract class StatClusterControl implements ActionListener,VisibilityToggle {
  // 
  //  shared/baseline interface for statistical-based clustering
  //
  private JFrame jf;
  private JComboBox jc_distance;
  private JCheckBox jc_separate, jc_zoom, jc_report;

  private GenomicMeasurement gm;
  private ClusterTool ct;

  private static final String LABEL_OK = "OK";
  private static final String LABEL_CANCEL = "Cancel";

  private static final String LABEL_DISTANCE_MAX = "Maximum (complete linkage)";
  private static final String LABEL_DISTANCE_MIN = "Minimum (single linkage)";
  private static final String LABEL_DISTANCE_AVG = "Mean (average linkage)";

  // implemented by subclasses:
  protected abstract Cluster get_control_cluster();
  protected abstract int init_control_components(JPanel jp);
  protected abstract String cluster_setup(ClusterTool ct);
  protected abstract void final_panel_setup(JPanel jp);
  
  public StatClusterControl() {
    // debug only
    setup();
  }
  
  public StatClusterControl(GenomicMeasurement gm, Observer o) {
    this.gm = gm;
    ct = new ClusterTool(gm);
    ct.addObserver(o);
    setup();
  }
  
  private void setup() {
    jf = new JFrame();
    Cluster cc = get_control_cluster();

    jf.setTitle(get_control_cluster().get_method_name() + " cluster control");
    JPanel jp_main = new JPanel();
    jp_main.setLayout(new BoxLayout(jp_main, BoxLayout.PAGE_AXIS));

    JPanel jp_controls = new JPanel();
    jp_controls.setLayout(new SpringLayout());

    //
    //  add custom control component used by subclass:
    //
    int rows_used = init_control_components(jp_controls);

    //
    // shared component setup
    //
    Vector distances = new Vector();
    distances.add(LABEL_DISTANCE_MAX);
    distances.add(LABEL_DISTANCE_AVG);
    distances.add(LABEL_DISTANCE_MIN);
    jc_distance = new JComboBox(distances);

    //    jp_controls.add(new JLabel("Distance metric to join clusters:", JLabel.TRAILING));
    jp_controls.add(new JLabel("Cluster-joining metric:", JLabel.TRAILING));
    jp_controls.add(jc_distance);
    rows_used++;

    jp_controls.add(new JLabel("Place unclustered empty samples at bottom:", JLabel.TRAILING));
    jp_controls.add(jc_separate = new JCheckBox());
    jc_separate.setToolTipText("Samples which can't be clustered are placed at the bottom of the display.  If this checkbox is set and any of these samples have no underlying data, they are shown at the very bottom.");
    jc_separate.setSelected(true);
    rows_used++;

    jp_controls.add(new JLabel("Zoom to cluster results:", JLabel.TRAILING));
    jp_controls.add(jc_zoom = new JCheckBox());
    jc_zoom.setSelected(true);
    rows_used++;

    jp_controls.add(new JLabel("Generate report:", JLabel.TRAILING));
    jp_controls.add(jc_report = new JCheckBox());
    jc_report.setSelected(true);
    rows_used++;

    SpringUtilities.makeCompactGrid(jp_controls,
				    rows_used, 2,
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

    final_panel_setup(jp_main);

    jf.pack();
    //    jf.setVisible(true);
  }

  public void setVisible (boolean v) {
    jf.setVisible(v);
  }
  
  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    String error = null;
    if (src instanceof AbstractButton) {
      String label = ((AbstractButton) src).getText();
      if (label.equals(LABEL_OK)) {
	String distance = (String) jc_distance.getSelectedItem();
	int method = -1;
	if (distance.equals(LABEL_DISTANCE_MAX)) {
	  method = Cluster.DISTANCE_METHOD_COMPLETE;
	} else if (distance.equals(LABEL_DISTANCE_AVG)) {
	  method = Cluster.DISTANCE_METHOD_MEAN;
	} else if (distance.equals(LABEL_DISTANCE_MIN)) {
	  method = Cluster.DISTANCE_METHOD_MINIMUM;
	}

	//	debug_r();
	
	ct.set_distance_method(method);
	ct.set_wants_zoom(jc_zoom.isSelected());

	error = cluster_setup(ct);
	if (error == null) {
	  ct.cluster(get_control_cluster(), jc_separate.isSelected(), jc_report.isSelected());
	}

      }
    }

    if (error == null) {
      jf.setVisible(false);
      jf.dispose();
    } else {
      JOptionPane.showMessageDialog(jf,
				    error,
				    "Error",
				    JOptionPane.ERROR_MESSAGE);
    }
  }
  // end ActionListener stub

  private void debug_r () {
    System.err.println("DEBUG");  // debug
    PearsonRCluster c = new PearsonRCluster();

    SampleSortTools sst = new SampleSortTools(gm);
    HashMap<String,ArrayList<GenomicSample>> patient2sample = sst.get_patient2samples();

    for (GenomicSample b1 : patient2sample.get("TCGA-02-0089")) {
      for (GenomicSample b2 : patient2sample.get("TCGA-02-0432")) {
	double result = -999;
	try {
	  result = c.compute_single_distance(b1.copynum_data, b2.copynum_data);
	} catch (Exception e) {
	}
	System.err.println(b1.sample_id + " vs " + b2.sample_id + " dist=" + result);
      }
    }

  }


}
