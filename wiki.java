import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class wiki {

    /**
     * This function starts my solr client
     * @return an an initialized solr client
     */
    public static HttpSolrClient startClient() {
        String urlString = "http://localhost:8983/solr/docs";
        HttpSolrClient solr = new HttpSolrClient.Builder(urlString).build();
        solr.setParser(new XMLResponseParser());
        return solr;
    }

    /**
     * This function runs a query on the specified field using phrase querying
     *
     * @param solr solr Instance
     * @param q the query
     * @return the response from Solr
     * @throws SolrServerException
     * @throws IOException
     */
    public static QueryResponse runQuery(HttpSolrClient solr, String q) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery();
        query.setRows(500);

        //tokenize query
        String tokenized = tokenizer(q);
        if (tokenized.length()>0){
            q=tokenized;
        }

        //this makes it a phrase and accepts phrases within 2 words
        q = "\"" + q + "\"";
        q = q + "~";
        //setup for Solr, searching
        q = "TITLE:" + q + " OR " + "SECTION_TEXT:" + q  + " OR " + "SECTION_TITLE:" + q;
        query.set("q", q);
        //apply weights to fields
        String qf="TITLE^1.1 SECTION_TEXT^0.3 SECTION_TITLE^0.6 ";
        query.set(qf);

        return solr.query(query);
    }

    /**
     * This function searches the TITLE: field for spellcheck results, with an edit distance of 2(default)
     * @param solr solr Instance
     * @param q query
     * @return the QueryResponse from Solr
     * @throws SolrServerException
     * @throws IOException
     */
    public static QueryResponse runQuerySP(HttpSolrClient solr, String q) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery();
        query.setRows(500);

        //tokenize query
        q = tokenizer(q);
        String[] qArr = q.split(" ");
        String querySP = "";
        for (String s: qArr){
            s = "TITLE:" + s + "~";
            querySP += s;
        }
        q = querySP;

        //setup for Solr, searching
        query.set("q", q);
        //apply weights to fields
        String qf = "TITLE^1.1 SECTION_TEXT^0.3 SECTION_TITLE^0.6 ";
        query.set(qf);

        return solr.query(query);
    }

    /*
     * this function tokenizes a string of words
     */
    public static String tokenizer(String input) throws IOException {
        //first i import my stopwords
        List<String> stopwords = Files.readAllLines(Paths.get("src/stoplist.txt"));

        input = input.replaceAll("[^a-zA-Z ]", "");
        ArrayList<String> words = Stream.of(input.toLowerCase().split(" "))
                .collect(Collectors.toCollection(ArrayList<String>::new));
        words.removeAll(stopwords);
        String[] str = new String[words.size()];
        Object[] objArr = words.toArray();
        int i = 0;
        for (Object obj : objArr) {
            str[i++] = (String)obj;
        }
        String result = Arrays.toString(str);
        //need to remove the [ ] caused by toString
        result = result.replaceAll("[^a-zA-Z ]", "");
        return result;
    }

    /**
     * main function
     * @param args
     * @throws SolrServerException
     * @throws IOException
     */
    public static void main(String[] args) throws SolrServerException, IOException {
        HttpSolrClient solr = startClient();

        String query = "anarchism";
        QueryResponse response = runQuery(solr, query);


        SolrDocumentList results = response.getResults();
        for (SolrDocument article: results){
            Object sectTitle = article.getFieldValue("SECTION_TITLE");
            Object sectText = article.getFieldValue("SECTION_TEXT");
            Object articleID = article.getFieldValue("ARTICLE_ID");
            Object title = article.getFieldValue("TITLE");


            System.out.println(title + "||  article ID: " + articleID);
            System.out.println( "|||" + sectTitle+ "|||");
            System.out.println(sectText);

        }
        System.out.println(results.getNumFound());
    }



}


