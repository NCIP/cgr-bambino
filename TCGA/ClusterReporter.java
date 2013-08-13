package TCGA;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.*;

public class ClusterReporter {
  private JTable table;
  private ClusterTool ct;
  private GenomicMeasurement gm;

  public ClusterReporter(ClusterTool ct, GenomicMeasurement gm) {
    this.ct = ct;
    this.gm = gm;
    setup();
  }

  private void setup() {
    ArrayList<Cluster> cluster_list = ct.get_clusters();

    if (cluster_list.size() > 0) {
      JFrame jf = new JFrame("Clustering report");

      jf.getContentPane().setLayout(new BorderLayout());

      String[] labels = {"Cluster",
			 "Sample count",
			 "Patient count",
			 "Internal distance",
			 "Members"};

      table = new JTable(cluster_list.size() + 7, labels.length);
      //    table.setPreferredScrollableViewportSize(new Dimension(500, 70));
      table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

      DefaultTableModel tm = (DefaultTableModel) table.getModel();
      tm.setColumnIdentifiers(labels);
      table.getColumnModel().setColumnMargin(5);

      JScrollPane scrollPane = new JScrollPane(table);

      int cluster_number = 0;
      int row_index = 0;
      int samples_clustered = 0;
      for (Cluster c : cluster_list) {
	ArrayList<GenomicSample> samples = c.get_sample_data();
	ArrayList<String> patients = GenomicSample.get_patient_id_list(samples);
	String members = Funk.Str.join(", ", patients.iterator());
	int col_index=0;
	table.setValueAt(row_index + 1, row_index, col_index++);
	samples_clustered += c.size();
	table.setValueAt(c.size(), row_index, col_index++);
	table.setValueAt(patients.size(), row_index, col_index++);
	table.setValueAt(c.get_internal_distance(), row_index, col_index++);
	table.setValueAt(members, row_index, col_index++);
	row_index++;
      }

      ArrayList<GenomicSample> leftovers = gm.get_unordered_samples();

      ArrayList<String> patients = GenomicSample.get_patient_id_list(leftovers);
      String members = Funk.Str.join(", ", patients.iterator());
      int col_index=0;
      table.setValueAt("unclustered", row_index, col_index++);
      table.setValueAt(leftovers.size(), row_index, col_index++);
      table.setValueAt(patients.size(), row_index, col_index++);
      table.setValueAt("n/a", row_index, col_index++);
      table.setValueAt(members, row_index, col_index++);
      row_index++;

      row_index++;
      table.setValueAt("distance metric:", row_index, 0);
      table.setValueAt(cluster_list.get(0).get_method_name(), row_index, 1);
      row_index++;
      table.setValueAt("cluster joining method:", row_index, 0);
      table.setValueAt(Cluster.describe_distance_method(ct.get_distance_method()), row_index, 1);

      row_index++;
      table.setValueAt("stop threshold:", row_index, 0);
      table.setValueAt(ct.get_cluster_stop_threshold(), row_index, 1);

      row_index++;
      int cluster_percent = (int) ((double) samples_clustered * 100) / (samples_clustered + leftovers.size());
      table.setValueAt("percent of samples clustered:", row_index, 0);
      table.setValueAt(cluster_percent, row_index, 1);

      table.setPreferredScrollableViewportSize(table.getPreferredSize());
      TableTools.calcColumnWidths(table);

      jf.getContentPane().add("Center", scrollPane);
      jf.pack();
      jf.setVisible(true);
    } else {
      System.err.println("error: no clusters");  // debug
    }
  }



}
