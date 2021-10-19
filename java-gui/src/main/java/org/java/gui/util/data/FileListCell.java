package org.java.gui.util.data;

import java.io.File;

import javafx.scene.control.ListCell;

public class FileListCell extends ListCell<File> {

	// private HBox content = new HBox();
	// private TextField textField = new TextField();
	// private Button actionButton = new Button("Action");

	public FileListCell() {

	}

	@Override
	protected void updateItem(File item, boolean empty) {
		super.updateItem(item, empty);
		// new item
		if (item == null || empty) {
			setText(null);
			setGraphic(null);
		} else {
			// setGraphic(content);
			// checkIfDisableButton(item.getName());
			// item.nameProperty().addListener(textListener);
			// textField.textProperty().bindBidirectional(item.nameProperty());
			// actionButton.setOnAction(e -> {
			setText(item.getAbsolutePath());
			// System.out.println(item.getName());
		}
	}

}