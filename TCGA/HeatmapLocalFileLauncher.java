package TCGA;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.filechooser.*;
import layout.SpringUtilities;
import java.io.*;

public class HeatmapLocalFileLauncher extends JFrame implements ActionListener,Runnable {
  JFileChooser fc;
  JTextField fn_data, fn_bin, fn_annotations;
  JButton jb_data, jb_bin, jb_annotations, jb_launch;

  public HeatmapLocalFileLauncher() {
    
    setTitle("Heatmap launcher");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    fc = new JFileChooser();

    JPanel jp_main = new JPanel();
    jp_main.setLayout(new BoxLayout(jp_main, BoxLayout.PAGE_AXIS));

    JPanel p = new JPanel(new SpringLayout());

    p.add(new JLabel("Data file:", JLabel.TRAILING));
    p.add(fn_data = new JTextField(80));

    p.add(jb_data = new JButton("Browse..."));

    p.add(new JLabel("Set/bin file (optional):", JLabel.TRAILING));
    p.add(fn_bin = new JTextField(80));
    p.add(jb_bin = new JButton("Browse..."));

    p.add(new JLabel("Annotation file (optional):", JLabel.TRAILING));
    p.add(fn_annotations = new JTextField(80));
    p.add(jb_annotations = new JButton("Browse..."));
    
    jp_main.add(p);

    SpringUtilities.makeCompactGrid(p,
				    3,3,
				    // rows, columns
				    6,6,
				    6,6);
    p = new JPanel();
    p.add(jb_launch = new JButton("Start viewer"));
    jp_main.add(p);

    jb_data.addActionListener(this);
    jb_bin.addActionListener(this);
    jb_annotations.addActionListener(this);
    jb_launch.addActionListener(this);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", jp_main);
    pack();
    setVisible(true);
  }

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src.equals(jb_data) || src.equals(jb_bin) || src.equals(jb_annotations)) {
      int returnVal = fc.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
	File file = fc.getSelectedFile();
	JTextField jtf = null;
	if (src.equals(jb_data)) {
	  jtf = fn_data;
	} else if (src.equals(jb_bin)) {
	  jtf = fn_bin;
	} else if (src.equals(jb_annotations)) {
	  jtf = fn_annotations;
	}
	jtf.setText(file.getAbsolutePath());
      }
    } else if (src.equals(jb_launch)) {
      new Thread(this).start();
    }
  }
  // end ActionListener stub

  public void run() {
    String gm_file = fn_data.getText();

    String bin_file = fn_bin.getText();
    String annotation_file = fn_annotations.getText();

    //    System.err.println("thread start");  // debug
    if (gm_file.length() > 0) {
      try {
	setVisible(false);
	GenomicMeasurement gm = new GenomicMeasurement(gm_file);
	AnnotationFlatfile2 af = (annotation_file.length() == 0) ? null : new AnnotationFlatfile2(annotation_file);
	//	GenomicSet gs = (bin_file.length() == 0) ? 
	//	  new GenomicSet(gm, GenomicSet.STYLE_GENOMIC, null) : new GenomicSet(bin_file);
	GenomicSet gs = bin_file.length() > 0 ? new GenomicSet(bin_file) : null;
	HeatmapConfiguration config = new HeatmapConfiguration(af,gm,gs);
	config.title = Funk.Str.basename(gm_file);
	Heatmap6 hm = new Heatmap6(config);
      } catch (Exception ex) {
	System.err.println("ERROR:"+ex);  // debug
	new ErrorReporter(ex);
      }
    } else {
      JOptionPane.showMessageDialog(this,
				    "Specify a data file to display.",
				    "Error",
				    JOptionPane.ERROR_MESSAGE);	  
    }
  }

  public JFileChooser get_chooser() {
    return fc;
  }

  
}
