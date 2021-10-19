package org.java.gui.util.data;

import java.io.File;
import javafx.scene.control.cell.CheckBoxTreeCell;

public class FileTreeCell extends CheckBoxTreeCell<File> {
	public FileTreeCell() {
	}

	@Override
	public void updateItem(File item, boolean empty) {
		super.updateItem(item, empty);

		FileTreeItem treeItem = (FileTreeItem) getTreeItem();

		if (!empty) {
			// 디렉토리일 경우
			if (item.isDirectory()) {
				setText(item.getName());

				// 최상위 디렉토리일 경우
				if (treeItem.getParent() == null) {
					setText(item.getAbsolutePath());
					treeItem.setExpanded(true);
				}
			}
			// 파일일 경우
			else {
				setText(item.getName());
			}
		} else {
			setText(null);
			setGraphic(null);
		}
	}
}
