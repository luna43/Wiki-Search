import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


import static java.lang.Math.random;

public class wikiFX extends Application {
    /**
     * This is the HashMap I use to store the articles returned by Solr
     */
    HashMap<Object, String> articleMap = new HashMap<Object, String>();

    /**
     * This function sets a grid
     *
     */
    public GridPane newGrid(){
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        return grid;
    }

    /**
     * This function takes all the sections and titles and makes a String that is the article
     * @param article
     */
    public void buildArticle(SolrDocument article){
        Object title = article.getFieldValue("TITLE");
        String result = "";

        if (!articleMap.containsKey(title)){
            //set up header
            articleMap.put(title,result);
        }
        result = articleMap.get(title);

        Object sectTitle = article.getFieldValue("SECTION_TITLE");
        Object sectText = article.getFieldValue("SECTION_TEXT");

        //clean up text
        String newText = "";
        if(sectText!=null){
            newText = sectText.toString().replaceAll("[^\\p{ASCII}]", "");
        }
        String newTitle = "";
        if(sectTitle!=null){
            newTitle = sectTitle.toString().replaceAll("[^\\p{ASCII}]", "");
        }

        result = result + "||" + newTitle + "||\n" + newText + "\n\n\n";
        articleMap.put(title,result);

    }

    /**
     * This function returns a newline seperated list of results from Solr
     * @param q String query
     * @return String results
     */
    public String doSolr(String q){
        HttpSolrClient solr = wiki.startClient();
        String query = q;
        String output = "";
        Object prevTitle = "";
        try {
            QueryResponse response = wiki.runQuery(solr, query);
            SolrDocumentList results = response.getResults();
            //something is misspelled so run spellchecked version
            if (results.getNumFound()<10){
                QueryResponse responseSP = wiki.runQuerySP(solr,query);
                results.addAll(responseSP.getResults());
            }
            for (SolrDocument article: results){
                buildArticle(article);
                Object title = article.getFieldValue("TITLE");
                if (title.toString().equals(prevTitle.toString())){
                    continue;
                }
                output = output + "\n" + title;
                prevTitle = title;
            }
        } catch (SolrServerException | IOException ex) {
            ex.printStackTrace();
        }
        return output;
    }

    /**
     * This function makes a new window for an article
     * @param article
     */
    public void articleWindow(String article){
        ScrollPane scrollPane = new ScrollPane();
        Stage stage3 = new Stage();
        stage3.initStyle(StageStyle.DECORATED);
        stage3.setWidth(1200);
        stage3.setHeight(1000);
        stage3.setX(700.0);
        stage3.setY(10.0);
        stage3.setTitle(article);
        GridPane grid3 = newGrid();

        Text ArticleTitle = new Text(article);
        ArticleTitle.setFont(Font.font("Calibri", FontWeight.NORMAL, 20));
        grid3.add(ArticleTitle, 0,0,2,1 );

        //add article text
        Text articleText = new Text(articleMap.get(article));

        scrollPane.setContent(articleText);
        articleText.setWrappingWidth(1000);
        articleText.setFont(Font.font("Calibri", FontWeight.NORMAL, 12));
        grid3.add(articleText, 0, 1, 2, 1);

        //make scrollpane
        scrollPane.setContent(grid3);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        stage3.setScene(new Scene(scrollPane, 450, 450));

        stage3.show();
    }

    /**
     * Main control function. Runs 3 windows: the main search window, the result list, and individual articles
     * @param stage
     */
    public void wikiSearch(Stage stage){
        //set window title, make grid
        stage.setTitle("Wiki-Search by Luna");
        GridPane grid = newGrid();

        //add title and searcbox
        Text scenetitle = new Text("Wiki-Search");
        scenetitle.setFont(Font.font("Calibri", FontWeight.NORMAL, 24));
        grid.add(scenetitle, 0, 0, 2, 1);

        Label searchBox = new Label("Search: ");
        grid.add(searchBox, 0, 1);
        TextField userTextField = new TextField();
        grid.add(userTextField, 1, 1);

        //add button
        Button btn = new Button("Go! ");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);
        /**
         * here the resultslist window starts
         */
        EventHandler<ActionEvent> makeResults;
        btn.setOnAction( makeResults = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                //get text from input
                String usertext = userTextField.getText();

                //set up new window
                Stage stage2 = new Stage();
                stage2.initStyle(StageStyle.DECORATED);
                stage2.setWidth(700);
                stage2.setHeight(500);
                stage2.setX(10.0);
                stage2.setY(350.0);
                stage2.setTitle("Results for : [" + usertext +"]");
                GridPane grid2 = newGrid();
                grid2.setHgap(10);

                //get solr results
                String output = doSolr(usertext);

                //add title
                Text scenetitle2 = new Text("Found " + articleMap.size() + " results for : [" + usertext +"]");
                scenetitle2.setFont(Font.font("Calibri", FontWeight.NORMAL, 20));
                grid2.add(scenetitle2, 0, 0, 2, 1);

                //make buttons
                String[] outputArr = output.split("\n");
                Set<String> articleSet = new HashSet<String>();
                Collections.addAll(articleSet, outputArr);
                int pos = 1;
                int pos2 = 0;
                int count = 1;
                for (String article : outputArr) {
                    if (article.length()<2){
                        continue;
                    }
                    //ensure no duplicates are shown
                    if(articleSet.contains(article)){
                        articleSet.remove(article);
                    } else {
                        continue;
                    }
                    String btnTitle = article.replaceAll("[^\\p{ASCII}]", "");
                    Button btn2 = new Button("(" + count + ")  " + btnTitle);
                    btn2.setFont(Font.font("Calibri", FontWeight.NORMAL, 14));
                    HBox hbBtn2 = new HBox(10);
                    hbBtn2.setAlignment(Pos.TOP_LEFT);
                    hbBtn2.getChildren().add(btn2);
                    grid2.add(hbBtn2, pos2, pos);

                    /**
                     * here the article window starts
                     */
                    btn2.setOnAction(actionEvent ->  {
                        articleWindow(article);
                    });

                    pos++;
                    count++;
                    if (count==21){
                        break;
                    }
                    if(pos==11){
                        pos2++;
                        pos=1;
                    }

                }
                BackgroundFill background_fill = new BackgroundFill(Color.LAVENDER, CornerRadii.EMPTY, Insets.EMPTY);
                Background background = new Background(background_fill);
                grid2.setBackground(background);
                stage2.setScene(new Scene(grid2, 450, 450));
                stage2.show();
            }
        });
        //set same action as search button but for pressing enter on the textfield
        userTextField.setOnAction(makeResults);

        //finish up
        BackgroundFill background_fill = new BackgroundFill(Color.LAVENDERBLUSH, CornerRadii.EMPTY, Insets.EMPTY);
        Background background = new Background(background_fill);
        grid.setBackground(background);
        Scene scene = new Scene(grid, 300, 275);

        stage.setScene(scene);
        stage.setX(10.0);
        stage.setY(10.0);
        stage.show();
    }

    /**
     * this function dispatches to my functions
     * @param stage
     */
    @Override
    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");

        //basic form
        wikiSearch(stage);
    }

    /**
     * main function, just calls dispatcher
     * @param args
     */
    public static void main(String[] args) {
        //directs to start()
        launch();
    }

}