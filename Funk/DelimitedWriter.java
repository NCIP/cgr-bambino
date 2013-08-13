package Funk;
// simple delimited line report generator
// mne 4/04

import java.util.*;
import java.io.*;

public class DelimitedWriter {
    private PrintWriter pw;
    private String delimiter;
    private boolean print_headers, header_printed, strict_mode;
    private Vector labels;

    public DelimitedWriter (PrintWriter pw) {
	this.pw = pw;
	setup();
    }

    public DelimitedWriter (PrintStream ps) {
	pw = new PrintWriter(ps);
	setup();
    }

    private void setup () {
	delimiter = "\t";
	header_printed = false;
	strict_mode = true;
	print_headers = true;
    }

    private void header_print_check () {
	if (header_printed == false && labels != null) {
	    if (print_headers) pw.println(Funk.Str.join(delimiter, labels.elements()));
	    header_printed = true;
	}
    }

    public void set_delimiter (String delimiter) {
	this.delimiter = delimiter;
    }

    public void print_headers (boolean status) {
	print_headers = status;
    }

    public void set_labels (Vector v) {
	labels = v;
    }

    public void add_label (String label) {
	if (labels == null) labels = new Vector();
	labels.addElement(label);
    }

    public void write_row (Hashtable ht) {
	header_print_check();
	if (labels == null) {
	    die("DelimitedWriter: can't write hash row without labels");
	} else {
	    Vector r = new Vector();
	    Enumeration e = labels.elements();
	    String label, value;
	    while (e.hasMoreElements()) {
		label = (String) e.nextElement();
		if (ht.containsKey(label)) {
		    value = (String) ht.get(label);
		    if (value == null) value = "";
		    r.addElement(value);
		} else {
		    die("DelimitedWriter: no value for column " + label);
		}
	    }

	    pw.println(Funk.Str.join(delimiter, r.elements()));
	}
    }

    public void write_row (Vector v) {
	header_print_check();
	pw.println(Funk.Str.join(delimiter, v.elements()));
    }

    public static void main (String []argv) {
	// debug
	DelimitedWriter dw = new DelimitedWriter(System.out);

	if (true) {
	    dw.add_label("col1");
	    dw.add_label("col2");
	    dw.add_label("col3");
	} else {
	    Vector labels = new Vector();
	    labels.addElement("col1");
	    labels.addElement("col2");
	    labels.addElement("col3");
	    dw.set_labels(labels);
	}

	Vector row = new Vector();
	row.addElement("one");
	row.addElement("two");
	row.addElement("three");
	dw.write_row(row);

	Hashtable ht = new Hashtable();
	ht.put("col1", "val1");
	ht.put("col2", "val2");
	ht.put("col3", "val3");
	dw.write_row(ht);

	dw.finish();
    }

    public void finish () {
	// cleanup
	pw.flush();
    }

    private void die (String msg) {
	System.err.println(msg);  // debug
	System.exit(1);
    }

}
