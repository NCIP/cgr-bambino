package Ace2;

import java.io.*;
import java.util.*;
import net.sf.samtools.*;

public class PairedReadWriter {
  WorkingFile wf_fwd, wf_rev;
  PrintStream ps_fwd, ps_rev;
  private HashMap<String,SAMRecord> pair_first, pair_second;
  private int FLUSH_CHECK_INTERVAL = 10000;
  int READ_REPORT_INTERVAL = 1000000;
  int read_count;
  HashSet<String> wanted_ids;

  public PairedReadWriter (String basename, HashSet<String> wanted_ids) throws FileNotFoundException,IOException {
  this.wanted_ids = wanted_ids;

    wf_fwd = new WorkingFile(basename + ".fwd.fastq");
    wf_rev = new WorkingFile(basename + ".rev.fastq");
    ps_fwd = wf_fwd.getPrintStream();
    ps_rev = wf_rev.getPrintStream();
    // fix me: abstract/subclass for different output types

    pair_first = new HashMap<String,SAMRecord>();
    pair_second = new HashMap<String,SAMRecord>();
    read_count = 0;
  }

  public void scan(SAMRecordIterator query) throws IOException {
    int read = 0;
    SAMRecord sr;
    while (query.hasNext()) {
      if (++read % READ_REPORT_INTERVAL == 0) {
	System.err.println("read:" + read + " ids remaining=" + wanted_ids.size());
      }
      sr = query.next();
      if (!sr.getReadPairedFlag())
	throw new IOException("paired reads required");
      if (wanted_ids.contains(sr.getReadName())) add(sr);
    }
  }

  public boolean close() {
    boolean ok = flush_check(true);
    wf_fwd.finish();
    wf_rev.finish();
    return ok;
  }

  public void add (SAMRecord sr) {
    if (sr.getFirstOfPairFlag()) {
      pair_first.put(sr.getReadName(), sr);
    } else if (sr.getSecondOfPairFlag()) {
      pair_second.put(sr.getReadName(), sr);
    } else {
      System.err.println("paired read not 1st or 2nd: that's unpossible!!");  // debug
      // ralph wiggum 4 life
    }
    if (++read_count % FLUSH_CHECK_INTERVAL == 0) flush_check(false);
  }

  private boolean flush_check (boolean closing) {
    boolean ok = true;
    HashSet<String> all_ids = new HashSet<String>();
    all_ids.addAll(pair_first.keySet());
    all_ids.addAll(pair_second.keySet());

    ArrayList<String> prunable = new ArrayList<String>();
    for (String id : all_ids) {
      SAMRecord first = pair_first.get(id);
      SAMRecord second = pair_second.get(id);

      if (first != null && second != null) {
	write_pair(first, second);
	prunable.add(id);
      }
    }

    for (String id : prunable) {
      //      System.err.println("pruning " + id);  // debug
      pair_first.remove(id);
      pair_second.remove(id);
      wanted_ids.remove(id);
    }

    if (closing) {
      int leftover = wanted_ids.size();
      System.err.print("finishing: ids remaining=" + leftover);
      if (leftover == 0) {
	System.err.print(" all reads found.");  // debug
      } else {
	System.err.print(" example:" + (new ArrayList<String>(wanted_ids)).get(0));
	ok = false;
      }
      System.err.println("");  // debug
    }
    return ok;
  }


  private void add_basic_tags (SAMRecord sr, ArrayList<String> tags) {
    tags.add(sr.getReadName());

    if (sr.getReadUnmappedFlag()) {
      tags.add("unmapped");
    } else {
      tags.add("mapped");
      tags.add(sr.getReferenceName() + ":" + Integer.toString(sr.getAlignmentStart()) + "-" + Integer.toString(sr.getAlignmentEnd()));
    }
  }

  private void write_pair (SAMRecord sr_f, SAMRecord sr_r) {
    ArrayList<String> tags_f = new ArrayList<String>();
    ArrayList<String> tags_r = new ArrayList<String>();

    add_basic_tags(sr_f, tags_f);
    add_basic_tags(sr_r, tags_r);

    rc_fix(sr_f, tags_f);
    rc_fix(sr_r, tags_r);

    SAMUtils.write_fastq(ps_fwd, sr_f, Funk.Str.join(" ", tags_f));
    SAMUtils.write_fastq(ps_rev, sr_r, Funk.Str.join(" ", tags_r));
  }

  private void rc_fix (SAMRecord sr, ArrayList<String> tags) {
    if (!sr.getReadUnmappedFlag() && sr.getReadNegativeStrandFlag()) {
      // BAM alignment transforms all reads into reference space.
      // Undo the reverse-complementation applied to minus-mapped reads.
      String bases_rc = new String(sr.getReadBases());
      String fixed = Funk.Str.reverse_complement(bases_rc);
      char[] fixed_c = fixed.toCharArray();
      byte[] fixed_b = new byte[fixed_c.length];
      for (int i = 0; i < fixed_c.length; i++) {
	fixed_b[i] = (byte) fixed_c[i];
      }
      sr.setReadBases(fixed_b);

      // since we reverse-complemented the sequence,
      // reverse the base quality scores:
      StringBuilder sb = new StringBuilder(sr.getBaseQualityString());
      sr.setBaseQualityString(sb.reverse().toString());
      tags.add("rc_fixed");
    }
  }




}