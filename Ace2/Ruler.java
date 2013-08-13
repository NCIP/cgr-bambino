package Ace2;

import java.util.Arrays;
import static Ace2.Constants.ALIGNMENT_GAP_CHAR;

public class Ruler {
  private AceViewerConfig config;

  public Ruler (AceViewerConfig config) {
    this.config = config;
  }

  public String get_ruler_for (int csv, int len, PadMap pm) {
    char[] consensus_padded = config.assembly.get_consensus_sequence();

    if (config.COUNTER_UNPADDED == false) {
      System.err.println("ERROR: padded ruler sequence not yet supported");  // debug
    }

    int prebuf_size = 20;
    int postbuf_size = 20;
    if (csv > prebuf_size) {
      csv -= prebuf_size;
    } else {
      //      System.err.println("TEST ME: ruler start <= prebuf_size");  // debug
      prebuf_size = 0;
    }

    int ruler_label_num = pm.get_padded_to_unpadded(csv) + config.ruler_start;
    int mod = 10;
    int dot_spacing = 5;
    int lsize = Integer.toString(ruler_label_num).length();
    if (lsize >= 7) {
      mod = 20;
      dot_spacing = 10;
    }

    char[] buf = new char[len + prebuf_size + postbuf_size];
    Arrays.fill(buf, ' ');

    //    System.err.println("csv label start="+ruler_label_num);  // debug

    //    int end = prebuf_size + len;
    int end = prebuf_size + len + postbuf_size;
    int ci = csv;

    int ci_max = pm.get_padded_sequence().length;

    for (int i = 0; i < end; i++, ci++) {
      //      System.err.println("req CI for " + ci + ", ci_len=" + ci_max);  // debug
      if (ci > ci_max) {
	buf[i] = ' ';
      } else {
	ruler_label_num = pm.get_padded_to_unpadded(ci) + config.ruler_start;
	// FIX ME: right boundary??
	if (ruler_label_num % mod == 0) {
	  if (ruler_label_num == 0) {
	    // don't draw 0
	  } else {
	    //	System.err.println("draw label at " + ruler_label_num);  // debug
	    String label = Integer.toString(ruler_label_num);
	    int si = i - (label.length() / 2);
	    char[] la = label.toCharArray();
	    //	  buf[i] = 'X';
	    //	System.err.println("si="+si + label);  // debug

	    if (si >= 0 && (si + la.length) < buf.length) {
	      System.arraycopy(la, 0, buf, si, la.length);
	    }
	  }
	} else if (ruler_label_num == 1) {
	  buf[i] = '1';
	} else if (ruler_label_num % dot_spacing == 0) {
	  buf[i] = '.';
	}
      }
    }

    return new String(buf, prebuf_size, len);
  }


}