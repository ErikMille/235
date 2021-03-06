package source.labyrinth.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
 * SaveMenuController are able to load or delete saves.
 *
 * @author Max
 */
public class SaveMenuController implements Initializable {
	private static String selectedSaveName;
	private static HBox selectedSaveHBox;

	@FXML
	private VBox vboxSaves;
	@FXML
	private Button deleteSaveButton;
	@FXML
	private TextArea saveDetailTextArea;
	@FXML
	private Button loadSaveButton;
	@FXML
	private VBox boardContainer;
	@FXML
	private HBox silkBagContainer;

	private Player[] players;
	private Board board;
	private int tileRenderSize;


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		showSaveFile();
		System.out.println("Created SaveMenuController");
	}
	/**
	 * setupFromSaveFile will refresh data about save for preview
	 *
	 * @param saveName The file name of the save file
	 */
	private void setupFromSaveFile(String saveName) {
		FileInputStream fis;
		ObjectInputStream objectInputStream;
		try {
			fis = new FileInputStream("source/resources/saves/" + saveName);
			objectInputStream = new ObjectInputStream(fis);

			int currentTime = (int) objectInputStream.readObject();
			String currentLevelName = (String) objectInputStream.readObject();
			this.players = (Player[]) objectInputStream.readObject();
			int currentPlayer = (int) objectInputStream.readObject();
			this.board = (Board) objectInputStream.readObject();
			FloorTile floorTileToInsert = (FloorTile) objectInputStream.readObject();
			Object currentTurnPhase = objectInputStream.readObject();
			SilkBag.setEntireBag((LinkedList<Tile>) objectInputStream.readObject());

			renderBoard();

			objectInputStream.close();

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			System.out.println("Error reading save file");
		}
	}
	/**
	 * renderBoard renders board with players
	 */
	private void renderBoard() {
		boardContainer.getChildren().clear();

		GridPane renderedBoard = new GridPane();
		renderedBoard.setAlignment(Pos.CENTER);
		boardContainer.setMinHeight((board.getHeight() * tileRenderSize) + (2 * tileRenderSize));
		boardContainer.setMinWidth((board.getWidth() * tileRenderSize) + (2 * tileRenderSize));
		// The actual board render
		for (int x = 0; x < this.board.getWidth(); x++) {
			for (int y = 0; y < this.board.getHeight(); y++) {
				FloorTile current = this.board.getTileAt(x, y);
				StackPane stack = current.renderTile(tileRenderSize);
				renderedBoard.add(stack, x + 1, y + 1);
			}
		}
		boardContainer.getChildren().add(renderedBoard);
	}

	/**
	 * Shows every found save file.
	 */
	private void showSaveFile() {
		vboxSaves.getChildren().clear();
		getSaves().forEach((savName) -> {
			HBox saveFile = new HBox(new Text(savName.substring(0, savName.length() - 4)));

			saveFile.setPrefHeight(30);
			saveFile.setAlignment(Pos.CENTER_LEFT);
			saveFile.setStyle("-fx-border-color: #c4fffd");

			saveFile.setOnMouseClicked(event -> {
				// Useful if this is the first time we click on a save
				deleteSaveButton.setDisable(false);
				loadSaveButton.setDisable(false);

				System.out.println(savName);

				File levelsFiles = new File("./source/resources/saves");
				for (File f : Objects.requireNonNull(levelsFiles.listFiles())) {
					if (savName.equals(f.getName()))
						saveDetailTextArea.setText("Save Name:\n" + f.getName() + "\n\nSave location:\n" + f.getAbsolutePath());
				}


				if (selectedSaveHBox != null) {
					selectedSaveHBox.setStyle("-fx-border-color: #c4fffd");
				}

				saveFile.setStyle("-fx-border-color: black;-fx-background-color: #ffb5b5;");
				selectedSaveHBox = saveFile;
				selectedSaveName = savName;
				setupFromSaveFile(savName);
			});
			vboxSaves.getChildren().addAll(saveFile);
		});
	}

	/**
	 * Get a list of all saves.
	 *
	 * @return ArrayList of save files
	 */
	private ArrayList<String> getSaves() {
		File actual = new File("./source/resources/saves");
		ArrayList<String> saves = new ArrayList<>();
		for (File save : Objects.requireNonNull(actual.listFiles())) {
			saves.add(save.getName());
		}
		return saves;
	}

	/**
	 * Goes to the main menu.
	 *
	 * @param event Click event to get current window
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
	 * Delete the currently selected save.
	 */
	@FXML
	public void deleteSave() {
		if (selectedSaveHBox != null) {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Delete Save");
			alert.setHeaderText("Warning !");
			alert.setContentText("Are you sure you want to delete these files ?");

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				File delFile = new File("./source/resources/saves/" + selectedSaveName);
				delFile.delete();

				Alert deleted = new Alert(Alert.AlertType.INFORMATION);
				deleted.setTitle("Delete Save");
				deleted.setHeaderText("File deleted");
				deleted.setContentText("File " + selectedSaveName + " deleted");
				deleted.showAndWait();

				// avoid to load a deleted file
				selectedSaveName = null;
				selectedSaveHBox = null;

				deleteSaveButton.setDisable(true);
				loadSaveButton.setDisable(true);
				showSaveFile();//refresh the saveData list
			} else {
				System.out.println("Delete Cancelled");
			}
		} else {
			System.out.println("Error Selection");
		}
	}


	/**
	 * Loads the currently selected save.
	 *
	 * @param event Event to find current window.
	 */
	@FXML
	public void loadSave(ActionEvent event) {
		if (selectedSaveName != null) {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Load Save");
			alert.setHeaderText("Loading Save");
			alert.setContentText("Load this save and continue the game?");

			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				LevelController.setNextSaveToLoad(selectedSaveName);
				try {
					Parent profileMenuParent = FXMLLoader.load(getClass().getResource("../../resources/scenes/level.fxml"));
					Scene profileMenuScene = new Scene(profileMenuParent);
					Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
					window.setScene(profileMenuScene);
					window.setTitle("Game");
					window.show();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Load Cancelled");
			}
		}
	}
}
