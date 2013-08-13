package Ace2;

import java.util.*;

public class Alignment extends Assembly {
  public Ace ace;

  //  Hashtable alignment = new Hashtable();
  HashMap<String,AlignmentSequence> alignment = new HashMap<String,AlignmentSequence>();

  Vector forward_ids, reverse_ids, forward_ids_alpha, reverse_ids_alpha;
  Vector forward_ids_grouped, reverse_ids_grouped, forward_ids_grouped_alpha, reverse_ids_grouped_alpha;
  boolean built;

  private ArrayList<AssemblySequence> all_visible;
  private int last_cs, last_ce;
  private boolean last_bypos;
  private HashMap<String, ArrayList<Integer>> quals = null;
  private FASTAQualityReader fq;
  private AceViewerConfig config = new AceViewerConfig();
  // unless overridden

  public Alignment(Ace a, AceViewerConfig config) {
    this.config = config;
    ace = a;
  }

  public Alignment(Ace a) {
    ace = a;
  }

  public void set_contig (String contig_id) {
    built=false;
    ace.set_current_contig(contig_id);
    build_alignment();
    // HACK
  }

  public void build_alignment () {
    // assumes current contig of .ace object has been already
    // been set to desired contig
    super.build_alignment();
    // build generic summary info

    built = false;

    alignment.clear();

    // set up consensus
    AceSequence cseq = ace.consensus();
    alignment.put(cseq.name, new AlignmentSequence(cseq));

    //    pm = new PadMap(ace.current_contig.consensus.sequence);
    // FIX ME: might be required for multi-contig .ace files??

    int i;
    String [] ids = ace.member_id_list;
    for (i=0; i < ids.length; i++) {
      build(ace.get_member(ids[i]));
      //      AlignmentSequence m = new AlignmentSequence(ace.get_member(ids[i]), this);
      //      alignment.put(ids[i], m);
    }

    build_summary_info();
    //    System.err.println("alignment built");  // debug
    built = true;
  }

  private void build (AceSequence am) {
    // given an AceSequence, build an AlignmentSequence for it
    AlignmentSequence as = new AlignmentSequence(am);
    if (quals != null) {
      ArrayList<Integer> q = quals.get(am.name);
      if (q != null) {
	// (FASTA) quality data available
	as.map_quality(q);
      }
    }
    alignment.put(am.name, as);
  }

  public void build_summary_info () {
    // summary assembly information for the sequences currently
    // in the alignment.
    // FIX ME: maybe just create one version, based on combination of settings currently in effect
    forward_ids = new Vector();
    reverse_ids = new Vector();
    boolean group_mode = config.read2sample != null;

    Vector group_n_f = new Vector();
    Vector group_n_r = new Vector();
    Vector group_t_f = new Vector();
    Vector group_t_r = new Vector();

    for (int i = 0; i < ace.member_id_list.length; i++) {
      String id = ace.member_id_list[i];
      AceSequence s = ace.get_member(id);
      if (s.complemented) {
	reverse_ids.addElement(id);
      } else {
	forward_ids.addElement(id);
      }

      if (group_mode) {
	Sample sa = config.read2sample.get(id);
	if (sa == null || (!(sa.is_normal() || sa.is_tumor()))) {
	  // no data or problem
	  System.err.println("error: can't determine tumor/normal for " + id);  // debug
	  if (s.complemented) {
	    group_n_r.addElement(id);
	  } else {
	    group_n_f.addElement(id);
	  }
	} else if (sa.is_normal()) {
	  if (s.complemented) {
	    group_n_r.addElement(id);
	  } else {
	    group_n_f.addElement(id);
	  }
	} else if (sa != null && sa.is_tumor()) {
	  if (s.complemented) {
	    group_t_r.addElement(id);
	  } else {
	    group_t_f.addElement(id);
	  }
	}
      }
    }

    forward_ids_alpha = new Vector(forward_ids);
    reverse_ids_alpha = new Vector(reverse_ids);
    Collections.sort(forward_ids_alpha);
    Collections.sort(reverse_ids_alpha);

    if (group_mode) {
      forward_ids_grouped = new Vector(group_n_f);
      forward_ids_grouped.addAll(group_t_f);
      reverse_ids_grouped = new Vector(group_n_r);
      reverse_ids_grouped.addAll(group_t_r);

      forward_ids_grouped_alpha = new Vector(forward_ids_grouped);
      reverse_ids_grouped_alpha = new Vector(reverse_ids_grouped);
      Collections.sort(forward_ids_grouped_alpha);
      Collections.sort(reverse_ids_grouped_alpha);
    }

  }

