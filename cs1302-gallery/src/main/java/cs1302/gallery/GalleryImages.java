package cs1302.gallery;

import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import java.util.ArrayList;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Custom component that contains all of the images.
 */
public class GalleryImages extends VBox {
    private VBox root;
    private HBox[] rows;
    ImageView[] imageViews;
    ArrayList<Image> artwork;
    ArrayList<Image> currentlyDisplayed;

    /**
     * Constructs the GalleryImages object.
     */
    public GalleryImages() {
        super();
        this.root = new VBox();
        HBox firstRow = new HBox();
        HBox secondRow = new HBox();
        HBox thirdRow = new HBox();
        HBox fourthRow = new HBox();
        this.rows = new HBox[] {firstRow, secondRow, thirdRow, fourthRow};
        imageViews = new ImageView[20];
        int count = 0;
        /*loops through the HBox rows, while at the same time looping through imageViews.
          Five ImageViews are placed in each row and the default image is loaded into each*/
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < 5; j++) {
                Image newImage = new Image("file:resources/default.png", 100, 100, false, false);
                imageViews[count] = new ImageView(newImage);
                rows[i].getChildren().add(imageViews[count]);
                count++;
            }
            root.getChildren().add(rows[i]);
        }
        this.getChildren().add(root);
    }

    /**
     * If there at least 21 URIs are provided, all of the images are donwloaded and the first 20 are
     * displayed in the ImageViews. Otherwise, an IllegalArgumentException is thrown.
     *
     * @param artworkUrls an ArrayList containing all of the unique artwork urls
     * @param bar the ProgressBar to be updated as images are downloaded
     */
    public void loadImages(ArrayList<String> artworkUrls, ProgressBar bar)
        throws IllegalArgumentException {
        bar.setProgress(0);
        if (artworkUrls.size() < 21) {
            throw new IllegalArgumentException(artworkUrls.size() +
                " distinct results were found, but 21 or more are required.");
        } else {
            artwork = new ArrayList<Image>();
            currentlyDisplayed = new ArrayList<Image>();
            /*downloads all of Images from the supplied URIs and adds them to the artwork ArrayList,
            as well as updating the progress bar as this occurs*/
            for (int i = 0; i < artworkUrls.size(); i++) {
                artwork.add(new Image(artworkUrls.get(i), 100, 100, false, false));
                bar.setProgress((double) i / artworkUrls.size());
            }
            bar.setProgress(1.0);
            /*adds the first 20 Images into the ImageViews and adds these images to the
            currentlyDisplayed ArrayList*/
            for (int i = 0; i < this.imageViews.length; i++) {
                this.imageViews[i].setImage(artwork.get(i));
                currentlyDisplayed.add(artwork.get(i));
            }
        }
    }

    /**
     * Randomly switches out one of the 20 images currently being displayed with a random image that
     * is not currently being displayed.
     */
    public void randomReplacement() {
        //randomly generates the index of the ImageView to be changed
        int imageViewIndex = (int) ((Math.random() * currentlyDisplayed.size()));
        //randomly generates an index of an Image from the artwork ArrayList
        int newImageIndex = (int) (Math.random() * artwork.size());
        //rerolls the Image index until the index reflects an Image that is not currenly displayed
        while (this.isAlreadyDisplayed(artwork.get(newImageIndex))) {
            newImageIndex = (int) (Math.random() * artwork.size());
        }
        //The old Image located at the chosen ImageView is removed from currentlyDisplayed
        currentlyDisplayed.remove(imageViews[imageViewIndex].getImage());
        //the ImageView's image is updated
        imageViews[imageViewIndex].setImage(artwork.get(newImageIndex));
        //the newly displayed Image is added to currentlyDisplayed
        currentlyDisplayed.add(artwork.get(newImageIndex));
    }

    /**
     * Checks to see if an image is already being displayed.
     *
     * @param image the image to be checked
     * @return whether the image is already being displayed
     */
    private boolean isAlreadyDisplayed(Image image) {
        for (int i = 0; i < currentlyDisplayed.size(); i++) {
            if (image.equals(currentlyDisplayed.get(i))) {
                return true;
            }
        }
        return false;
    }
}
