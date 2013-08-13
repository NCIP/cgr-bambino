package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

public class SummaryPanelCNSI extends SummaryPanel implements Observer,MouseListener {

  //  private static int IMAGE_HEIGHT = 75;
  public static int IMAGE_HEIGHT = 75;
  // HACK

  protected CopyNumberSummaryInfo2 cnsi;

  public SummaryPanelCNSI (CopyNumberSummaryInfo2 cnsi, int raw_width) {
    this.cnsi = cnsi;
    cnsi.addObserver(this);
    set_raw_size(new Dimension(raw_width, IMAGE_HEIGHT));
    addMouseListener(this);
  }

  public void update (Observable o, Object arg) {
    repaint();
    // summary info updated: repaint
  }
  
  // begin MouseListener stubs
  public void mousePressed(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {
    //
    //  prompt for threshold value
    //
    Integer[] values = new Integer[Options.MAX_GRADIENTS];
    Integer current = null;
    int threshold = cnsi.get_change_threshold();
    for (int i = 0; i < Options.MAX_GRADIENTS; i++) {
      int v = i + 1;
      values[i] = new Integer(v);
      if (v == threshold) current = values[i];
    }
    if (current == null) current=values[0];

    Integer result;

    if (false) {
      // unfinished: need to set current value
      result = (Integer) SpinnerInputDialog.showSpinnerDialog(
							      Funk.Gr.getFrame(this),
							      values,
							      "Threshold value for inclusion in graph:",
							      "Set graph threshold",
							      JOptionPane.QUESTION_MESSAGE,
							      JOptionPane.OK_CANCEL_OPTION
							      );
    } else {
      result = (Integer) JOptionPane.showInputDialog(
						     Funk.Gr.getFrame(this),
						     "Threshold value for inclusion in graph:",
						     "Set graph threshold",
						     JOptionPane.QUESTION_MESSAGE,
						     null,
						     values,
						     current);
    }

    if (result != null) {
      cnsi.set_change_threshold(result.intValue());
    }

  };
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

  public String generate_tooltip (Point mp, String label, byte[] array) {
    Point up = get_unscaled_point(mp);
    String result;
    if (up.x >= 0 && up.x < array.length) {
      result = array[up.x] + "% of samples" +
	" at " + cnsi.get_bin_label(up.x) +
	" show " + label + " using threshold " + cnsi.get_change_threshold() + " (click to change)";
    } else {
      result = "Frequency of " + label;
    }
    return result;
  }


}
