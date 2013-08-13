package Ace2;

import java.util.*;
import java.util.regex.*;

public class SampleNamingConvention {
  String convention_name;
  // naming convention style/origin: organization, etc.

  String global_pattern;
  // regexp to isolate sample ID and tumor/normal portion of filename
  // in 2 capturing groups

  ArrayList<SampleNamingConventionPattern> type_matches;
  // mappings of tumor/normal capture group strings to
  // tumor/normal status and description

  SampleMatchInfo match_info;

  private static boolean VERBOSE = false;
  private static ArrayList<SampleNamingConvention> known_conventions = null;

  public SampleNamingConvention (String convention_name) {
    this.convention_name = convention_name;
    type_matches = new ArrayList<SampleNamingConventionPattern>();
  }

  public void add_pattern (SampleNamingConventionPattern sncp) {
    type_matches.add(sncp);
  }

  public boolean matches (String s) {
    //
    // does the specified string match this naming convention?
    //
    Pattern sp = Pattern.compile(global_pattern, Pattern.CASE_INSENSITIVE);
    Matcher sm = sp.matcher(s);
    match_info = null;

    if (sm.find()) {
      if (VERBOSE) {
	(new Exception()).printStackTrace();
	System.err.println("checking " + s);  // debug

	System.err.println("global match for " + global_pattern + " => " + sm.group(1) + " " + sm.group(2));
      }

      String type_string = sm.group(2);
      for (SampleNamingConventionPattern sncp : type_matches) {
	if (sncp.type_string.equalsIgnoreCase(type_string)) {
	  match_info = new SampleMatchInfo();
	  match_info.sample_name = sm.group(1);
	  match_info.description = sncp.description;
	  match_info.tumor_normal = sncp.tumor_normal;
	  match_info.is_recurrent = sncp.is_recurrent;
	  break;
	}
      }
    } else if (VERBOSE) {
      System.err.println("no match for " + global_pattern);  // debug
    }

    return match_info != null;
  }

  public SampleMatchInfo get_match_info() {
    return match_info;
  }

  public static ArrayList<SampleNamingConvention> get_known_conventions() {
    //
    //  default set of known naming conventions
    //

    TumorNormal CELL_LINE_TN_HACK = TumorNormal.TUMOR;

    
    if (known_conventions == null) {
      //
      // generate (singleton)
      //
      known_conventions = new ArrayList<SampleNamingConvention>();
      SampleNamingConvention snc;
      SampleNamingConventionPattern sncp;

      //
      // St. Jude naming convention:
      //
      // examples:
      //    SJINF001_D-TB-98-6292.bam
      //    SJINF001_G-TB-99-5066.bam
      //
      // Where SJINF001 is the patient ID and D for Diagnosis (tumor) and G for Germline (normal).
      //
      known_conventions.add(snc = new SampleNamingConvention("st_jude"));
      snc.global_pattern = "^(\\w+)_([DG])\\-";
      snc.add_pattern(new SampleNamingConventionPattern("G", TumorNormal.NORMAL, "germline (normal)"));
      snc.add_pattern(new SampleNamingConventionPattern("D", TumorNormal.TUMOR, "diagnosis (tumor)"));

      //
      // TCGA naming convention:
      //
      // examples:
      //    TCGA-10-0926-01A-01W.bam
      //    TCGA-10-0926-11A-01W.bam
      //
      if (SNPConfig.TCGA_SAMPLE_FIELD_COUNT == 3) {
	// consider the "sample ID" the patient/donor ID (1st 3 fields)
	known_conventions.add(snc = new SampleNamingConvention("tcga_donor"));
	//	snc.global_pattern = "^(TCGA\\-\\d\\d\\-\\d\\d\\d\\d)\\-(\\d\\d)";
	snc.global_pattern = "^(TCGA\\-\\w\\w\\-\\w\\w\\w\\w)\\-(\\w\\w)";
      } else if (SNPConfig.TCGA_SAMPLE_FIELD_COUNT == 4) {
	known_conventions.add(snc = new SampleNamingConvention("tcga_sample"));
	snc.global_pattern = "^(TCGA\\-\\w\\w\\-\\w\\w\\w\\w\\-(\\w\\w))";
      } 

      // see http://tcga-data.nci.nih.gov/datareports/codeTablesReport.htm
      // Sample Type table
      snc.add_pattern(new SampleNamingConventionPattern("01", TumorNormal.TUMOR, "primary solid tumor"));
      snc.add_pattern(new SampleNamingConventionPattern("02", TumorNormal.TUMOR, "recurrent solid tumor"));
      snc.add_pattern(new SampleNamingConventionPattern("03", TumorNormal.TUMOR, "primary blood-derived cancer"));
      snc.add_pattern(new SampleNamingConventionPattern("04", TumorNormal.TUMOR, "recurrent blood-derived cancer"));
      snc.add_pattern(new SampleNamingConventionPattern("05", TumorNormal.TUMOR, "additional - new primary cancer"));
      snc.add_pattern(new SampleNamingConventionPattern("06", TumorNormal.TUMOR, "metastatic"));
      snc.add_pattern(new SampleNamingConventionPattern("07", TumorNormal.TUMOR, "additional metastatic"));

      snc.add_pattern(new SampleNamingConventionPattern("10", TumorNormal.NORMAL, "blood-derived normal"));
      snc.add_pattern(new SampleNamingConventionPattern("11", TumorNormal.NORMAL, "solid tissue normal"));
      snc.add_pattern(new SampleNamingConventionPattern("12", TumorNormal.NORMAL, "buccal cell normal"));
      snc.add_pattern(new SampleNamingConventionPattern("13", TumorNormal.NORMAL, "EBV immortalized normal"));
      snc.add_pattern(new SampleNamingConventionPattern("14", TumorNormal.NORMAL, "bone marrow normal"));

      snc.add_pattern(new SampleNamingConventionPattern("20", CELL_LINE_TN_HACK, "cell line control"));


      //
      // TARGET naming convention:
      //
      // examples:
      //   TARGET-50-PAJMKN-01A-01D_Illumina.bam
      //   TARGET-50-PAJMKN-10A-01D_Illumina.bam
      //
      known_conventions.add(snc = new SampleNamingConvention("target_sample"));
      snc.global_pattern = "^(TARGET\\-\\w\\w\\-\\w{6}\\-(\\w\\w))";
      //snc.global_pattern = "^(TARGET\\-\\w\\w\\-\\w\-(\\w\\w))";

      //
      // see TARGET_Sample_Codes_v5.docx
      //
      snc.add_pattern(new SampleNamingConventionPattern("01", TumorNormal.TUMOR, "primary tumor"));
      snc.add_pattern(new SampleNamingConventionPattern("02", TumorNormal.TUMOR, "recurrent tumor"));
      snc.add_pattern(new SampleNamingConventionPattern("03", TumorNormal.TUMOR, "primary blood cancer"));
      snc.add_pattern(new SampleNamingConventionPattern("04", TumorNormal.TUMOR, "recurrent blood cancer"));
      snc.add_pattern(new SampleNamingConventionPattern("05", TumorNormal.TUMOR, "additional new primary cancer"));
      snc.add_pattern(new SampleNamingConventionPattern("06", TumorNormal.TUMOR, "metastatic cancer"));
      snc.add_pattern(new SampleNamingConventionPattern("07", TumorNormal.TUMOR, "additional metastatic cancer"));
      snc.add_pattern(new SampleNamingConventionPattern("08", TumorNormal.TUMOR, "human tumor additional cells"));
      snc.add_pattern(new SampleNamingConventionPattern("09", TumorNormal.TUMOR, "primary blood cancer: bone marrow"));

      snc.add_pattern(new SampleNamingConventionPattern("10", TumorNormal.NORMAL, "blood derived normal"));
      snc.add_pattern(new SampleNamingConventionPattern("11", TumorNormal.NORMAL, "solid tissue normal"));
      snc.add_pattern(new SampleNamingConventionPattern("12", TumorNormal.NORMAL, "buccal cell normal"));
      snc.add_pattern(new SampleNamingConventionPattern("13", TumorNormal.NORMAL, "EBV normal"));
      snc.add_pattern(new SampleNamingConventionPattern("14", TumorNormal.NORMAL, "bone mrrow normal"));

      snc.add_pattern(new SampleNamingConventionPattern("20", CELL_LINE_TN_HACK, "cell line control"));

      snc.add_pattern(new SampleNamingConventionPattern("40", TumorNormal.TUMOR, "recurrent blood cancer"));
      snc.add_pattern(new SampleNamingConventionPattern("50", TumorNormal.TUMOR, "cancer cell line"));

      snc.add_pattern(new SampleNamingConventionPattern("60", TumorNormal.TUMOR, "xenograft, primary"));
      snc.add_pattern(new SampleNamingConventionPattern("70", TumorNormal.TUMOR, "xenograft, cell-line derived"));
      

    }

    return known_conventions;
  }

