// parse sequence values from FASTA-format file

package Ace2;

import java.net.*;
import java.io.*;
import java.util.*;

public class FASTASequenceReader implements Runnable {
  private BufferedReader br;
  private boolean data_loaded = false;
  private HashMap<String, StringBuffer> seqs;
  private AceViewer av;
  
  public FASTASequenceReader (BufferedReader br, boolean async) {
    this.br = br;
    setup(async);
  }

  private void setup (boolean async) {
    if (async) {
      Thread t = new Thread(this);
      //    t.setPriority(Thread.MIN_PRIORITY);
      t.start();
    } else {
      // synchronous
      run();
    }
  }

  public void run() {
    String line;
    String id = null;
    seqs = new HashMap<String, StringBuffer>();
    try {
      //      System.err.println("quality read start");  // debug
      while (true) {
	line = br.readLine();
	// FIX ME: update to use reader...
	if (line == null) break;
	// EOF
	if (line.indexOf(">") == 0) {
	  String[] stuff = line.substring(1).split("\\s+");
	  id = new String(stuff[0]);
	  // HACK, WRONG if line contains comments...
	} else {
	  // sequence
	  StringBuffer sb = seqs.get(id);
	  if (sb == null) seqs.put(id, sb = new StringBuffer());
	  sb.append(line);
	}
      }
      data_loaded = true;
      //      System.err.println("quality read end");  // debug
    } catch (Exception e) {
      System.err.println("FASTASequenceReader load error: " + e);  // debug
      System.exit(1);
    }

  }

  public static void main (String[] argv) {
    try {
      URL url = new URL(argv[0]);
      BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
      FASTASequenceReader fa = new FASTASequenceReader(br, false);
      StringBuffer sb = fa.get_sequences().get("chr7");
      //      StringBuffer sb = fa.get_sequences().get("302P7AAXX090507:5:73:1207:509#0.F1");
      System.err.println(sb);  // debug
    } catch (Exception e) {
      System.err.println(e);  // debug
    }
  }

  public HashMap<String,StringBuffer> get_sequences() {
    return seqs;
  }


}
