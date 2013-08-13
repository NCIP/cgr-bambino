// unfinished...
//
// the idea was to have a widget that would black out the whole component
// until some loading process is complete

package Funk;

import java.awt.*;

public class WaitPanel extends Panel {
  private boolean loaded = false;
  private Component comp;

  public WaitPanel (Component c) {
    comp = c;
  }
  
  public void paint (Graphics g) {
    System.out.println("paint!");  // debug

    if (!loaded) {
      Funk.Gr.centerText(comp, "Loading...");
    } else {
      super.paint(g);
    }
  }

  public void loaded () {
    // indicate loaded
    loaded = true;
    repaint();
  }


}
