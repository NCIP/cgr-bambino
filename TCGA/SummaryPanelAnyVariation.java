package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

public class SummaryPanelAnyVariation extends SummaryPanelCNSI {

  private Color COLOR = Color.black;
  private Color COLOR_BACKGROUND = Color.white;

  private static final String LABEL_HELP = "Help: variation panel";
  
  public SummaryPanelAnyVariation (CopyNumberSummaryInfo2 cnsi, int raw_width) {
    super(cnsi,raw_width);
    setToolTipText("Frequency of any change");

    JPopupMenu jpm = new JPopupMenu();
    jpm.add(HelpLauncher.generate_jmenuitem(LABEL_HELP, HelpLauncher.ANCHOR_SUMMARY_ANY));
    new PopupListener(this, jpm);

  }

  protected void paintComponent(Graphics g) {
    Dimension d = getSize();

    g.setColor(COLOR_BACKGROUND);
    g.fillRect(0,0,d.width,d.height);

    float scale_by = (float) d.height / 100;

    ArrayList<Point> any = digest_array(cnsi.percent_showing_any, cnsi.max_percent_showing_any, scale_by, Options.NORMALIZE_SUMMARY_PANEL_PEAKS);
    g.setColor(COLOR);
    render_array(g, any, d.height, false);

  }

  public String getToolTipText(MouseEvent e) {
    String upper_label = Options.COMMENT_OPTIONS.get("high_label");
    String lower_label = Options.COMMENT_OPTIONS.get("low_label");
    String label = "";
    if (upper_label != null && lower_label != null) {
      label = upper_label + " or " + lower_label;
    } else {
      label = "any change";
    }
    return generate_tooltip(e.getPoint(), label, cnsi.percent_showing_any);
  }


}
