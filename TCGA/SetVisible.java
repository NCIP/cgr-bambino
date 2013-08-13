package TCGA;
import java.awt.event.*;

public class SetVisible implements ActionListener {
  private VisibilityToggle vt;

  public SetVisible (VisibilityToggle vt) {
    this.vt = vt;
  }

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    vt.setVisible(true);
  }
  // end ActionListener stub


}

