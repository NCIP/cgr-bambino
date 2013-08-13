package TCGA;

public class IdentifierMunger {

  private String raw_id;
  private String[] chunks;
  private String delimiter = "-";
  
  public IdentifierMunger (String id) {
    setup(id);
  }

  private void setup (String id) {
    raw_id = id;

    int i;

    String[] tokens = id.split(" ");
    for (i=0; i < tokens.length; i++) {
      if (tokens[i].indexOf("TCGA") >= 0) {
	// "Broad TCGA-02-0333" => "TCGA-02-0333"
	// "TCGA-06-0152 Methylation" => "TCGA-06-0152"
	id = tokens[i];
	break;
      }
    }
    chunks = null;
    //
    // parse ID, whether dash or period delimited
    //
    if (id.indexOf("-") >= 0) {
      chunks = id.split("-");
    } else if (id.indexOf(".") >= 0) {
      chunks = id.split("\\.");
    } else {
      //      System.err.println("ERROR splitting ID string for ID " + id);  // debug
      // don't complain: sometimes "placeholder" IDs used
      // as vertical separators
    }

    if (chunks != null) {
      i = chunks[0].indexOf("TCGA");
      if (i > 0) {
	chunks[0] = chunks[0].substring(i);
      }

      if (chunks.length >= 6) {
	// strip leading zeros from field #6
	// e.g. TCGA-02-0001-01C-01D-00182-01 => TCGA-02-0001-01C-01D-182-01
	while (chunks[5].indexOf("0") == 0) {
	  chunks[5] = chunks[5].substring(1);
	}
      }
    }

    if (false) {
      System.err.println("chunks="+chunks.length);  // debug
      for (i = 0; i < chunks.length; i++) {
	System.err.println("  " + i + ":"+chunks[i]);  // debug
      }
    }

  }

  public String get_aliquot () {
    return build_id(7);
  }

  public String get_analyte () {
    return build_id(5);
  }

  public String get_sample () {
    return build_id(4);
  }

  public String get_patient () {
    return build_id(3);
  }

  private String build_id (int fields) {
    String result = null;
    if (chunks != null && chunks.length >= fields) {
      return Funk.Str.join(delimiter, chunks, fields);
    }
    return result;
  }

  public static void main (String [] argv) {
    IdentifierMunger im = new IdentifierMunger("TCGA.02.0069.01A.01D.00193.01");
    System.err.println(im.get_aliquot());  // debug
    System.err.println(im.get_analyte());  // debug
    System.err.println(im.get_sample());  // debug
    System.err.println(im.get_patient());  // debug
  }

}
