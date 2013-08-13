package TCGA;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import layout.SpringUtilities;

// REPLACE:
// - controls for "ticks": copynumber breakpoints
//   e.g. 1,3,4,5,10,20
// - 2 controls total: 
//    - baseline brightness level
//    - tick settings

public class ContrastControl implements ControlFrameListener,Observer {
  private static String LABEL_HELP = "Help";

  // components:
  private ControlFrame jf;
  private JTabbedPane jtb;
  private JButton jb_help;
  private JCheckBoxMenuItem jcbmi_multi;

  // data:
  private CopyNumberVariationImage cnvi = null;
  private GenomicMeasurement gm;
  private ArrayList<ContrastControlPanel> ccps;
  private HeatmapConfiguration config;

  public ContrastControl(HeatmapConfiguration config) {
    this.config = config;
    this.gm = config.gm;
    setup();
  }

  private void setup() {
    for (ColorScheme cs : gm.get_color_manager().get_all_color_schemes()) {
      cs.addObserver(this);
    }

    ccps = new ArrayList<ContrastControlPanel>();
    jf = new ControlFrame(this);

    //
    //  control panel:
    //
    JPanel jp_buttons = new JPanel();
    jp_buttons.add(jf.generate_ok_button());
    jp_buttons.add(jf.generate_apply_button());
    //    jp_buttons.setLayout(new BoxLayout(jp_buttons, BoxLayout.LINE_AXIS));

    //    jb_guess.setBorder(BorderFactory.createEmptyBorder(100,100,100,100));

    jp_buttons.add(new JLabel("      "));
    // hack!

    jb_help = new JButton(LABEL_HELP);
    jb_help.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    HelpLauncher hl = new HelpLauncher(HelpLauncher.ANCHOR_COLOR_CONTRAST);
	    hl.launch_url();
	  }
      });

    jp_buttons.add(jb_help);

    jp_buttons.add(new JLabel("      "));
    jp_buttons.add(jf.generate_cancel_button());

    //
    //  container layout
    //
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    
    SampleSubsets sample_subsets = gm.get_sample_subsets();

    ContrastControlPanel ccp;
    ColorManager cm = gm.get_color_manager();
    if (cm.is_multicolor_enabled()) {
      // multiple subsets: use tabbed panels
      HashMap<String,ColorScheme> colors = cm.get_subset_colors();

      jtb = new JTabbedPane();

      panel.add(ccp = new ContrastControlPanel(cm.get_global_color_scheme(), null, config));
      ccps.add(ccp);
      ccp.setToolTipText("This tab controls the display (color and contrast) for the entire dataset.");
      jtb.addTab("Combined", ccp);

      for (String subset : sample_subsets.get_subsets_arraylist()) {
	ccp = new ContrastControlPanel(colors.get(subset), subset, config);
	ccp.setToolTipText("This tab controls the display (color and contrast) for the " + subset + " portion of the data.");
	ccps.add(ccp);
	jtb.addTab(subset, ccp);
      }
      panel.add(jtb);

      configure_enabled_tabs();

      //
      // create Tools menu when using multiple subsets:
      //
      JMenuBar mb = new JMenuBar();
      jf.setJMenuBar(mb);
      JMenu m = new JMenu("Tools");
      m.setMnemonic(KeyEvent.VK_T);
      mb.add(m);

      m.add(jcbmi_multi = new JCheckBoxMenuItem("Independent subset controls", cm.is_multicolor_enabled()));
      jcbmi_multi.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    ColorManager cm = gm.get_color_manager();
	    cm.set_multicolor_enabled(jcbmi_multi.getState());
	    configure_enabled_tabs();
	  }
	}
	);

      JMenu sub = new JMenu("Set all colors...");
      m.add(sub);

      ActionListener al_bg = new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    String color = ((AbstractButton) e.getSource()).getText();
	    for (ContrastControlPanel ccp : ccps) {
	      ccp.set_background_color(color);
	    }
	  }
	};

      ActionListener al_up = new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    String color = ((AbstractButton) e.getSource()).getText();
	    for (ContrastControlPanel ccp : ccps) {
	      ccp.set_increase_color(color);
	    }
	  }
	};

      ActionListener al_down = new ActionListener() {
	  public void actionPerformed(ActionEvent e) {
	    String color = ((AbstractButton) e.getSource()).getText();
	    for (ContrastControlPanel ccp : ccps) {
	      ccp.set_decrease_color(color);
	    }
	  }
	};

      JMenu sub2 = new JMenu("Background...");
      sub.add(sub2);
      JMenuItem jmi;
      sub2.add(jmi = new JMenuItem(ColorScheme.LABEL_WHITE));
      jmi.addActionListener(al_bg);
      sub2.add(jmi = new JMenuItem(ColorScheme.LABEL_BLACK));
      jmi.addActionListener(al_bg);

      sub2 = new JMenu("Increase...");
      sub.add(sub2);
      for (String color : ColorScheme.get_color_names()) {
	sub2.add(jmi = new JMenuItem(color));
	jmi.addActionListener(al_up);
      }

      sub2 = new JMenu("Decrease...");
      sub.add(sub2);
      for (String color : ColorScheme.get_color_names()) {
	sub2.add(jmi = new JMenuItem(color));
	jmi.addActionListener(al_down);
      }


    } else {
      //
      // no subsets within data: single control panel
      //
      panel.add(ccp = new ContrastControlPanel(cm.get_global_color_scheme(), null, config));
      ccps.add(ccp);
    }

    panel.add(jp_buttons);

    jf.addComponentListener(new ComponentListener() {
	// begin ComponentListener stubs
	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {
	  update_current_values();
	}
	// end ComponentListener stubs
      });


    jf.setTitle("Color and contrast controls");
    //    jf.add(panel);
    jf.getContentPane().add(new JScrollPane(panel));
    jf.pack();

    //    setTitle("Contrast");
    jf.setVisible(true);
  }

  private void update_current_values() {
    for (ContrastControlPanel ccp : ccps) {
      // in case colors have been changed elsewhere; i.e. "Background" button shortcut
      ccp.set_current_values();
    }
  }

  // begin Observer stub
  public void update (Observable o, Object arg) {
    // ColorModel has changed (background toggle, etc.)
    update_current_values();
  }
  // end Observer stub


  public void setVisible (boolean v) {
    jf.setVisible(v);
  }

  public void setState (int state) {
    jf.setState(state);
  }

  public void apply_changes() {
    // renders the image
    int len = ccps.size();
    for (int i = 0; i < len; i++) {
      ccps.get(i).apply_changes(i == len - 1);
    }
  }

  public static void main (String [] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    try {
      HeatmapConfiguration hc = new HeatmapConfiguration();
      hc.gm = new GenomicMeasurement("80.txt.gz", false);
      ContrastControl cc = new ContrastControl(hc);
    } catch (Exception e) {
      System.err.println("error:"+e);  // debug
    }
  }

  private JPanel get_buffer_titled_panel (JPanel panel, String title) {
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

  private void configure_enabled_tabs() {
    ColorManager cm = gm.get_color_manager();
    ArrayList<String> ss = gm.get_sample_subsets().get_subsets_arraylist();

    int si = jtb.getSelectedIndex();

    boolean first = !cm.is_multicolor_enabled();
    jtb.setEnabledAt(0, first);
    for (int i=1; i <= ss.size(); i++) {
      jtb.setEnabledAt(i, !first);
    }

    // ensure current tab index doesn't point to a disabled tab:
    int move_to = -1;
    if (si == 0) {
      if (first == false) move_to = 1;
    } else {
      if (first == true) move_to = 0;
    }

    if (move_to != -1) {
      jtb.setSelectedIndex(move_to);
      apply_changes();
    }
    
  }


}

