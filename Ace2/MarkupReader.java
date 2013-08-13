// simple annotations from flatfile

package Ace2;

import java.net.*;
import java.io.*;
import java.util.*;

public class MarkupReader implements Runnable {
  private BufferedReader br;
  private AceViewer av;
  private AceViewerConfig config = new AceViewerConfig();
  
  public MarkupReader (BufferedReader br, AceViewer av, boolean async) {
    this.br = br;
    this.av = av;
    if (av != null) config = av.get_config();
    config.read2sample = new HashMap<String,Sample>();
    if (async) {
      Thread t = new Thread(this);
      //    t.setPriority(Thread.MIN_PRIORITY);
      t.start();
    } else {
      run();
    }
  }

  public MarkupReader (BufferedReader br, AceViewerConfig config, boolean async) {
    this.br = br;
    this.av = null;
    this.config = config;
    config.read2sample = new HashMap<String,Sample>();
    if (async) {
      Thread t = new Thread(this);
      //    t.setPriority(Thread.MIN_PRIORITY);
      t.start();
    } else {
      run();
    }
  }

  public MarkupReader (String file) throws FileNotFoundException {
    br = new BufferedReader(new FileReader(file));
    run();
  }

  public void run() {
    String line;
    String id = null;
    config.refgenes = new ArrayList<RefGene>();
    config.dbsnp = new ArrayList<dbSNP>();

    try {
      //      System.err.println("quality read start");  // debug
      boolean need_ruler_rebuild = false;
      boolean need_title = false;
      SAMRegion sr = null;
      while (true) {
	line = br.readLine();
	if (line == null) break;
	// EOF
	String[] stuff = line.split("\t");
	if (stuff[0].equals("sample2id")) {
	  Sample sample = Sample.get_sample(stuff[1]);
	  for (int i = 2; i < stuff.length; i++) {
	    config.read2sample.put(new String(stuff[i]), sample);
	  }
	} else if (stuff[0].equals("consensus_label")) {
	  config.CONSENSUS_TAG = new String(stuff[1]);
	} else if (stuff[0].equals("ruler_start")) {
	  config.ruler_start = Integer.parseInt(stuff[1]);
	  need_ruler_rebuild = true;
	} else if (stuff[0].equals("title")) {
	  config.title = stuff[1];
	  need_title = true;
	} else if (stuff[0].equals("refGene")) {
	  config.refgenes.add(new RefGene(stuff));
	} else if (stuff[0].equals("dbsnp")) {
	  config.dbsnp.add(new dbSNP(stuff));
	} else if (stuff[0].toUpperCase().indexOf("SAM_") == 0) {
	  // FUGLY
	  SAMResourceTags v = SAMResourceTags.valueOf(stuff[0].toUpperCase());
	  if (v.equals(SAMResourceTags.SAM_URL)) config.sams.add(new SAMResource());
	  config.sams.get(config.sams.size() - 1).import_data(v, stuff[1]);
	} else if (stuff[0].equals("reference_sequence")) {
	  StringBuffer target_sequence = new StringBuffer();
	  while (true) {
	    line = br.readLine();
	    if (line.equals(">")) {
	      //	      config.target_sequence = target_sequence.toString().toCharArray();
	      config.target_sequence = target_sequence.toString().getBytes();
	      break;
	    } else {
	      target_sequence.append(line);
	    }
	  }
	} else if (stuff[0].equals("target_region")) {
	  sr = new SAMRegion();
	  sr.tname = new String(stuff[1]);
	  sr.range = new Range(Integer.parseInt(stuff[2]), Integer.parseInt(stuff[3]));
	  //	  System.err.println("range " + sr.tname + " " + sr.range.start + " " + sr.range.end);  // debug
	} else {
	  System.err.println("error, don't recognize tag " + stuff[0]);  // debug
	}
      }

      if (config.ruler_start > 0) {
	for (dbSNP snp : config.dbsnp) {
	  snp.consensus_adjust(config.ruler_start);
	}

	for (RefGene rg : config.refgenes) {
	  rg.consensus_adjust(config.ruler_start);
	}
      }

      if (config.dbsnp != null) {
	for (dbSNP snp : config.dbsnp) {
	  //	  System.err.println("adding snp at " + snp.start + " = " + (snp.start + config.ruler_start));  // debug
	}

	config.snp_config.snp_query = new dbSNPSet(config.dbsnp);
      }
      
      for (SAMResource sre : config.sams) {
	sre.set_region(sr);
      }

      if (av != null) {
	while (av.get_acepanel().is_built() == false) {
	  // spin until dependency built
	  try {
	    System.err.println("MarkupReader spin...");  // debug
	    Thread.sleep(50);
	  } catch (Exception e) {}
	}

	PadMap pm = av.get_acepanel().get_assembly().get_padmap();
	for (RefGene rg : config.refgenes) {
	  rg.consensus_setup(pm);
	}

	Runnable later;

	if (need_title) {
	  //	  System.err.println("title="+config.title);  // debug
	  later = new Runnable() {
	    public void run() {
	      av.setTitle(config.title);
	      av.repaint();
	    }
	    };
	  javax.swing.SwingUtilities.invokeLater(later);
	}

	if (need_ruler_rebuild) {
	  //
	  //  ruler labeling has changed.
	  //
	  later = new Runnable() {
	      public void run() {
		//		av.get_acepanel().get_canvas().build_ruler();

		if (config.start_unpadded_offset != 0) {
		  AcePanel ap = av.get_acepanel();
		  PadMap pm = ap.get_assembly().get_padmap();
		  int upo = (config.start_unpadded_offset - config.ruler_start) + 1;
		  // +1: convert to 1-based offset
		  int po = pm.get_unpadded_to_padded(upo);
		  System.err.println("upo=" + upo + " po="+po);  // debug
		  SNPList sl = new SNPList();
		  sl.addElement(new SNP(po, 0.0));
		  ap.ace.set_snps(sl);
		  ap.get_canvas().center_on(po);
		  ap.get_canvas().repaint();
		}
	      }
	    };
	  javax.swing.SwingUtilities.invokeLater(later);
	}

	if (config.enable_exon_navigation) {
	  if (false) {
	    System.err.println("DEBUG: exon nav disabled");
	  } else {
	    av.get_acepanel().init_exon_navigation();
	  }
	}

	av.get_acepanel().get_assembly().build_summary_info();
	// group samples by tumor/normal, if applicable
	//      av.repaint();

	later = new Runnable() {
	    public void run() {
	      av.repaint();
	    }
	  };
	javax.swing.SwingUtilities.invokeLater(later);
      }

      //      System.err.println("quality read end");  // debug
    } catch (Exception e) {
      e.printStackTrace();  // debug
    }

  }

  public static void main (String[] argv) {
    try {
      URL url = new URL(argv[0]);
      BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
      MarkupReader mr = new MarkupReader(br, new AceViewerConfig(), false);
    } catch (Exception e) {
      System.err.println(e);  // debug
    }
  }

  public AceViewerConfig get_config() {
    return config;
  }



}
