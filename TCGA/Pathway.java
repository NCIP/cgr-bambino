package TCGA;

import java.util.ArrayList;

public class Pathway {
  public String name;
  public int pathway_id;
  public ArrayList<String> genes;

  public Pathway (String name) {
    this.name=name;
    genes = new ArrayList<String>();
  }

  public void add_gene (String gene) {
    genes.add(gene);
  }

}

