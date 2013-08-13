package TCGA;
// create a JDialog using a JSpinner widget
//
// FIX ME: editability

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
 
public class SpinnerInputDialog implements ChangeListener {
  private JSpinner js;
  private JOptionPane jop;
  private String title = null;

  public SpinnerInputDialog (
			     Object[] values,
			     Object message,
			     String title,
			     int messageType,
			     int optionType
			     ) {
    SpinnerListModel slm = new SpinnerListModel(values);
    this.title = title;
    js = new JSpinner(slm);
    js.addChangeListener(this);
    Object msg = null;
    if (message == null) {
      msg = js;
    } else {
      JPanel jp = new JPanel();

      jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
      // meh: how do we left-justify the generated label (below)??
      //      jp.setLayout(new SpringLayout());

      if (message instanceof Component) {
	jp.add((Component) message);
      } else {
	JPanel p = new JPanel();
	p.setLayout(new BorderLayout());
	JLabel jl = new JLabel(message.toString(), JLabel.LEADING);
	p.add(jl, "West");
	jp.add(p);
	//	jl.setAlignmentX(Component.LEFT_ALIGNMENT);
	//	jl.setLabelFor(js);
	jp.add(p);
      }
      jp.add(js);
      msg = jp;
    }

    jop = new JOptionPane(msg,
			  messageType,
			  optionType
			  );
    jop.setInputValue(values[0]);
    // hack
  }

  public Object show_dialog(Component c) {
    JDialog jd = jop.createDialog(c,title);
    jd.setVisible(true);
    return getInputValue();
  }

  public static Object showSpinnerDialog(Component parentComponent,
					 Object[] values,
					 Object message,
					 String title,
					 int messageType,
					 int optionType) {
    SpinnerInputDialog sid = new SpinnerInputDialog(values,message,title,messageType,optionType);
    sid.show_dialog(parentComponent);
    return sid.getInputValue();
  }

  // begin changeListener stub
  public void stateChanged(ChangeEvent e) {
    //    System.err.println("CHANGE");  // debug
    jop.setInputValue(js.getValue());
  }
  // end changeListener stub

  public Object getInputValue() {
    //    System.err.println("value:" + jop.getValue() + " inputValue:" + jop.getInputValue());  // debug

    Object v = jop.getValue();
    Object result;

    if (v == null ||
	v.equals(JOptionPane.UNINITIALIZED_VALUE) ||
	v.equals(JOptionPane.CANCEL_OPTION)) {
      // user closed window/cancelled/didn't make a selection
      result = null;
    } else {
      result = jop.getInputValue();
    }
    return result;
  }

  
  public static void main (String[] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    JFrame jf = new JFrame();

    Integer[] things = {1,2,3,4,5};

    Object result = SpinnerInputDialog.showSpinnerDialog(
							 jf,
							 things,
							 "prompt for something:",
							 "title",
							 JOptionPane.QUESTION_MESSAGE,
							 JOptionPane.OK_CANCEL_OPTION
							 );

    System.err.println(result);  // debug

    
//     SpinnerInputDialog sid = new SpinnerInputDialog(
// 						    things,
// 						    JOptionPane.QUESTION_MESSAGE,
// 						    JOptionPane.OK_CANCEL_OPTION
// 						    );
//     sid.show_dialog(jf);
//    System.err.println(sid.getInputValue());  // debug

    jf.pack();
    jf.setVisible(true);
  }

}

