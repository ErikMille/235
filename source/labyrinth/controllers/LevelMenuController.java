package source.labyrinth.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import source.labyrinth.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.*;

/**
 * LevelMenuController let user choose level number of players and associated profiles.
 *
 * @author Erik Miller
 */
public class LevelMenuController implements Initializable {
    private static ArrayList<String> profilesChosen = new ArrayList<>();
    private static String selectedLevel;
    private static HBox selectedHBox;
    private static int numberOfPlayers = 2;
    private static ArrayList<String> profileNames;
    private Board board;
    private ArrayList<int[]> playerLocations = new ArrayList<>();
    private HashMap<String, Integer> silkbagAmounts;
    private int tileRenderSize;
    private static float playerToTileScale = 0.6f;

    @FXML
    private VBox vboxLevels;
    @FXML
    private VBox vboxPlayers;
    @FXML
    private Button addPlayerButton;
    @FXML
    private Button removePlayerButton;
    @FXML
    private TableView<Profile> tableView;
    @FXML
    private TableColumn<Profile, Integer> winCol;
    @FXML
    private TableColumn<Profile, String> nameCol;
    @FXML
    private ChoiceBox levelSelect;
    @FXML
    private VBox boardContainer;
    @FXML
    private HBox silkBagContainer;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        levelSelect.getItems().add("Default");
        levelSelect.getItems().add("Custom");
        levelSelect.setValue("Default");
        levelSelect.setOnAction(event -> {
            System.out.println(levelSelect.getValue());
            selectedLevel = null;
            renderLevels();
            renderLeaderBoard();
        });

        renderLevels();
        renderPlayersChoiceBox();

        addPlayerButton.setOnMouseClicked(event -> {
            numberOfPlayers = numberOfPlayers == 4 ? 4 : numberOfPlayers + 1;
            renderPlayersChoiceBox();
        });
        removePlayerButton.setOnMouseClicked(event -> {
            numberOfPlayers = numberOfPlayers == 2 ? 2 : numberOfPlayers - 1;
            if (profilesChosen.size() > numberOfPlayers) profilesChosen.remove(numberOfPlayers);
            renderPlayersChoiceBox();
        });

        renderLeaderBoard();

