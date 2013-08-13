package TCGA;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import layout.SpringUtilities;

public class SubsetCombinerControl implements ControlFrameListener {
  private ControlFrame jf;
  private HeatmapConfiguration config;
  private JComboBox jc_sub1, jc_logic1, jc_value1;
  private JComboBox jc_sub2, jc_logic2, jc_value2;
  private JComboBox jc_join;
  private JTextField jt_value1, jt_value2;

  public SubsetCombinerControl (HeatmapConfiguration config) {
    this.config = config;
    setup();
  }

  private void setup() {
    jf = new ControlFrame(this);

    SampleSubsets ss = config.gm.get_sample_subsets();
    if (ss.isEmpty()) {
      System.err.println("ERROR: no samples in subset");  // debug
    } else {
      JPanel jp = new JPanel();
      jp.setLayout(new SpringLayout());

      ArrayList<String> subsl = ss.get_subsets_arraylist();
      Collections.sort(subsl);
      Vector subs = new Vector(subsl);

      Vector logic_comp = new Vector();
      logic_comp.add(ByteComparator.LABEL_NE);
      logic_comp.add(ByteComparator.LABEL_GT);
      logic_comp.add(ByteComparator.LABEL_GE);
      logic_comp.add(ByteComparator.LABEL_LT);
      logic_comp.add(ByteComparator.LABEL_LE);
      logic_comp.add(ByteComparator.LABEL_EQ);
      Vector logic_op = new Vector();
      logic_op.add(BooleanComparator.LABEL_AND);
      logic_op.add(BooleanComparator.LABEL_OR);
      
      jp.add(jc_sub1 = new JComboBox(subs));
      jp.add(jc_logic1 = new JComboBox(logic_comp));
      jp.add(jt_value1 = new JTextField("0"));

      jp.add(new JLabel(""));
      jp.add(jc_join = new JComboBox(logic_op));
      jp.add(new JLabel(""));

      jp.add(jc_sub2 = new JComboBox(subs));
      jp.add(jc_logic2 = new JComboBox(logic_comp));
      jp.add(jt_value2 = new JTextField("0"));

      SpringUtilities.makeCompactGrid(jp,
				      3, 3,
				      // rows, columns
				      6,6,
				      6,6);

      jf.setTitle("Combine data subsets");
      jf.setLayout(new BorderLayout());
      jf.add("Center", jp);
      jf.add("South", jf.generate_panel(jf.PANEL_OK_APPLY_CANCEL));
      jf.pack();
      jf.setVisible(true);
    }

  }

  public void setVisible (boolean v) {
    jf.setVisible(v);
  }
  
  public static void main(String[] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    try {
      HeatmapConfiguration config = new HeatmapConfiguration();
      config.gm = new GenomicMeasurement("65_binary.gz", false);
      new SubsetCombinerControl(config);
    } catch (Exception e) {
      System.err.println("error:"+e);  // debug
    }
  }

  public void apply_changes() {
    String subset_1 = (String) jc_sub1.getSelectedItem();
    byte value_1 = Byte.parseByte(jt_value1.getText());
    String logic_1 = (String) jc_logic1.getSelectedItem();
    ByteComparator bc_1 = ByteComparator.get_comparator(logic_1, value_1);

    String subset_2 = (String) jc_sub2.getSelectedItem();
    byte value_2 = Byte.parseByte(jt_value2.getText());
    String logic_2 = (String) jc_logic2.getSelectedItem();
    ByteComparator bc_2 = ByteComparator.get_comparator(logic_2, value_2);

    String join = (String) jc_join.getSelectedItem();
    BooleanComparator bc_join = BooleanComparator.get_comparator(join);

    System.err.println(subset_1 + " " + subset_2);  // debug

    SubsetCombiner ssc = new SubsetCombiner(config.gm,
					    subset_1, bc_1,
					    subset_2, bc_2,
					    bc_join
					    );

    HeatmapConfiguration hc = new HeatmapConfiguration();
    hc.exit_on_close = false;
    hc.show_up_down_histogram = false;
    // combined data will have no negative values
    hc.gs = config.gs;
    hc.gm = ssc.get_results();
    hc.parent_gm = config.gm;
    hc.parent_subsets = new ArrayList<String>();
    hc.parent_subsets.add(subset_1);
    hc.parent_subsets.add(subset_2);

    try {
      Heatmap6 hm = new Heatmap6(hc);
    } catch (Exception ex) {
      System.err.println("error:"+ex);  // debug
    }

  }


}
