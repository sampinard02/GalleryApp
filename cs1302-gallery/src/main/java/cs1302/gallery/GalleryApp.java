package cs1302.gallery;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URLEncoder;
import java.io.IOException;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ChoiceBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ProgressBar;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Represents an iTunes Gallery App.
 */
public class GalleryApp extends Application {

    /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()                          // enable nice output when printing
        .create();                                    // builds and returns a Gson object

    private Stage stage;
    private Scene scene;
    private HBox root;
    private VBox subroot;
    private HBox topRow;
    private Button playButton;
    private Label search;
    private TextField searchBar;
    private ChoiceBox<String> mediaType;
    private Button getImages;
    private HBox secondRow;
    private Text instructions;
    private HBox thirdRow;
    private GalleryImages images;
    private HBox bottomRow;
    private ProgressBar pBar;
    private Text credit;
    private Boolean playing;
    private Boolean defaultImages;

    /**
     * Constructs a {@code GalleryApp} object}.
     */
    public GalleryApp() {
        this.stage = null;
        this.scene = null;
        this.root = new HBox();
        this.subroot = new VBox();
        this.topRow = new HBox(5);
        this.playButton = new Button("Play");
        this.search = new Label ("Search: ");
        this.searchBar = new TextField("star wars");
        this.mediaType = new ChoiceBox<String>();
        this.getImages = new Button("Get Images");
        this.secondRow = new HBox();
        this.instructions = new Text("Type in a term, select a media type, then click the button.");
        this.images = new GalleryImages();
        this.bottomRow = new HBox(5);
        this.pBar = new ProgressBar(0);
        this.credit = new Text("Images provided by iTunes Search API.");
        this.defaultImages = true;
    } // GalleryApp