  public AlignmentSequence get_sequence (String id) {
    // return AlignmentSequence for a specified sequence ID
    return((AlignmentSequence) alignment.get(id));
  }

  public ArrayList<AssemblySequence> get_sequences () {
    return new ArrayList<AssemblySequence>(alignment.values());
  }

  public char[] get_consensus_sequence() {
    return ace.consensus().sequence.toCharArray();
    // HACK
  }

  public boolean alignment_is_built() {
    return built;
  }


  AlignmentSequence get_consensus () {
    // return AlignmentSequence for the consensus.
    return((AlignmentSequence) alignment.get(ace.consensus().name));
  }

  public ArrayList<AssemblySequence> get_visible_sequences (int contig_start_view,
							    int contig_end_view) {
    if (contig_start_view != last_cs ||
	contig_end_view != last_ce ||
	display_sequences_by_position != last_bypos) {
      // if cache invalid
      all_visible = new ArrayList<AssemblySequence>();
      boolean group_mode = config.read2sample != null;
      Vector f_set, r_set;

      if (group_mode && forward_ids_grouped != null) {
	// possible race condition

	f_set = display_sequences_by_position ? forward_ids_grouped : forward_ids_grouped_alpha;
	r_set = display_sequences_by_position ? reverse_ids_grouped : reverse_ids_grouped_alpha;
      } else {
	f_set = display_sequences_by_position ? forward_ids : forward_ids_alpha;
	r_set = display_sequences_by_position ? reverse_ids : reverse_ids_alpha;
      }

      populate_visible(all_visible,
		       f_set,
		       contig_start_view, contig_end_view);

      populate_visible(all_visible,
		       r_set,
		       contig_start_view, contig_end_view);

      last_cs = contig_start_view;
      last_ce = contig_end_view;
      last_bypos = display_sequences_by_position;
    }
    return all_visible;
  }

  private void populate_visible (ArrayList<AssemblySequence> v, Vector src, int contig_start_view, int contig_end_view) {
    // given Enumeration of sequence ids, add to Vector those ids
    // that are visible in the current consensus view.
    Enumeration e = src.elements();
    AlignmentSequence as;
    String id;
    while (e.hasMoreElements()) {
      id = (String) e.nextElement();
      as = get_sequence(id);

      if (!(as.get_asm_start() >= contig_end_view ||
	    as.get_asm_end() <= contig_start_view)) {
	// sequence is visible unless it starts after end of visible area,
	// or ends before visible area.
	v.add(as);
      }
    }
  }

  //  public void set_quality(HashMap<String, ArrayList<Integer>> quals) {
  public void set_quality(FASTAQualityReader fq) {
    this.fq = fq;
    HashMap<String, ArrayList<Integer>> quals = fq.get_quality();
    this.quals = quals;
    // Quality information has now been loaded, patch it into the alignment.
    // Only call this AFTER the initial alignment has been built.
    for (String id : alignment.keySet()) {
      AlignmentSequence as = (AlignmentSequence) alignment.get(id);
      ArrayList<Integer> q = quals.get(id);
      if (q != null) as.map_quality(q);
    }
  }

  public boolean has_quality() {
    return quals != null;
  }

  public String get_biggest_contig_id() {
    return ace.get_biggest_contig();
  }

  public String get_contig_id() {
    return ace.get_contig_id();
  }

  public ArrayList<String> get_contig_id_list() {
    return new ArrayList<String>(ace.contigs.keySet());
    // FIX ME: not sorted!!!
  }

  public boolean supports_contigs() {
    return true;
  }

  public boolean has_error() {
    return ace.error();
  }

  public String get_error_message() {
    return "fix me!";
  }

  public boolean is_empty() {
    return ace.is_empty();
  }

  public Sample get_sample_for (String id) {
    return config.read2sample == null ? null : config.read2sample.get(id);
  }

  public boolean is_loaded() {
    return ace.loaded();
  }

  public String get_title() {
    return ace.filename;
    // bleh
  }

  public void addObserver (Observer o) {
    System.err.println("addObserver not implemented");  // debug
  }


}

