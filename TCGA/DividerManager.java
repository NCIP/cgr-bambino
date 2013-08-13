package TCGA;

import java.util.*;
import java.awt.*;

public class DividerManager {
  private ArrayList<Cluster> cluster_list = null;
  private GenomicMeasurement gm = null;
  private DividerSet cluster_dividers, patient_dividers;

  public DividerManager (GenomicMeasurement gm) {
    this.gm = gm;
    setup();
  }

  private void setup() {
    // dividers between patients (when multiple samples per patient in dataset):
    patient_dividers = new DividerSet();
    patient_dividers.color = Color.lightGray;
    patient_dividers.min_weight = 0.10f;
    patient_dividers.max_weight = 1.0f;
    patient_dividers.full_weight_at_zoom = 8.0f;

    // dividers between clusters:
    cluster_dividers = new DividerSet();
    cluster_dividers.color = Color.yellow;
    cluster_dividers.min_weight = 0.30f;
    cluster_dividers.max_weight = 1.10f;
    cluster_dividers.full_weight_at_zoom = 8.0f;
  }

  public void set_cluster_list (ArrayList<Cluster> cluster_list) {
    this.cluster_list = cluster_list;
    rebuild_cluster_dividers();
  }

  public DividerSet get_cluster_dividers() {
    cluster_dividers.color = gm.get_color_manager().get_cluster_divider_color();
    return cluster_dividers;
  }

  public void clear_cluster_dividers() {
    cluster_list = null;
    cluster_dividers.clear();
  }

  public DividerSet get_patient_dividers() {
    ColorManager cm = gm.get_color_manager();
    if (cm != null) patient_dividers.color = cm.get_patient_divider_color();
    return patient_dividers;
  }
  
  public void rebuild_patient_dividers() {
      patient_dividers.clear();

      int row = 0;
      String last_id = null;
      String this_id;
      int id_count = 0;
      boolean singles_only = true;

      for (GenomicSample gs : gm.get_visible_rows()) {
	if (gs.patient_id != null && last_id != null) {
	  if (gs.patient_id.equals(last_id)) {
	    id_count++;
	  } else {
	    // new ID
	    patient_dividers.add(row);
	    if (id_count > 1) singles_only = false;
	    id_count = 1;
	  }
	}
	last_id = gs.patient_id;
	row++;
      }
      patient_dividers.add(row);
      if (id_count > 1) singles_only = false;

      if (singles_only && Options.SUPPRESS_DIVIDERS_IF_ALL_SINGLETONS) {
	patient_dividers.clear();
      }

      //      System.err.println("dividers="+patient_dividers);  // debug

  }

  public HashSet<String> get_last_patients_in_clusters() {
    //
    //  return the set of last patient IDs in each cluster
    //
    HashSet<String> results = new HashSet<String>();
    if (cluster_list != null) {
      for (Cluster c : cluster_list) {
	ArrayList<GenomicSample> sd = c.get_sample_data();
	results.add(sd.get(sd.size() - 1).patient_id);
      }
    }
    return results;
  }

  public void rebuild_cluster_dividers() {
    //
    //  set dividers between clusters
    //
    cluster_dividers.clear();
    if (cluster_list != null) {
      String last_id=null;
      int di = 0;

      HashSet<String> last_patient_in_cluster = get_last_patients_in_clusters();

      for (GenomicSample gs : gm.get_visible_rows()) {
	if (last_id != null && !gs.patient_id.equals(last_id) && last_patient_in_cluster.contains(last_id)) {
	  cluster_dividers.add(Integer.valueOf(di));
	}
	last_id = gs.patient_id;
	di++;
      }

    }
  }


}

