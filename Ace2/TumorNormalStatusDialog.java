package Ace2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import layout.SpringUtilities;

public class TumorNormalStatusDialog implements ItemListener {
  private JDialog jd;
  private static final String TUMOR = "tumor";
  private static final String NORMAL = "normal";
  private static final String UNSPECIFIED = "unspecified";
  private JFrame parent_frame;
  private ArrayList<SAMResource> sams;
  private JComboBox jcb_source, jcb_tn;
  private HashMap<String,SAMResource> basename2sr;

  public TumorNormalStatusDialog() {
    // debug only
    sams = null;
    parent_frame = null;
    setup();
  }

  public TumorNormalStatusDialog(JFrame parent_frame, ArrayList<SAMResource> sams) {
    this.parent_frame = parent_frame;
    this.sams = sams;
    setup();
  }

  private void setup() {
    if (parent_frame == null) {
      jd = new JDialog();
    } else {
      jd = new JDialog(parent_frame, true);
    }

    JPanel jp_main = new JPanel();
    jp_main.setLayout(new BoxLayout(jp_main, BoxLayout.PAGE_AXIS));

    JPanel jp_controls = get_buffer_titled_panel(jp_main, "Set tumor/normal status");
    jp_controls.setLayout(new SpringLayout());
    
    int rows = 0;
    int columns = 2;
    
    jp_controls.add(new JLabel("BAM file: ", SwingConstants.RIGHT));
    Vector items = new Vector();
    basename2sr = new HashMap<String,SAMResource>();
    if (sams == null) {
      items.add("file1");
      items.add("file2");
    } else {
      for (SAMResource sr : sams) {
	String basename = sr.get_basename();
	basename2sr.put(basename, sr);
	items.add(basename);
      }
    }
    jcb_source = new JComboBox(items);
    jcb_source.addItemListener(this);
    jp_controls.add(jcb_source);
    rows++;

    jp_controls.add(new JLabel("data type: ", SwingConstants.RIGHT));
    items = new Vector();
    items.add(TUMOR);
    items.add(NORMAL);
    items.add(UNSPECIFIED);
    jcb_tn = new JComboBox(items);
    jcb_tn.addItemListener(this);
    jp_controls.add(jcb_tn);
    rows++;

    int border_pad = 2;
    SpringUtilities.makeCompactGrid(jp_controls,
 				    rows, columns,
 				    // rows, columns
				    border_pad, border_pad, border_pad, border_pad
				    );

    JButton jb_close = new JButton("Close");
    jb_close.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  jd.setVisible(false);
	  jd.dispose();
	}
      });

    JPanel jp_buttons = new JPanel();
    jp_buttons.add(jb_close);
    jp_main.add(jp_buttons);

    jd.getContentPane().add(new JScrollPane(jp_main));

    change_source();
    jd.pack();
    jd.setTitle("Tumor/normal status");
    jd.setVisible(true);
  }

  private JPanel get_buffer_titled_panel (JPanel panel, String title) {
    JPanel jp_buffer = new JPanel();
    jp_buffer.setLayout(new BorderLayout());
    //    jp_buffer.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    // why doesn't this work???  appears to add padding just at top, see example below
    //
    //    jp_buffer.setBorder(BorderFactory.createEmptyBorder(100,100,100,100));

    JPanel jp_titled = new JPanel();
    jp_titled.setLayout(new BoxLayout(jp_titled, BoxLayout.PAGE_AXIS));
    jp_titled.setBorder(BorderFactory.createTitledBorder(title));

    jp_buffer.add("Center", jp_titled);
    panel.add(jp_buffer);

    return jp_titled;
  }
  
  public static void main (String[] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    new TumorNormalStatusDialog();
  }

  private void change_source() {
    String basename = (String) jcb_source.getSelectedItem();
    SAMResource sr = basename2sr.get(basename);
    if (sr != null) {
      //      System.err.println("change " + basename+ " " + sr);  // debug
      Sample sample = sr.get_sample();
      String value;
      if (sample.is_normal()) {
	value = NORMAL;
      } else if (sample.is_tumor()) {
	value = TUMOR;
      } else {
	value = UNSPECIFIED;
      }
      jcb_tn.setSelectedItem(value);
    } else {
      System.err.println("ERROR: no SAMResource for " + basename);  // debug
    }
  }

  // begin ItemListener stub
  public void itemStateChanged(ItemEvent e) {
    Object source = e.getSource();
    if (source.equals(jcb_source)) {
      //
      // source combo box has changed: set tumor/normal to appropriate value
      //
      change_source();
    } else if (source.equals(jcb_tn)) {
      //
      // tumor/normal combo box has changed:
      // set underlying Sample record to user-specified tumor/normal status
      //
      String basename = (String) jcb_source.getSelectedItem();
      SAMResource sr = basename2sr.get(basename);
      if (sr != null) {
	String tn = (String) jcb_tn.getSelectedItem();
	Sample sample = sr.get_sample();
	if (tn.equals(NORMAL)) {
	  sample.set_normal(true);
	} else if (tn.equals(TUMOR)) {
	  sample.set_tumor(true);
	} else {
	  sample.reset_tumor_normal();
	  // hack
	}
      }
    } else {
      System.err.println("?");  // debug
    }


  }
  // end ItemListener stub



}