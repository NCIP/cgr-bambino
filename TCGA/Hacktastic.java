package TCGA;
import javax.swing.*;
import java.awt.*;
import java.util.*;

public class Hacktastic implements Runnable {
  private Heatmap6 parent;
  private ArrayList<GenomicMeasurement> gm_supplemental;

  public Hacktastic(Heatmap6 parent, ArrayList<GenomicMeasurement> gm_supplemental) {
    this.parent = parent;
    this.gm_supplemental = gm_supplemental;
    if (parent != null) new Thread(this).start();
  }

  public void run() {
    NavigationControl nc = parent.get_nc();
    JFrame jf = nc.get_jframe();

    jf.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    
    while (true) {
      boolean all_loaded = true;
      for (GenomicMeasurement gms : gm_supplemental) {
	//	  System.err.println("loaded: " + gms.is_loaded());  // debug
	if (!gms.is_loaded()) all_loaded = false;
      }
      if (all_loaded) {
	break;
      } else {
	try {
	  Thread.sleep(100);
	} catch (InterruptedException e) {}
      }
    }

    jf.setCursor(Cursor.getDefaultCursor());
  }
}