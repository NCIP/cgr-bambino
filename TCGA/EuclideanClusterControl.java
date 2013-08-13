package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import layout.SpringUtilities;

public class EuclideanClusterControl extends StatClusterControl {
  private JTextField jt_threshold, jt_limiter;
  private static String DEFAULT_DISTANCE = "1.25";
  private static String DEFAULT_LIMIT_DISTANCE = "2";

  public static byte EUCLIDEAN_LIMIT_DISTANCE = 0;
  // hack

  public EuclideanClusterControl(GenomicMeasurement gm, Observer o) {
    super(gm, o);
  }

  public Cluster get_control_cluster () {
    return new EuclideanCluster();
  }

  public void final_panel_setup (JPanel jp) {
  }

  public int init_control_components(JPanel jp) {
    jp.add(new JLabel("Maximum Euclidean distance to cluster sequences:", JLabel.TRAILING));
    jp.add(jt_threshold = new JTextField(6));
    jt_threshold.setText(DEFAULT_DISTANCE);

    jp.add(new JLabel("Limit distance on +/- axis to maximum of:", JLabel.TRAILING));
    jp.add(jt_limiter = new JTextField(6));
    jt_limiter.setText(DEFAULT_LIMIT_DISTANCE);
    jt_limiter.setToolTipText("Limit positive and negative values for purposes distance calculation. e.g. copy-# distance between -2 and +2 is more interesting than between +2 and +6.");

    return 2;
  }
  
  // begin ActionListener stub
  public String cluster_setup(ClusterTool ct) {
    String tval = jt_threshold.getText();
    String tlimit = jt_limiter.getText();
    double max_e_dist = 0;
    String error = null;

    try {
      max_e_dist = Double.parseDouble(tval);
      if (max_e_dist < 0) {
	error = "Euclidean distance must be a positive number (fractional values are allowed).";
      } else {
	ct.set_cluster_stop_threshold(max_e_dist);
      }
    } catch (Exception ex) {
      error = "Euclidean distance must be a positive number.";
    }

    try {
      if (tlimit.length() == 0) {
	EUCLIDEAN_LIMIT_DISTANCE = 0;
	// disable
      } else {
	EUCLIDEAN_LIMIT_DISTANCE = Byte.parseByte(tlimit);
	if (EUCLIDEAN_LIMIT_DISTANCE < 0) {
	  EUCLIDEAN_LIMIT_DISTANCE = 0;
	  error = "Limiter distance must be a positive integer.";
	}
      }
    } catch (Exception ex) {
      error = "Euclidean limiter distance must be a positive number.";
    }

    return error;
  }
  // end ActionListener stub

  public static void main (String [] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    //    EuclideanClusterControl ecc = new EuclideanClusterControl();
  }


}