        System.out.println("Created LevelMenuController");
    }

    /**
     * starts game
     *
     * @param event click on button
     */
    @FXML
    public void playGame(ActionEvent event) {
        if (selectedLevel != null) {
            System.out.println("Going to board ...");
            String[] prof = new String[numberOfPlayers];
            for (int i = 0; i < prof.length; i++) {
                if (profilesChosen.size() > i) prof[i] = profilesChosen.get(i);
            }

            LevelController.setNextLevelToLoad(selectedLevel, prof);

            System.out.println("level: " + selectedLevel);
            for (String s : prof) {
                System.out.println("player " + s);
            }

            try {
                Parent profileMenuParent = FXMLLoader.load(getClass().getResource("../../resources/scenes/level.fxml"));
                Scene profileMenuScene = new Scene(profileMenuParent);
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(profileMenuScene);
                window.setTitle("Level Select");
                window.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Choose a level!");
            alert.showAndWait();
        }
    }

    /**
     * Return to the main menu screen
     *
     * @param event Event click to find current window
     */
    @FXML
    public void returnToMainMenu(ActionEvent event) {
        System.out.println("Going back to main menu...");
        try {
            Parent profileMenuParent = FXMLLoader.load(getClass().getResource("../../resources/scenes/main_menu.fxml"));
            Scene profileMenuScene = new Scene(profileMenuParent);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();

            window.setScene(profileMenuScene);
            window.setTitle("Main Menu");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * changeLevelToRender loads file by name and refreshes data about level to preview
     * @param nextFileToLoad name of file
     */
    private void changeLevelToRender(String nextFileToLoad) {
        LevelData ld = null;
        if (nextFileToLoad != null) {
            ld = LevelIO.readDataFile("source/resources/" + nextFileToLoad + ".txt");
            board = ld.getBoard();
        } else {
            board = new Board(0, 0);
        }

        playerLocations.clear();
        if (ld != null) {
            int[][] previousPlayers = ld.getPlayerStartingPositions();
            for (int[] playerLocation : previousPlayers) {
                playerLocations.add(new int[]{playerLocation[0], playerLocation[1]});
            }
        }

        silkbagAmounts = new HashMap<>();
        for (ActionTile.ActionType actionType : ActionTile.ActionType.values()) {
            silkbagAmounts.put(actionType.name(), (ld != null ? ld.getActionTileAmount(actionType) : 0));
        }
        for (FloorTile.FloorType floorType : FloorTile.FloorType.values()) {
            silkbagAmounts.put(floorType.name(), (ld != null ? ld.getFloorTileAmount(floorType) : 0));
        }
    }

    /**
     * renderBoard renders board with players
     */
    private void renderBoard() {
        GridPane renderedBoard = new GridPane();
        renderedBoard.setAlignment(Pos.CENTER);
        boardContainer.setMinHeight((board.getHeight() * tileRenderSize) + (2 * tileRenderSize));
        boardContainer.setMinWidth((board.getWidth() * tileRenderSize) + (2 * tileRenderSize));

        // The actual board render
        for (int x = 0; x < this.board.getWidth(); x++) {
            for (int y = 0; y < this.board.getHeight(); y++) {
                FloorTile current = this.board.getTileAt(x,y);
                StackPane stack;

                if (current != null) {
                    stack = current.renderTile(tileRenderSize);
                } else {
                    Image img = new Image("source/resources/img/tile_none.png", tileRenderSize, tileRenderSize, false, false);
                    ImageView iv = new ImageView(img);
                    stack = new StackPane(iv);
                }

                renderedBoard.add(stack, x, y);
            }
        }

        playerLocations.forEach(location -> {
            int stackpaneLocation = location[0] * board.getHeight() + location[1];
            StackPane relevantStackPane = (StackPane) renderedBoard.getChildren().get(stackpaneLocation);

            ImageView playerImage = new ImageView(new Image("source/resources/img/player_default.png", tileRenderSize * playerToTileScale, tileRenderSize * playerToTileScale, false, false));
            relevantStackPane.getChildren().add(playerImage);
        });

        boardContainer.getChildren().clear();
        boardContainer.getChildren().add(renderedBoard);
    }

    /**
     * renderTiles renders silkbag for preview
     */
    public void renderTiles() {
        String imageURL = "source/resources/img/tile_none.png";
        silkBagContainer.getChildren().clear();
        for (FloorTile.FloorType ft : FloorTile.FloorType.values()) {
            imageURL = ft.imageURL;
            ImageView tileImg = new ImageView(new Image(imageURL, 64, 64, false, false));
            StackPane stack = new StackPane(tileImg);
            Text numOfTiles = new Text("" + silkbagAmounts.get(ft.name()));
            numOfTiles.setStyle("-fx-font-weight: bold; -fx-font-size: 26px; -fx-stroke: black; -fx-stroke-width: 1px");
            DropShadow shadow = new DropShadow(7, 0, 0, Color.BLACK);
            numOfTiles.setEffect(shadow);
            numOfTiles.setFill(Color.GREEN);
            stack.getChildren().add(numOfTiles);
            silkBagContainer.getChildren().add(stack);
        }
        for (ActionTile.ActionType at : ActionTile.ActionType.values()) {
            imageURL = at.imageURL;
            ImageView tileImg = new ImageView(new Image(imageURL, 64, 64, false, false));
            StackPane stack = new StackPane(tileImg);
            Text numOfTiles = new Text("" + silkbagAmounts.get(at.name()));
            numOfTiles.setStyle("-fx-font-weight: bold; -fx-font-size: 26px; -fx-stroke: black; -fx-stroke-width: 1px");
            DropShadow shadow = new DropShadow(7, 0, 0, Color.BLACK);
            numOfTiles.setEffect(shadow);
            numOfTiles.setFill(Color.GREEN);
            stack.getChildren().add(numOfTiles);
            silkBagContainer.getChildren().add(stack);
        }
    }

    /**
     * renders leaderboard for every level on click on this level
     */
    private void renderLeaderBoard() {
        tableView.getItems().clear();
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("./source/resources/leaderboards/" + selectedLevel + "_leaderboard.ser"));
            HashMap<Integer, Integer> leaderboardInfo = (HashMap<Integer, Integer>) objectInputStream.readObject();
            objectInputStream.close();

            ArrayList<Profile> profiles = new ArrayList<>();
            leaderboardInfo.forEach((id, wins) -> {
                Profile p = ProfileManager.getProfileById(id);
                if (p != null) {
                    profiles.add(new Profile(p.getName(), id, 0, wins, 0));
                }
            });
            profiles.sort(Comparator.comparingInt(Profile::getWins));
            Collections.reverse(profiles);

            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            winCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
            tableView.getItems().setAll(profiles);
        } catch (IOException | ClassNotFoundException e) {
            tableView.setPlaceholder(new Label("Leaderboard is empty"));
        }

    }

    /**
     * renders level list in menu
     */
    private void renderLevels() {
        vboxLevels.getChildren().clear();
        getLevels().forEach((value) -> {
            HBox levelHBox = new HBox(new Text(value.substring(0, value.length() - 4)));
            levelHBox.setPrefHeight(30);
            levelHBox.setAlignment(Pos.CENTER_LEFT);
            levelHBox.setStyle("-fx-border-color: black");
            levelHBox.setOnMouseClicked(event -> {
                if (selectedHBox != null) {
                    selectedHBox.setStyle("-fx-border-color: black");
                }
                selectedHBox = levelHBox;
                selectedLevel = levelSelect.getSelectionModel().getSelectedIndex() == 0 ? "levels/" : "custom_levels/";
                selectedLevel += value.substring(0, value.length() - 4);
                System.out.println(selectedLevel);
                levelHBox.setStyle("-fx-border-color: black;-fx-background-color: #c4ffd5;");
                renderLeaderBoard();
                changeLevelToRender(selectedLevel);
                renderBoard();
                renderTiles();
            });
            vboxLevels.getChildren().addAll(levelHBox);
        });
    }

    /**
     * renders player profile choice in menu
     */
    private void renderPlayersChoiceBox() {
        vboxPlayers.getChildren().clear();

        ArrayList<Profile> profiles = ProfileManager.getProfiles();
        profileNames = new ArrayList<>();
        profiles.forEach(profile -> profileNames.add(profile.getName()));

        for (int i = 0; i < numberOfPlayers; i++) {
            ChoiceBox<String> pChoiceBox = new ChoiceBox<>();
            pChoiceBox.setPrefWidth(250);
            pChoiceBox.getItems().addAll(profileNames);

            profilesChosen.forEach(prof -> pChoiceBox.getItems().remove(prof));
            if (profilesChosen.size() > i) {
                pChoiceBox.getItems().addAll(profilesChosen.get(i));
            }
            if (profilesChosen.size() > i) {
                pChoiceBox.getSelectionModel().select(profilesChosen.get(i));
            }

            pChoiceBox.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
                        if (!oldValue.equals(-1)) {
                            profilesChosen.remove(pChoiceBox.getItems().get((oldValue.intValue())));
                        }
                        profilesChosen.add(pChoiceBox.getItems().get((newValue.intValue())));
                        renderPlayersChoiceBox();
                    }
            );
            vboxPlayers.getChildren().addAll(pChoiceBox);
        }

    }

    /**
     * reeds level names fromm files
     *
     * @return level names as string
     */
    private ArrayList<String> getLevels() {
        File levelsFiles;
        if (levelSelect.getSelectionModel().getSelectedIndex() == 0) {
            levelsFiles = new File("./source/resources/levels");
        } else {
            levelsFiles = new File("./source/resources/custom_levels");
        }
        ArrayList<String> levels = new ArrayList<>();
        for (File f : Objects.requireNonNull(levelsFiles.listFiles())) {
            levels.add(f.getName());
        }
        return levels;
    }
}
