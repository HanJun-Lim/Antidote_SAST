package org.java.gui.util.data;

import java.io.File;

import org.java.gui.Main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// TreeView내의 각각의 트리 아이템 클래스 : FileTreeItem
public class FileTreeItem extends CheckBoxTreeItem<File> {

	// --------------- 멤버 변수 ---------------
	private boolean isFirstTimeChildren = true;
	private boolean isFirstTimeLeaf = true;
	private boolean isLeaf;

	// --------------- 생성자 ---------------
	public FileTreeItem(File f) {
		super(f);
		ImageView iv = new ImageView();

		// 체크 박스가 있는 트리 아이템의 핸들러, 파일의 경우에만 핸들러 등록
		// 1. 체크가 되면 검사 대상 리스트에 추가 / 체크 해제 시 검사 대상 리스트에서 제거
		if (isLeaf()) {
			this.addEventHandler(CheckBoxTreeItem.<File>checkBoxSelectionChangedEvent(),
					(TreeModificationEvent<File> e) -> {
						CheckBoxTreeItem<File> item = e.getTreeItem();

						if (item.isSelected()) {
							System.out.println("checked : " + item.getValue().getAbsolutePath());
							Main.filesToVerify.add(item.getValue());
							//
							System.out.println(Main.filesToVerify.size());
						} else {
							System.out.println("unchecked : " + item.getValue().getAbsolutePath());
							Main.filesToVerify.remove(item.getValue());
							System.out.println(Main.filesToVerify.size());
						}
					});
			iv.setImage(new Image(getClass().getResourceAsStream("/org/java/gui/icon_java.png")));
			setGraphic(iv);
		} else {
			iv.setImage(new Image(getClass().getResourceAsStream("/org/java/gui/icon_folder.png")));
			setGraphic(iv);
		}
	}

	// --------------- 멤버 메소드 ---------------

	@Override
	public ObservableList<TreeItem<File>> getChildren() {
		if (isFirstTimeChildren) {
			isFirstTimeChildren = false;

			/*
			 * First getChildren() call, so we actually go off and determine the children of
			 * the File contained in this TreeItem.
			 */
			super.getChildren().setAll(buildChildren(this));
		}
		return super.getChildren();
	}

	@Override
	public boolean isLeaf() {
		if (isFirstTimeLeaf) {
			isFirstTimeLeaf = false;
			File f = (File) getValue();
			isLeaf = f.isFile();
		}

		return isLeaf;
	}

	private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
		File f = TreeItem.getValue();

		if (f != null && f.isDirectory()) {
			File[] files = f.listFiles();

			// 자식이 존재함 (하위 파일이 존재함)
			if (files != null) {
				ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();

				for (File childFile : files) {
					if (childFile.isDirectory() || isJavaFile(childFile)) {
						children.add(new FileTreeItem(childFile));
					}
				}
				return children;
			}
		}

		return FXCollections.emptyObservableList();
	}

	private boolean isJavaFile(File f) {
		if (getFileExtension(f).matches("java")) {
			return true;
		}

		return false;
	}

	private static String getFileExtension(File file) {
		String fileName = file.getName();
		if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		else
			return "";
	}
}
