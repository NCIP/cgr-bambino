package TCGA;

import java.util.*;
import javax.swing.*;
import java.awt.*;

public class AnnotationDisplayControl extends Observable implements ControlFrameListener {
  private ControlFrame jf;
  private CheckBoxSet cbs;

  public AnnotationDisplayControl (ArrayList<String> columns) {
    jf = new ControlFrame(this);
    cbs = new CheckBoxSet(columns, true);

    JPanel sub_p = get_titled_panel("Choose clinical annotations to display:");
    sub_p.setLayout(new BoxLayout(sub_p, BoxLayout.PAGE_AXIS));

    for (String col : columns) {
      sub_p.add(cbs.get(col));
    }
    
    jf.setTitle("Clinical annotation display");
    jf.setLayout(new BorderLayout());
    jf.add("Center", new JScrollPane(buffer_panel(sub_p)));
    jf.add("South", jf.generate_panel(jf.PANEL_OK_APPLY_CANCEL));
    jf.pack();
    jf.setVisible(true);
  }

  public void setVisible (boolean v) {
    jf.setVisible(v);
  }
  
  public static void main(String[] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    try {
      AnnotationFlatfile2 af = new AnnotationFlatfile2("updated_clinical_data.tab", false);
      //      new AnnotationDisplayControl(af, null);
    } catch (Exception e) {
      System.err.println("error:"+e);  // debug
    }
  }

  private JPanel get_titled_panel (String title) {
    // create a new line-bordered JPanel
    JPanel jp_titled = new JPanel();
    jp_titled.setBorder(BorderFactory.createTitledBorder(title));
    return jp_titled;
  }

  private JPanel buffer_panel (JPanel jp) {
    // create a new JPanel adding a buffer to the given JPanel
    JPanel jp_buffer = new JPanel();
    jp_buffer.setLayout(new BorderLayout());
    jp_buffer.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    jp_buffer.add("Center", jp);
    return jp_buffer;
  }

  public void apply_changes() {
    ArrayList<String> selected = cbs.get_selected();
    setChanged();
    notifyObservers(selected);
  }


}