  public static void import_convention (String s) {
    String[] chunks = s.split(",");
    int ptr = 0;

    if (chunks.length >= 5 && (chunks.length - 2) % 3 == 0) {
      SampleNamingConvention snc = new SampleNamingConvention(chunks[ptr++]);
      SampleNamingConvention.get_known_conventions().add(snc);
      snc.global_pattern = chunks[ptr++];

      int left = chunks.length - 2;
      if (left > 0 && left % 3 == 0) {
	int count = left / 3;
	for (int j=0; j < count; j++) {
	  String type_string = chunks[ptr++];
	  TumorNormal tn = TumorNormal.valueOfString(chunks[ptr++]);
	  String desc = chunks[ptr++];
	  if (desc != null && desc.equalsIgnoreCase("null")) desc = null;
	  snc.add_pattern(new SampleNamingConventionPattern(
							    type_string,
							    tn,
							    desc
							    ));
	}
      } else {
	System.err.println("naming convention spec error");  // debug
      }
    } else {
      System.err.println("naming convention spec error: specify convention_name,regexp,tn_string,[TN],desc[,tn_string,[TN],desc...]");  // debug
    }

  }

  public static void main (String[] argv) {
    if (argv.length > 0) {
      for (int i=0; i < argv.length; i++) {
	if (argv[i].equals("-convention")) {
	  SampleNamingConvention.import_convention(argv[++i]);
	} else if (argv[i].equals("-v")) {
	  VERBOSE = true;
	} else {
	  String thing = argv[i];
	  ArrayList<SampleNamingConvention> conventions = SampleNamingConvention.get_known_conventions();
	  System.err.println("conventions: " + conventions.size());  // debug

	  for (SampleNamingConvention snc : conventions) {
	    if (snc.matches(thing)) {
	      System.err.println("matches naming convention: " + snc.convention_name);  // debug
	      SampleMatchInfo info = snc.get_match_info();
	      System.err.println("sample_name: " + info.sample_name);  // debug
	      System.err.println("desc: " + info.description);
	      System.err.println("tn: " + info.tumor_normal);
	      System.err.println("");  // debug
	    }
	  }
	}
      }
    } else {
      System.err.println("specify test file name");  // debug
    }
  }
  





}