package org.java.gui.selectproject;

import java.io.File;

import org.java.gui.Main;
import org.java.gui.util.data.FileListCell;
import org.java.gui.util.data.FileTreeCell;
import org.java.gui.util.data.FileTreeItem;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

public class selectProjectController {

	// --------------- 멤버 변수 ---------------
	private File projectDir;

	@FXML
	private Label lb_filesToVerifyCount;

	@FXML
	private TextField tf_projectPath;

	@FXML
	public TreeView<File> tv_projectStructure;

	@FXML
	private ListView<File> lv_filesToVerify;
	
	@FXML
	private AnchorPane selectProjectTab;
	
	@FXML
	public Button btn_projectOpen;

	// --------------- 멤버 메소드 ---------------
	// 컨트롤러 클래스를 초기화. FXML 파일이 로드된 후 자동으로 호출됨
	@FXML
	private void initialize() {

		Main.controllerList.add(this);

		tf_projectPath.setText("프로젝트 폴더를 선택하십시오");

		tv_projectStructure.setCellFactory(treeView -> new FileTreeCell());

		lv_filesToVerify.setCellFactory(listView -> new FileListCell());
		lv_filesToVerify.setPlaceholder(new Label("검사할 항목을 선택하십시오"));
		lv_filesToVerify.setItems(Main.filesToVerify);

		lb_filesToVerifyCount.textProperty().bind(Bindings.size(Main.filesToVerify).asString());
	}

	// 찾아보기 버튼의 핸들러
	// 1. 폴더 탐색 다이얼로그를 오픈
	// 2. 읽은 내용을 트리 뷰에 세팅
	@FXML
	private void handleBrowse() {
		// 1. 폴더 탐색 다이얼로그를 오픈하고 선택한 폴더 경로를 받아옴
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("검사할 프로젝트의 디렉토리 선택");
		projectDir = directoryChooser.showDialog(null);

		if (projectDir != null) {
			// 2. 폴더 경로를 텍스트 필드에 세팅
			tf_projectPath.setText(projectDir.getAbsolutePath());

			// 3. 읽은 내용을 트리 뷰에 세팅하고 검사할 파일을 담는 리스트 자료구조는 clear
			Main.filesToVerify.clear();
			tv_projectStructure.setRoot(new FileTreeItem(new File(projectDir.getAbsolutePath())));
		}
	}
}