    /** {@inheritDoc} */
    @Override
    public void init() {
        playing = false;
        playButton.setStyle("-fx-font-size:12.5");
        search.setStyle("-fx-font-size:12.5");
        searchBar.setStyle("-fx-font-size:12.5");
        mediaType.setStyle("-fx-font-size:12.5");
        getImages.setStyle("-fx-font-size:12.5");
        instructions.setStyle("-fx-font-size:12.5");
        credit.setStyle("-fx-font-size:12.5");
        playButton.setDisable(true);
        mediaType.getItems().addAll("movie", "podcast", "music", "musicVideo", "audiobook",
            "shortFilm", "tvShow", "software", "ebook", "all");
        mediaType.setValue("music");
        topRow.setPadding(new Insets(5));
        topRow.setAlignment(Pos.BASELINE_LEFT);
        EventHandler<ActionEvent> handler = event -> images.randomReplacement();
        KeyFrame keyFrame = new KeyFrame(Duration.seconds(2), handler);
        Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(keyFrame);
        EventHandler<ActionEvent> buttonHandler = event -> {
            timeline.pause();
            this.itunesQuery();
        };
        getImages.setOnAction(buttonHandler);
        EventHandler<ActionEvent> play = event -> {
            this.randomReplacementButtonPressed(timeline);
        };
        playButton.setOnAction(play);
        topRow.getChildren().addAll(playButton, search, searchBar, mediaType, getImages);
        secondRow.setPadding(new Insets(5));
        secondRow.getChildren().add(instructions);
        pBar.setPrefSize(240, 20);
        bottomRow.setPadding(new Insets(5));
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.getChildren().addAll(pBar, credit);
        subroot.getChildren().addAll(topRow, secondRow, images, bottomRow);
        root.getChildren().add(subroot);
        System.out.println("init() called");
    } // init

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(this.root);
        this.stage.setOnCloseRequest(event -> Platform.exit());
        this.stage.setTitle("GalleryApp!");
        this.stage.setScene(this.scene);
        this.stage.sizeToScene();
        this.stage.show();
        Platform.runLater(() -> this.stage.setResizable(false));
    } // start

    /** {@inheritDoc} */
    @Override
    public void stop() {
        // feel free to modify this method
        System.out.println("stop() called");
    } // stop

    /**
     * Retreives a JSON response string for a query to the iTunes Search API based on a query
     * string, and calls the {@code getURIs()} method to get an ArrayList of URI strings. Then
     * calls the {@code loadImages()} method of the GalleryImages class based on these URIs.
     */
    private void itunesQuery() {
        playButton.setText("Play");
        playing = false;
        playButton.setDisable(true);
        getImages.setDisable(true);
        instructions.setText("Getting images...");
        String term = URLEncoder.encode(searchBar.getText(), StandardCharsets.UTF_8);
        String limit = URLEncoder.encode("200", StandardCharsets.UTF_8);
        String media = URLEncoder.encode(mediaType.getValue(), StandardCharsets.UTF_8);
        String query = String.format("?term=%s&limit=%s&media=%s", term, limit, media);
        String uri = "https://itunes.apple.com/search" + query;
        Runnable task = () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
                if (!response.toString().equals("(GET " + uri + ") 200")) {
                    throw new IOException(response.toString());
                }
                String body = response.body();
                ArrayList<String> urls = this.getURIs(body);
                images.loadImages(urls, pBar);
                instructions.setText(request.uri().toString());
                playButton.setDisable(false);
                this.setDefaultImages(false);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorAlert(e, uri);
                });
                if (!this.defaultImages) {
                    playButton.setDisable(false);
                    pBar.setProgress(1);
                }
            }
            getImages.setDisable(false);
        };
        Thread taskThread = new Thread(task);
        taskThread.setDaemon(true);
        taskThread.start();
    }

    /**
     * Returns a list of URI strings based on JSON response string retrieved from the iTunes
     * Search API. Also uses the {@code isUnique()} method to only add unique URIs to the list.
     *
     * @param response the JSON response string
     * @return a list of URI strings
     */
    private ArrayList<String> getURIs(String response) {
        ItunesResponse itunesResponse = GSON.fromJson(response, ItunesResponse.class);
        ItunesResult[] results = itunesResponse.results;
        ArrayList<String> uris = new ArrayList<String>();
        for (int i = 0; i < results.length; i++) {
            if (isUnique(uris, results[i].artworkUrl100)) {
                uris.add(results[i].artworkUrl100);
            }
        }
        return uris;
    }

    /**
     * Checks to see if a given URI String already exists in the given ArrayList.
     *
     * @param list the ArrayList of URI Strings
     * @param uri the URI String to look for
     * @return whether the URI is unique withing the ArrayList
     */
    private boolean isUnique(ArrayList<String> list, String uri) {
        for (int i = 0; i < list.size(); i++) {
            if (uri.equals(list.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts or stops Random Replacement depending on the current state of {@code playButton}.
     *
     * @param timeline the javafx.animation.Timeline to be paused or played.
     */
    private void randomReplacementButtonPressed(Timeline timeline) {
        if (playing) {
            playing = false;
            playButton.setText("Play");
            timeline.pause();
        } else {
            playing = true;
            playButton.setText("Pause");
            timeline.play();
        }
    }

    /**
     * Displays an alert based on the given Exception.
     *
     * @param e the Exception to display within the alert
     * @param url the URL that caused the Exception
     */
    private void errorAlert(Exception e, String url) {
        this.instructions.setText("Last attempt to get images failed...");
        Alert a = new Alert(AlertType.ERROR);
        String eFQN = e.getClass().toString().substring(6);
        a.setContentText("URL : " + url + "\n\nException: " + eFQN + ": " + e.getMessage());
        a.showAndWait();
    }

    /**
     * Sets the value of {@code defaultImages} to the parameter.
     *
     * @param stillDefault the boolean value to assign to {@code defaultImages}
     */
    private void setDefaultImages(boolean stillDefault) {
        this.defaultImages = stillDefault;
    }

} // GalleryApp
