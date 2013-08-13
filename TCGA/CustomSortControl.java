package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class CustomSortControl extends Observable {
  private static final String LABEL_SHOW_ALL = "Show current ID ordering";
  private static final String LABEL_CLEAR = "Clear";
  private static final String LABEL_SORT = "Set sort order";
  private static final String LABEL_CANCEL = "Cancel";

  private JFrame jf;
  private JTextArea jta;
  private GenomicMeasurement gm;

  private static final String CLUSTER_BOUNDARY_MARKER = ">cluster_boundary";

  public CustomSortControl (GenomicMeasurement gm) {
    this.gm = gm;
    setup();
  }
  
  private JButton generate_jbutton (String label, ActionListener al, String tooltip_text) {
    // create JButton and add listener
    JButton jb = new JButton(label);
    jb.addActionListener(al);
    if (tooltip_text != null) jb.setToolTipText(tooltip_text);
    return jb;
  }


  private void setup() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

    JPanel sub_p = get_buffer_titled_panel(panel, "Specify sample ID ordering:");
    sub_p.setLayout(new BorderLayout());

    //
    //  user-entered markers
    //
    jta = new JTextArea(20,40);
    //    jta.setText("TCGA-06-0188\ntcga-02-0086\n");
    show_all_ids();

    JScrollPane jsp_samples = new JScrollPane(jta,
				      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				      //				      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
				      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
				      );

    sub_p.add("Center", jsp_samples);
    panel.add(sub_p);

    panel.add(sub_p = new JPanel());

    sub_p.add(generate_jbutton(LABEL_SORT,
			       new ActionListener() {
				 public void actionPerformed(ActionEvent e) {
				   sort_samples();
				 }
			       },
			       "Set the sample sort order.  If only a subset of the IDs are specified, they will be moved to the top of the current sort order."
			       ));

    sub_p.add(new JLabel("    "));

    sub_p.add(generate_jbutton(LABEL_SHOW_ALL,
			       new ActionListener() {
				 public void actionPerformed(ActionEvent e) {
				   show_all_ids();
				 }
			       },
			       "Reset list to show the current ordering of sample IDs in the dataset."
			       ));
    
    sub_p.add(generate_jbutton(LABEL_CLEAR,
			       new ActionListener() {
				 public void actionPerformed(ActionEvent e) {
				   jta.setText("");
				 }
			       },
			       "Clears the sample ID list."
			       ));
    
    sub_p.add(new JLabel("    "));

    sub_p.add(generate_jbutton(LABEL_CANCEL,
			       new ActionListener() {
				 public void actionPerformed(ActionEvent e) {
				   jf.setVisible(false);
				 }
			       }, null));

    jf = new JFrame("Set custom sort order");
    jf.add(panel);
    jf.pack();
    jf.setVisible(true);
  }

  public void setVisible (boolean v) {
    // emulate JFrame method
    jf.setVisible(v);
  }
  
  public void setState (int state) {
    // emulate JFrame method
    jf.setState(state);
  }
  
  public static void main (String [] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    try {
      //      GenomicMeasurement gm = new GenomicMeasurement("snp6_genomicmeasurement_broad_updated_cn.txt", false);
      //      GenomicMeasurement gm = new GenomicMeasurement("test.txt", false);
      //      GenomicMeasurement gm = new GenomicMeasurement("carl.txt", false);
      GenomicMeasurement gm = new GenomicMeasurement("p53.txt", false);

      if (true) {
	ClusterTool ct = new ClusterTool(gm);
	ct.set_cluster_stop_threshold(0.9f);
	ct.cluster(new PearsonRCluster(), false, false);
	// cluster to create dividers
      }

      CustomSortControl csc = new CustomSortControl(gm);
    } catch (Exception e) {
      System.err.println(e);  // debug
    }
  }

  private JPanel get_buffer_titled_panel (JPanel panel, String title) {
    // create a new line-bordered JPanel and add it to the given panel
    // FIX ME: move to common utility class...
    JPanel jp_buffer = new JPanel();
    jp_buffer.setLayout(new BorderLayout());
    jp_buffer.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    JPanel jp_titled = new JPanel();
    jp_titled.setLayout(new BoxLayout(jp_titled, BoxLayout.PAGE_AXIS));
    jp_titled.setBorder(BorderFactory.createTitledBorder(title));

    jp_buffer.add("Center", jp_titled);
    panel.add(jp_buffer);

    return jp_titled;
  }

  private void sort_samples() {
    SampleSortTools sst = new SampleSortTools(gm);
    HashMap<String,ArrayList<GenomicSample>> patient2samples = sst.get_patient2samples();

    String[] patients = jta.getText().split("\\s+");

    ArrayList<String> unique_patient_ids = gm.get_unique_patient_ids();
    HashSet<String> unique = new HashSet<String>(unique_patient_ids);

    HashSet<String> desired_order = new HashSet<String>();

    ArrayList<GenomicSample> rows_new = new ArrayList<GenomicSample>();
    HashSet<String> processed = new HashSet<String>();
    ArrayList<String> missing = new ArrayList<String>();

    ArrayList<Cluster> cluster_list = new ArrayList<Cluster>();
    ArrayList<GenomicSample> cluster_queue = new ArrayList<GenomicSample>();

    for (int i = 0; i < patients.length; i++) {
      String found_id = null;

      if (patients[i].length() == 0) continue;

      if (patients[i].equals(CLUSTER_BOUNDARY_MARKER)) {
	PearsonRCluster cluster = new PearsonRCluster();
	// hack (not really); maybe clone() existing?
	cluster.set_sample_data(cluster_queue);
	cluster_list.add(cluster);
	cluster_queue = new ArrayList<GenomicSample>();
	// don't have to worry about adding the last cluster reference at the end of
	// this process, since it actually consists of "leftovers"
	// FIX ME: possible bug, what if source clustering covered all entries??
	continue;
      }

      if (unique.contains(patients[i])) {
	found_id = patients[i];
      } else {
	for (String id : unique_patient_ids) {
	  if (patients[i].equalsIgnoreCase(id)) {
	    found_id = id;
	  }
	}
      }

      if (found_id == null) {
	missing.add(patients[i]);
      } else {
	rows_new.addAll(patient2samples.get(found_id));
	cluster_queue.addAll(patient2samples.get(found_id));
	processed.add(found_id);
      }
    }

    for (GenomicSample gs : gm.get_rows()) {
      if (!processed.contains(gs.patient_id)) {
	rows_new.add(gs);
      }
    }

    if (gm.get_rows().size() != rows_new.size()) {
      System.err.println("ERROR: size mismatch in custom sort!!");
    }

    if (missing.size() > 0) {
      String msg = "Can't find " + 
	missing.size() + " sample ID" + 
	(missing.size() == 1 ? "" : "s") +
	": " + Funk.Str.join(", ", missing.iterator());
      JOptionPane.showMessageDialog(jf, msg);
    }

    gm.set_rows(rows_new, true);
    if (cluster_list.size() > 0) gm.get_divider_manager().set_cluster_list(cluster_list);
    setVisible(false);
  }
  
  private void show_all_ids() {
    StringBuilder sb = new StringBuilder();

    HashSet<String> cluster_end = gm.get_divider_manager().get_last_patients_in_clusters();

    for (String id : gm.get_unique_patient_ids()) {
      sb = sb.append(id + "\n");
      if (cluster_end.contains(id)) sb = sb.append(CLUSTER_BOUNDARY_MARKER + "\n");
    }
    jta.setText(sb.toString());
  }

}

