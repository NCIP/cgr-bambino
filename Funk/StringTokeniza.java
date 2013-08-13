package Funk;
// some optionally smarter functionality than StringTokenizer,
// allows two kinds of quote protection (" and ' vs just one
// in StreamTokenizer
//
// - optional leading/trailing whitespace stripping
// - optional token unquoting
//
// to add:
//  - escape char for quote mode "quoted" vs. \"quoted\"

// mne 7/04

import java.util.*;
import javax.print.attribute.standard.PageRanges;

public class StringTokeniza {
    private boolean allow_null_tokens = true;
    private boolean quote_mode = false;
    private boolean trim_whitespace = false;
    private boolean unquote_mode = false;

    public void set_quote_mode (boolean mode) {
	// whether to ignore delimiters in quoted regions
	quote_mode = mode;
    }

    public void set_trim_whitespace (boolean mode) {
	// whether to trim leading/trailing whitespace from tokens
	trim_whitespace = mode;
    }

    public void set_unquote_mode (boolean mode) {
	// whether to unquote resulting tokens
	unquote_mode = mode;
    }

    public Enumeration tokenize (String parse_string, String delimiter) {
	int delim_len = delimiter.length();
	int parse_string_length = parse_string.length();

	if (delim_len > 1) {
	    die("delim_len > 1 untested");
	}

	if (allow_null_tokens == false) {
	    // not yet implemented
	    die("allow_null_tokens false, fix me!");
	}

	Vector tokens = new Vector();
	Vector stops = new Vector();

	stops.addElement(new Integer(0));

	PageRanges quoted_regions = null;

	int i;
	if (quote_mode) {
	    // build hash of "false positives"
	    // BETTER IDEA:
	    // build set of X-Y quote ranges, use "union"-type set logic
	    // to test if inside...use PageRanges!
	    boolean single_quote_mode = false;
	    boolean double_quote_mode = false;
	    int single_quote_start = 0;
	    int double_quote_start = 0;
	    Vector quote_ranges = new Vector();
	    char c;
	    for (i=0; i < parse_string_length; i++) {
		c = parse_string.charAt(i);
		if (c == '"') {
		    if (double_quote_mode) {
			quote_ranges.addElement(double_quote_start + "-" + i);
		    } else {
			double_quote_start = i;
		    }
		    double_quote_mode = !double_quote_mode;
		} else if (c == '\'') {
		    if (single_quote_mode) {
			quote_ranges.addElement(single_quote_start + "-" + i);
		    } else {
			single_quote_start = i;
		    }
		    single_quote_mode = !single_quote_mode;
		}
	    }

	    if (quote_ranges.size() > 0) {
		quoted_regions = new PageRanges(Funk.Str.join(quote_ranges.elements()));
	    }
	}

	i = parse_string.indexOf(delimiter);
	while (i > -1) {
	    //	    System.err.println(i);  // debug
	    
	    if (quoted_regions != null && quoted_regions.contains(i)) {
		// delimiter appears in quoted region: ignore
		//		System.out.println("skipping quoted delimim occurence at " + i);  // debug
	    } else {
		stops.addElement(new Integer(i));
	    }

	    i = parse_string.indexOf(delimiter, i + 1);
	}

	stops.addElement(new Integer(parse_string_length));

	int slen = stops.size();
	int hither,yon;

	for (i=0; i < slen - 1; i++) {
	    hither = ((Integer) stops.elementAt(i)).intValue();
	    if (i > 0) hither += delim_len;
	    yon = ((Integer) stops.elementAt(i + 1)).intValue();
	    //	    System.err.println(hither + " -> " + yon);  // debug
	    //	    System.err.println("token: " + parse_string.substring(hither,yon));  // debug

	    if (trim_whitespace) {
		// remove leading/trailing whitespace
		// yeah, yeah, we could use String.trim() but since 
		// we might also have to unquote below...
		while (hither < yon &&
		       Character.isWhitespace(parse_string.charAt(hither))) {
		    hither++;
		}

		while (yon > hither &&
		       Character.isWhitespace(parse_string.charAt(yon - 1))) {
		    yon--;
		}
	    }

	    if (unquote_mode && hither != yon) {
		// if token is quoted, unquote it
		if ((parse_string.charAt(hither) == '"' &&
		     parse_string.charAt(yon - 1) == '"') ||
		    (parse_string.charAt(hither) == '\'' &&
		     parse_string.charAt(yon - 1) == '\'')) {
		    hither++;
		    yon--;
		}
	    }

	    tokens.addElement(parse_string.substring(hither, yon));
	}
	
	return tokens.elements();
    }

    private static void die (String msg) {
	System.err.println(msg);
	System.exit(1);
    }

    public static void main (String argv[]) {
	StringTokeniza st = new StringTokeniza();
	st.set_quote_mode(true);
	st.set_trim_whitespace(true);
	st.set_unquote_mode(true);

	//	Enumeration e = st.tokenize("molid|100590|complex||node|27|AS|Calpain1", "|");
	//	Enumeration e = st.tokenize("label=\"CDKN1A,this,will,break\", width=\"1.14\", shape=plaintext, fontsize=14, URL=\"javascript:spawn(\'MoleculePage?molid=20230\')\", height=\"0.50\", color=black, pos=\"321,166\"", ",");
	// Enumeration e = st.tokenize("label=\"CDKN1A=this=will=break\"", "=");
	//	Enumeration e = st.tokenize("label=\'CDKN1A=this=will=break\'", "=");
	Enumeration e = st.tokenize("key=\"\"", "=");
	while (e.hasMoreElements()) {
	    String token = (String) e.nextElement();
	    System.err.println(token + ":" + token.length());  // debug
	}
    }

}

