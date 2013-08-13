// parse quality values from FASTA-format quality file
// FIX ME: possible reversal (i.e. for reverse-complemented assembly reads)

package Ace2;

import java.net.*;
import java.io.*;
import java.util.*;

public class FASTAQualityReader implements Runnable {
  private BufferedReader br;
  private boolean data_loaded = false;
  private HashMap<String, ArrayList<Integer>> quals;
  private AceViewer av;
  
  public FASTAQualityReader (BufferedReader br, AceViewer av, boolean async) {
    this.br = br;
    this.av = av;
    setup(async);
  }

  public FASTAQualityReader (BufferedReader br, boolean async) {
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
    quals = new HashMap<String, ArrayList<Integer>>();
    try {
      //      System.err.println("quality read start");  // debug
      while (true) {
	line = br.readLine();
	// FIX ME: update to use reader...
	if (line == null) break;
	// EOF
	if (line.indexOf(">") == 0) {
	  id = line.substring(1);
	  // HACK, WRONG if line contains comments...
	} else {
	  // quality scores
	  ArrayList<Integer> list = quals.get(id);
	  if (list == null) quals.put(id, list = new ArrayList<Integer>());
	  String[] q = line.split(" ");
	  int i=0;
	  for(i=0; i < q.length; i++) {
	    list.add(Integer.parseInt(q[i]));
	  }
	}
      }
      data_loaded = true;
      //      System.err.println("quality read end");  // debug

      if (av != null) {
	while (av.get_acepanel().get_assembly().alignment_is_built() == false) {
	  // spin until alignment data completely built
	  try {
	    System.err.println("FASTAQualityReader spin...");  // debug
	    Thread.sleep(50);
	  } catch (Exception e) {}
	}

	if (false) {
	  System.err.println("DEBUG: artificial sleep");  // debug
	  try {Thread.sleep(1000);} catch (Exception e) {}
	}

	av.get_acepanel().get_assembly().set_quality(this);
	av.repaint();
      }

    } catch (Exception e) {
      System.err.println("FASTAQualityReader load error: " + e);  // debug
      e.printStackTrace();
      System.exit(1);
    }

  }

  public static void main (String[] argv) {
    try {
      URL url = new URL(argv[0]);
      BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
      FASTAQualityReader fq = new FASTAQualityReader(br, null, false);
    } catch (Exception e) {
      System.err.println(e);  // debug
    }
  }

  public HashMap<String, ArrayList<Integer>> get_quality() {
    return quals;
  }


}
