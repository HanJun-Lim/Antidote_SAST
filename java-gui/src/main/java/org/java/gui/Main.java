package org.java.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.java.gui.util.data.ViolatedFile;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.java.checks.JavaRulesDefinition;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class Main extends Application {

	// --------------- 멤버 변수 ---------------
	public static Stage primaryStage;
	private AnchorPane rootLayout;

	// 검사할 파일이 들어 있는 Observable List
	public static ObservableList<File> filesToVerify = FXCollections.observableArrayList();

	// Repository 공간
	public static RulesDefinition.Repository repos;

	// 검사한 파일과 오류 정보가 담긴 리스트
	public static ObservableList<ViolatedFile> violatedFileList = FXCollections.observableArrayList();

	public static ArrayList<Object> controllerList = new ArrayList<>();
	
	public enum controllerIndex {
		SELECT_PROJECT, SELECT_RULES, VERIFY_PROJECT, ANALYSIS_STATISTICS, MAIN_VIEW
	}


	// --------------- 멤버 메소드 ---------------
	@Override
	public void start(Stage primaryStage) throws Exception {
		// 룰들을 담고있는 클래스들을 참조하여 Repository 생성
		JavaRulesDefinition rulesDefinition = new JavaRulesDefinition();
		RulesDefinition.Context context = new RulesDefinition.Context();
		rulesDefinition.define(context);

		repos = context.repository(JavaRulesDefinition.REPOSITORY_KEY);

		{ // 디버깅용으로 쓰이는 룰들에 대한 어노테이션 정보를 확인하는 부분
			List<RulesDefinition.Rule> rulesInRepos = Main.repos.rules();
			Iterator<RulesDefinition.Rule> it = rulesInRepos.iterator();
			while (it.hasNext()) {
				RulesDefinition.Rule rule = it.next();
				System.out.println("*************** " + rule.name() + " ***************");
				System.out.println("Rule key: " + rule.key());
				System.out.println("Rule type: " + rule.type());
				System.out.println("Rule tags: " + rule.tags());
				System.out.println("Rule severity: " + rule.severity());
			}
		}

		Main.primaryStage = primaryStage;

		primaryStage.setTitle("Antidote");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/org/java/gui/icon_antidote.png")));
		initRootLayout();
	}

	// 상위 레이아웃 초기화
	public void initRootLayout() {
		try {
			// fxml 파일에서 상위 레이아웃을 가져온다.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(Main.class.getResource("MainView.fxml"));

			//controllerList.add(loader.getController());
			

			rootLayout = (AnchorPane) loader.load();

			// 상위 레이아웃을 포함하는 scene 을 보여준다.
			Scene scene = new Scene(rootLayout, 90 + 800, 600);
			primaryStage.setScene(scene);
			primaryStage.show();

			// DB 파일 생성
			createDB("Antidote_statistics.db");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	public void createDB(String dbName) {
	    String currentPath = System.getProperty("user.dir") + "/";
		if (!dbFileExistsCheck(currentPath, dbName)) { // db가 없다면 db생성하기
			System.out.println("db생성 : " + currentPath + dbName);

			String source = getClass().getResource("/org/java/gui/dbFiles/Antidote_statistics.db").getPath();

			try {
				Files.copy(new File(source).toPath(), new File(currentPath + dbName).toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public boolean dbFileExistsCheck(String currentPath, String dbName) {
		File dbFileExistsCheck = new File(currentPath + dbName);
		return dbFileExistsCheck.exists();
	}

}