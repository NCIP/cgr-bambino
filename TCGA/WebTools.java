package TCGA;

public class WebTools {

  public static String entrez_gene_link (String marker) {
    // URL to Entrez Gene for a human marker
    return "http://www.ncbi.nlm.nih.gov/sites/entrez?cmd=search&db=gene&term=" + marker + "[sym]%20AND%20%22Homo%20sapiens%22[orgn]";
  }

  public static String entrez_genbank_link (String gb) {
    //
    // URL to Entrez Gene for a core nucleotide accession
    //
    //    return "http://www.ncbi.nlm.nih.gov/sites/entrez?cmd=search&db=gene&term=" + marker + "[sym]%20AND%20%22Homo%20sapiens%22[orgn]";
    return "http://www.ncbi.nlm.nih.gov/sites/entrez?cmd=search&db=nuccore&format=text&term=" + gb;
  }


}