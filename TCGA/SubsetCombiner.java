package TCGA;

import java.util.*;

public class SubsetCombiner {
  private GenomicMeasurement gm;
  private GenomicMeasurement gm_combined;

  public SubsetCombiner (GenomicMeasurement gm,
			 String subset_a, ByteComparator bc_a,
			 String subset_b, ByteComparator bc_b,
			 BooleanComparator bc_join
			 ) {
    // - iterate through (sorted) sample order
    // - foreach patient ID
    // - retrieve sample data
    // - evaluate comparators
    // - pixel = conditions met ? true : false

    ArrayList<GenomicSample> rows_raw = gm.get_rows_raw();
    ArrayList<String> patients = GenomicSample.get_patient_id_list(rows_raw);
    SampleSortTools sst = new SampleSortTools(rows_raw);

    DoubleHashMap ps_map = sst.build_patient_subset_map();
    // map patient -> subset -> row

    int i;
    boolean conditional_a, conditional_b;
    byte dp_a, dp_b, dp_result;
    String[] headers = gm.get_headers();

    ArrayList<GenomicSample> results = new ArrayList<GenomicSample>();

    for (String patient : patients) {
      // foreach patient (original ordering)

      GenomicSample gs_a = (GenomicSample) ps_map.get(patient, subset_a);
      GenomicSample gs_b = (GenomicSample) ps_map.get(patient, subset_b);
      GenomicSample gs_new;

      if (gs_a != null && gs_b != null) {
	gs_new = new GenomicSample();
	gs_new.sample_id = "results for " + patient;
	gs_new.patient_id = patient;
	gs_new.copynum_data = new byte[gs_a.copynum_data.length];
	results.add(gs_new);

	for (i=0; i < gs_a.copynum_data.length; i++) {
	  dp_a = gs_a.copynum_data[i];
	  dp_b = gs_b.copynum_data[i];
	  conditional_a = dp_a == GenomicSample.NULL_VALUE ? false : bc_a.compare(dp_a);
	  conditional_b = dp_b == GenomicSample.NULL_VALUE ? false : bc_b.compare(dp_b);

	  //	  System.err.println("a="+dp_a + " b="+dp_b + " ca=" + conditional_a + " cb=" + conditional_b);  // debug

	  if (bc_join.compare(conditional_a, conditional_b)) {
	    //	    System.err.println(headers[i] + " HEY NOW for " + patient + " @" + i + ": a=" + dp_a + " b=" + dp_b);
	    dp_result = 1;
	    //	    System.err.println("result=1");  // debug
	  } else {
	    dp_result = 0;
	  }
	  gs_new.copynum_data[i] = dp_result;
	}
      }
    }

    System.err.println("src="+rows_raw.size());  // debug
    System.err.println("rlen="+results.size());  // debug

    gm_combined = new GenomicMeasurement(results, gm);
  }

  public GenomicMeasurement get_results() {
    return gm_combined;
  }

}
