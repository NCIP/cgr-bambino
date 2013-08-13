package TCGA;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import layout.SpringUtilities;

public class ContrastControlPanel extends JPanel implements ActionListener {
  private static final int BORDER_SIZE = 10;
  private static String LABEL_OPTIMIZE = "Optimize";

  private ColorScheme cs;
  private GenomicMeasurement gm;
  private JSlider[] sliders;
  private JSlider js_min_brightness_level;
  private JComboBox jc_schemes,jc_background,jc_increase,jc_decrease;
  private JButton jb_guess;
  private String subset;
  private HeatmapConfiguration config;

  public ContrastControlPanel (ColorScheme cs, String subset, HeatmapConfiguration config) {
    this.config = config;
    this.cs = cs;
    this.gm = config.gm;
    this.subset = subset;
    setup();
  }

  private void setup() {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    //    String gradients_label = Options.DATA_TYPE + " brightness gradients:";
    String gradients_label = "Brightness gradients:";
    // unless we can describe the subsets beyond what's visible in the tab

    sliders = new JSlider[Options.SLIDER_COUNT];

    int i;
    int[] gradients = cs == null ? Options.DEFAULT_COPYNUMBER_GRADIENTS : cs.get_gradients();

    ChangeListener cl = new ChangeListener() {
	public void stateChanged(ChangeEvent e) {
	  calculate_minimum_intensity();
	}
      };

    for (i = 0; i < Options.SLIDER_COUNT; i++) {
      int value = i < gradients.length ? gradients[i] : 0;
      if (value > Options.MAX_GRADIENTS) value = Options.MAX_GRADIENTS;
      JSlider js = new JSlider(0, Options.MAX_GRADIENTS, value);
      int minor = (int) (Options.MAX_GRADIENTS / 20);
      if (minor < 1) minor = 1;
      int major = (int) (Options.MAX_GRADIENTS / 4);
      js.setMinorTickSpacing(minor);
      js.setMajorTickSpacing(major);
      js.setSnapToTicks(true);
      js.setPaintTicks(true);
      js.setPaintLabels(true);
      js.setBorder(BorderFactory.createEmptyBorder(0,0,BORDER_SIZE,0));
      js.addChangeListener(cl);
      sliders[i] = js;
    }

    js_min_brightness_level = new JSlider(0,100, cs == null ? 50 : cs.get_minimum_intensity_percent());

    //    js_min_brightness_level.setMinorTickSpacing(5);
    //    js_min_brightness_level.setMajorTickSpacing(25);
    js_min_brightness_level.setMinorTickSpacing(Options.BRIGHTNESS_MINOR_TICK);
    js_min_brightness_level.setMajorTickSpacing(20);
    js_min_brightness_level.setSnapToTicks(true);
    js_min_brightness_level.setPaintTicks(true);
    js_min_brightness_level.setPaintLabels(true);
    js_min_brightness_level.setBorder(BorderFactory.createEmptyBorder(0,0,BORDER_SIZE,0));

    JPanel jp;

    jp = get_buffer_titled_panel(this, "Colors");
    // jp.setLayout(new BorderLayout());
    //    jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
    jp.setLayout(new SpringLayout());

    Vector schemes = new Vector();
    schemes.add(ColorScheme.COLOR_SCHEME_RED_GREEN);
    schemes.add(ColorScheme.COLOR_SCHEME_RED_BLUE);
    schemes.add(ColorScheme.COLOR_SCHEME_MAGENTA_CYAN);
    schemes.add(ColorScheme.COLOR_SCHEME_RED_CYAN);
    schemes.add(ColorScheme.COLOR_SCHEME_GREEN_MAGENTA);
    schemes.add(ColorScheme.COLOR_SCHEME_BLUE_YELLOW);
    schemes.add(ColorScheme.COLOR_SCHEME_GREEN_YELLOW);
    schemes.add(ColorScheme.COLOR_SCHEME_ORANGE_WHITE);
    schemes.add(ColorScheme.COLOR_SCHEME_YELLOW_WHITE);

    jc_schemes = new JComboBox(schemes);
    jc_schemes.setSelectedItem(Options.DEFAULT_COLOR_SCHEME);
    jc_schemes.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
	  set_colors_from_current_scheme();
	}
      });
    
    String[] default_colors = Options.DEFAULT_COLOR_SCHEME.split("/");
    
    Vector bg_colors = new Vector();
    bg_colors.add(ColorScheme.LABEL_WHITE);
    bg_colors.add(ColorScheme.LABEL_BLACK);
    jc_background = new JComboBox(bg_colors);

    set_current_values();

    Vector color_names = new Vector(ColorScheme.get_color_names());

    //    jp.add("Center", jc_schemes);
    jp.add(new JLabel("Increase:", JLabel.TRAILING));
    jp.add(jc_increase = new JComboBox(color_names));

    jp.add(new JLabel("Decrease:", JLabel.TRAILING));
    jp.add(jc_decrease = new JComboBox(color_names));

    jp.add(new JLabel("Background:", JLabel.TRAILING));
    jp.add(jc_background);

    jp.add(new JLabel(" "));
    jp.add(new JLabel(" "));

    jp.add(new JLabel("Load preset:", JLabel.TRAILING));
    jp.add(jc_schemes);

    set_colors_from_current_scheme();

    SpringUtilities.makeCompactGrid(jp,
				    5, 2,
				    // rows, columns
				    
				    6,6,
				    6,6);


    jp = get_buffer_titled_panel(this, "Baseline brightness level (%)");
    jp.add(js_min_brightness_level);

    jp = get_buffer_titled_panel(this, gradients_label);
    for (i=0; i < sliders.length; i++) {
      jp.add(sliders[i]);
    }

    JPanel jp_button = new JPanel();

    jb_guess = new JButton(LABEL_OPTIMIZE);
    jb_guess.addActionListener(this);
    jp_button.add(jb_guess);
    jp.add(jp_button);

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

  public void set_current_values() {
    String color = cs.get_colorscheme_model().white_mode ? ColorScheme.LABEL_WHITE : ColorScheme.LABEL_BLACK;
    jc_background.setSelectedItem(color);
  }

  private void set_colors_from_current_scheme() {
    String name = (String) jc_schemes.getSelectedItem();
    String[] colors = name.split("/");
    jc_increase.setSelectedItem(colors[0]);
    jc_decrease.setSelectedItem(colors[1]);
  }

  public void apply_changes() {
    // inherited
    apply_changes(true);
  }

  public void apply_changes(boolean notify) {
    ArrayList<Integer> gradients = new ArrayList<Integer>();
    for (int i=0; i < sliders.length; i++) {
      int v = sliders[i].getValue();
      if (v > 0) gradients.add(new Integer(v));
    }
    Collections.sort(gradients);
    int[] g2 = new int[gradients.size()];
    for (int i=0; i < gradients.size(); i++) {
      g2[i] = gradients.get(i).intValue();
    }

    if (cs != null) {
      ColorSchemeModel csm = cs.get_colorscheme_model();
      csm.up_color = ColorScheme.get_color((String) jc_increase.getSelectedItem());
      csm.down_color = ColorScheme.get_color((String) jc_decrease.getSelectedItem());
      csm.min_intensity_percent = js_min_brightness_level.getValue();
      csm.gradients = g2;
      String bg = (String) jc_background.getSelectedItem();
      csm.white_mode = bg.equals(ColorScheme.LABEL_WHITE);

      cs.set_colorscheme_model(csm, notify);
    }
  }

  public void actionPerformed(ActionEvent e) {
    //
    // optimize
    //
    SampleSummaryInfo ssi = gm.get_sample_summary_info();
    ColorSchemeModel gr = ssi.calculate_gradients(subset);
    
    js_min_brightness_level.setValue(gr.min_intensity_percent);
    for (int i=0; i < sliders.length; i++) {
      sliders[i].setValue(i < gr.gradients.length ? gr.gradients[i] : 0);
    }
    apply_changes();
  }

  public void set_background_color (String color) {
    jc_background.setSelectedItem(color);
  }

  public void set_increase_color (String color) {
    jc_increase.setSelectedItem(color);
  }

  public void set_decrease_color (String color) {
    jc_decrease.setSelectedItem(color);
  }

  public static int calculate_minimum_intensity(int active_gradient_count) {
    int min_int = 75 - (active_gradient_count * 12);
    //
    // snap the default intensity to the minor tick level used by the
    // color/contrast control, so that initial calculation will match what
    // the brightness slider is capable of displaying.  If we don't do this,
    // an initial "apply" of the contrast controls may change the image colors,
    // even though the user hasn't modified any of the controls.
    //
    int mod = min_int % Options.BRIGHTNESS_MINOR_TICK;
    if (mod >= (Options.BRIGHTNESS_MINOR_TICK / 2)) {
      // round up
      min_int += Options.BRIGHTNESS_MINOR_TICK - mod;
    } else {
      // round down
      min_int -= mod;
    }
    return min_int;
  }

  public void calculate_minimum_intensity() {
    int active=0;
    for (int i=0; i < sliders.length; i++) {
      int v = sliders[i].getValue();
      if (v > 0) active++;
    }
    js_min_brightness_level.setValue(calculate_minimum_intensity(active));
  }

}
