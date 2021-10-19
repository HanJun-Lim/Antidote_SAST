package org.java.gui.selectrules;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.java.gui.Main;
import org.java.gui.MainViewController;
import org.java.gui.analysisstatistics.analysisStatisticsController;
import org.java.gui.selectproject.selectProjectController;
import org.java.gui.util.data.ViolatedFile;
import org.java.gui.util.data.ViolatedRule;
import org.java.gui.util.db.AntidoteDB;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.java.checks.RulesList;
import org.sonar.java.checks.verifier.ErrorInfo;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.plugins.java.api.JavaFileScanner;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener.Change;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;

public class selectRulesController {

	@FXML
	private Accordion accordionRules;

	@FXML
	private TitledPane titlePaneInputValid;

	@FXML
	private VBox vboxInputValid;

	@FXML
	private Button startButton;

	@FXML
	public WebView ruleHtmlDescription;

	@FXML
	private ListView<RuleListItem> ruleSelectionList;

	@FXML
	private ProgressBar verifyingProgressBar;

	@FXML
	private Label verifyingProgressLabel;

	@FXML
	public CheckBox checkBoxAllCheck;
	
	@FXML
	public Label choicedRules;
	
	// 선택되어있는 체크박스가 담긴 ObservableSet
	private ObservableSet<RuleListItem> selectedCheckBoxes = FXCollections.observableSet();

	// 선택되지 않은 체트박스가 담긴 ObservableSet
	// private ObservableSet<RuleListItem> unselectedCheckBoxes =
	// FXCollections.observableSet();
	
	// 리스트뷰에 들어갈 룰이름들을 선언함.
	ObservableList<RuleListItem> inputDataVerificationAndExpressionList = FXCollections.observableArrayList();
	ObservableList<RuleListItem> securityFunctionList = FXCollections.observableArrayList();
	ObservableList<RuleListItem> timeAndStateList = FXCollections.observableArrayList();
	ObservableList<RuleListItem> errorHandlingList = FXCollections.observableArrayList();
	ObservableList<RuleListItem> codeErrorList = FXCollections.observableArrayList();
	ObservableList<RuleListItem> encapsulationList = FXCollections.observableArrayList();
	ObservableList<RuleListItem> apiMisuseList = FXCollections.observableArrayList();
	//ObservableList<RuleListItem> sonarRulesList = FXCollections.observableArrayList();

	@FXML
	public void initialize() {

		Main.controllerList.add(this);

		// 룰 선택 리스트에 항목이 없을 경우 출력될 메시지 설정
		ruleSelectionList.setPlaceholder(new Label("선택된 룰이 없습니다"));

		// 선택된 체크 박스에 대한 Listener 추가
		// 체크 박스 셋에 아이템이 추가될 경우, 추가된 아이템을 룰 선택 리스트에 추가
		selectedCheckBoxes.addListener((Change<? extends RuleListItem> c) -> {
			choicedRules.setText("선택된 룰 " + "(" +selectedCheckBoxes.size() + ")");
			if (c.wasAdded()) {
				ruleSelectionList.getItems().add(c.getElementAdded());
			}
			if (c.wasRemoved()) {
				ruleSelectionList.getItems().remove(c.getElementRemoved());
			}
		});



		// 룰네임으로 자동 파싱(리스트뷰에 넣기 위해)
		List<RulesDefinition.Rule> rulesInRepos = Main.repos.rules();
		Iterator<RulesDefinition.Rule> it = rulesInRepos.iterator();
		while (it.hasNext()) {
			RulesDefinition.Rule rule = it.next();
			String parseToKey = rule.key();
			if (parseToKey.charAt(0) == 'C') { // CustomRule (행안부 룰) 이라
				if (parseToKey.charAt(1) == '1') { // 입력데이터 검증 및 표현 챕터
					inputDataVerificationAndExpressionList
							.add(new RuleListItem(rule.key(), rule.type().name(), rule.severity()));
				} else if (parseToKey.charAt(1) == '2') { // 보안기능 챕터
					securityFunctionList.add(new RuleListItem(rule.key(), rule.type().name(), rule.severity()));
				} else if (parseToKey.charAt(1) == '3') { // 시간과 상태 챕터
					timeAndStateList.add(new RuleListItem(rule.key(), rule.type().name(), rule.severity()));
				} else if (parseToKey.charAt(1) == '4') { // 에러처리 챕터
					errorHandlingList.add(new RuleListItem(rule.key(), rule.type().name(), rule.severity()));
				} else if (parseToKey.charAt(1) == '5') { // 코드오류 챕터
					codeErrorList.add(new RuleListItem(rule.key(), rule.type().name(), rule.severity()));
				} else if (parseToKey.charAt(1) == '6') { // 캡슐화 챕터
					encapsulationList.add(new RuleListItem(rule.key(), rule.type().name(), rule.severity()));
				} else if (parseToKey.charAt(1) == '7') { // API오용 챕터
					apiMisuseList.add(new RuleListItem(rule.key(), rule.type().name(), rule.severity()));
				}
			} else {
				//sonarRulesList.add(new RuleListItem(rule.key(), rule.type().name(), rule.severity()));
			}
		}

		// Accordion의 타이틀팬과 안에 들어가는 리스트뷰 생성(Task클래스로 제네릭된 ObservableList를 넘겨줌)
		titledPaneAndListViewGenerator("입력데이터 검증 및 표현", inputDataVerificationAndExpressionList);
		titledPaneAndListViewGenerator("보안기능", securityFunctionList);
		titledPaneAndListViewGenerator("시간 및 상태", timeAndStateList);
		titledPaneAndListViewGenerator("에러 처리", errorHandlingList);
		// titledPaneAndListViewGenerator("코드 오류", codeErrorList);
		titledPaneAndListViewGenerator("캡슐화", encapsulationList);
		// titledPaneAndListViewGenerator("API 오용", apiMisuseList);
		// titledPaneAndListViewGenerator("Sonar 기본 룰", sonarRulesList);

		// 전체선택 체크박스를 눌렀을때 모든 ObservableList가 선택되게하기

		checkBoxAllCheck.setOnAction(e -> {

			for (RuleListItem item : inputDataVerificationAndExpressionList) {
				item.checkbox.setSelected(((CheckBox) e.getSource()).isSelected());
			}
			for (RuleListItem item : securityFunctionList) {
				item.checkbox.setSelected(((CheckBox) e.getSource()).isSelected());
			}
			for (RuleListItem item : timeAndStateList) {
				item.checkbox.setSelected(((CheckBox) e.getSource()).isSelected());
			}
			for (RuleListItem item : errorHandlingList) {
				item.checkbox.setSelected(((CheckBox) e.getSource()).isSelected());
			}
			for (RuleListItem item : codeErrorList) {
				item.checkbox.setSelected(((CheckBox) e.getSource()).isSelected());
			}
			for (RuleListItem item : encapsulationList) {
				item.checkbox.setSelected(((CheckBox) e.getSource()).isSelected());
			}
			for (RuleListItem item : apiMisuseList) {
				item.checkbox.setSelected(((CheckBox) e.getSource()).isSelected());
			}
			//for (RuleListItem item : sonarRulesList) {
			//	item.checkbox.setSelected(((CheckBox) e.getSource()).isSelected());
			//}
		});

	}

	public void titledPaneAndListViewGenerator(String title, ObservableList<RuleListItem> obList) {

		// 리스트뷰 선언과 동시에 초기화
		ListView<RuleListItem> checklist = new ListView<>(obList);

		checklist.setItems(obList);
		checklist.setCellFactory(studentListView -> new CustomListCell());

		// 리스트뷰의 체크박스를 선택했을때의 이벤트처리
		obList.forEach(task -> task.selectedProperty().addListener((observable, wasSelected, isSelected) -> {
			if (isSelected) {

				try {
					String ruleHtml = Main.repos.rule(task.getName()).htmlDescription();

					ruleHtmlDescription.getEngine().loadContent(ruleHtml);
				} catch (Exception ex) {
					ruleHtmlDescription.getEngine().loadContent(task.getName() + " \n해당하는 룰의 정보가 없습니다.");
				}
				// unselectedCheckBoxes.remove(task);
				selectedCheckBoxes.add(task);
			} else {
				selectedCheckBoxes.remove(task);
				// unselectedCheckBoxes.add(task);
			}
		}));

		// 리스트뷰를 클릭했을때 html보여주기 (체크박스아님)
		checklist.setOnMouseClicked((MouseEvent) -> {
			try {
				String ruleHtml = Main.repos.rule(checklist.getSelectionModel().getSelectedItem().getName())
						.htmlDescription();
				ruleHtmlDescription.getEngine().loadContent(ruleHtml);
			} catch (Exception ex) {
				ruleHtmlDescription.getEngine().loadContent("해당하는 룰의 정보가 없습니다.");
			}
		});

		//룰 키값으로 정렬하기
		checklist.getItems().sort((o1,o2)->{
		    if(o1.equals(o2)) return 0;
		    if(Integer.parseInt(o1.getName(),27) > Integer.parseInt(o2.getName(),27)) 
		        return 1; 
		    else 
		        return 0;
		});
		
		// TitledPane 선언과 동시에 초기화
		TitledPane tp = new TitledPane();
		tp.setContent(checklist);
		tp.setText(title);
		accordionRules.getPanes().add(tp);
	}

	double indicatorPercentage;
	String currentPercent;

	// 검사시작 버튼 클릭 핸들러
	@FXML
	public void startButtonHandle() {

		// 검사가 시작되면 다른탭들 이동 못하게 disable 시키기, 버튼, 체크박스 disable
		int mvcIndex = Main.controllerIndex.MAIN_VIEW.ordinal();
		int spcIndex = Main.controllerIndex.SELECT_PROJECT.ordinal();
		MainViewController mvc = (MainViewController) Main.controllerList.get(mvcIndex);
		selectProjectController spc = (selectProjectController) Main.controllerList.get(spcIndex);
		mvc.setDisableTabs(true);
		spc.btn_projectOpen.setDisable(true);
		spc.tv_projectStructure.setDisable(true);
		startButton.setDisable(true);
		allCheckBoxDisable(true);
		
		// 룰이름과 룰클래스(JavaFileScanner)의 매핑되는 HashMap 생성
		HashMap<String, JavaFileScanner> ruleInst = RulesList.getJavaChecksHashMap();

		Main.violatedFileList.clear();

		// 룰 또는 검사 대상 파일의 사이즈가 0이면 에러메시지 호출 후 함수종료
		if (Main.filesToVerify.size() == 0 || selectedCheckBoxes.size() == 0) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setHeaderText("Error");
			alert.setContentText("검사할 대상 또는 룰이 존재하지 않습니다.");
			alert.showAndWait();
			return;
		}

		// 프로그래스바 처리와 다중 쓰레드 처리를 위한 Task클래스 사용
		Task<Void> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {

				// test-classes 폴더 검사 후 폴더 생성
				String path = System.getProperty("user.dir") + "/";
				File Folder = new File(path + "target/test-classes");
				if (!Folder.exists()) {
					try {
						Folder.mkdirs();
						System.out.println("폴더생성완료 " + Folder.getAbsolutePath());
					} catch (Exception ex) {
						ex.getStackTrace();
					}
				} else {
					System.out.println("이미 폴더가 존재함 " + Folder.getAbsolutePath());
				}

				int currentProgressValue = 1;
				long progressMaxValue = Main.filesToVerify.size() * selectedCheckBoxes.size();

				// --------------- 파일 순환 ---------------
				for (Iterator<File> filesItr = Main.filesToVerify.iterator(); filesItr.hasNext();) {
					File file = filesItr.next();
					String filePath = file.getAbsolutePath();

					ViolatedFile fileToVerify = new ViolatedFile();
					fileToVerify.setFilePath(filePath);

					// --------------- 룰 순환 ---------------
					for (Iterator<RuleListItem> selectedCheckBoxItr = selectedCheckBoxes.iterator(); selectedCheckBoxItr
							.hasNext();) {

						if (isCancelled()) {
							break;
						} // 만약 쓰레드가 취소되었다면

						String ruleName = selectedCheckBoxItr.next().getName();
						JavaFileScanner ruleClass = ruleInst.get(ruleName);

						// =============== 검사 시작 ===============
						List<ErrorInfo> occuredErrorList;
						occuredErrorList = JavaCheckVerifier.verify(filePath, ruleClass);
						// ======================================

						// 에러 리스트와 룰 정보가 담긴 ViolatedRule을 생성한 뒤, 파일 리스트에 추가
						ViolatedRule violatedRuleInfo = new ViolatedRule(ruleName, occuredErrorList);

						if (occuredErrorList.size() != 0) {
							// 룰 위반이 발생하였을 경우에만 추가한다.
							System.out.println("룰 위반이 발생하였으므로 리스트에 추가됨 " + ruleName);
							fileToVerify.addViolatedRule(violatedRuleInfo);
						}

						currentPercent = Integer
								.toString((int) ((double) currentProgressValue++ / (double) progressMaxValue * 100.0));
						updateMessage(currentPercent + "% "); // 프로그래스바 라벨 업데이트
						updateProgress(currentProgressValue, progressMaxValue); // 프로그래스바 업데이트


					} // --------------- 룰 순환 End ---------------
					
					System.out.println("fileToVerifyCount : "+fileToVerify.getViolationCount());
					// 취약점이 한 개라도 발견되었을 경우 ViolatedFileList에 추가함
					if (fileToVerify.getViolationCount() > 0) {
						// JavaFX UI 업데이트가 일어나므로 runLater를 이용하여 UI 업데이트 스레드가 처리하도록 함
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								// 위반 파일 목록(violatedFileList)에 파일 추가
								System.out.println("fileToVerify!!"+fileToVerify.getFilePath());
								Main.violatedFileList.add(fileToVerify);

							}
						});
					}
				} // --------------- 파일 순환 End ---------------
				
				
				// 탭과 버튼들을 활성화
				mvc.setDisableTabs(false);
				startButton.setDisable(false);
				spc.btn_projectOpen.setDisable(false);
				spc.tv_projectStructure.setDisable(false);
				allCheckBoxDisable(false);
				
				
				//모든 검사가 끝나서 통계와 DB 설정
				setStatisticsController();
				
				
				if (Folder.exists()) {
					File[] deleteFolderList = Folder.listFiles();

					for (int j = 0; j < deleteFolderList.length; j++) {
						deleteFolderList[j].delete();
					}

					if (deleteFolderList.length == 0 && Folder.isDirectory()) {
						Folder.delete();
						System.out.println("폴더삭제 완료");
					}
				}
				


				return null;
			} // call 메소드 End
		};

		verifyingProgressLabel.textProperty().bind(task.messageProperty());
		verifyingProgressBar.progressProperty().bind(task.progressProperty());

		Thread thread = new Thread(task);
		thread.start();
	}

	public void setStatisticsController() {
		
	
		
		Platform.runLater(new Runnable() {	

			@Override
			public void run() {
				//TODO 분석통계탭에 있는 모든 컨트롤러들을 여기서 설정해주자
				//현재 analysisStatistics.java에서는 setItems(ObservableList) 쓰는데
				//이제 asc로 다른 fxml의 컨트롤러를 사용할수 있으니 setItem.add 등의 함수를 써서 넣자. (lv_violatedFiles)
				
				// 분석 통계 탭의 컨트롤러 불러옴
				int ascIndex = Main.controllerIndex.ANALYSIS_STATISTICS.ordinal();
				analysisStatisticsController asc = (analysisStatisticsController) Main.controllerList.get(ascIndex);
				
				
				{ //이행률 업데이트
					double violatedFileCount = Main.violatedFileList.size();
					double filesToVerifyCount = Main.filesToVerify.size();
					double compliantFilePercentage = filesToVerifyCount - violatedFileCount;
					indicatorPercentage = compliantFilePercentage / filesToVerifyCount * 100;
					System.out.println("violatedFileCount : " + violatedFileCount);
					System.out.println("filesToVerifyClount : " + filesToVerifyCount);
					System.out.println("compliantFilePercentage : " + compliantFilePercentage);
				}

				//DB데이터 넣기
				insertDB();
				
				//asc.resetViolationAnalysis();
				// 리셋해버리면 안되네 (아마 시간이 너무빨라서 그런듯) asc.resetPage();
				
				// 분석 통계에서 룰 이행률, 룰 통계 업데이트
				
				//asc.setIndicatorPercentage(indicatorPercentage);
				asc.setTotalSafePer(indicatorPercentage);
				
				//asc.setDatabaseAndLinechart();
				asc.setLineChart();
				asc.setChoiceBox();
				
				//2. 리스트 뷰 내용 추가
				//for(Iterator<ViolatedFile> itr = Main.violatedFileList.iterator(); itr.hasNext();) {
				//	asc.lv_violatedFiles.getItems().add(itr.next());
				//}
				for (int i=0; i < Main.violatedFileList.size(); i++) {
					asc.setFlieStatistics(Main.violatedFileList.get(i));
				}
				

				// 3. 룰 통계 업데이트
				for (int i = 0; i < Main.violatedFileList.size(); i++) {
					ViolatedFile vf = Main.violatedFileList.get(i);
					asc.setRuleStatistics(vf.getViolationResult());
				}
				asc.webLaunch();

			}
			
			
		});

	}
	
	public void insertDB() {
		// DB연결
		AntidoteDB db = new AntidoteDB();
		db.dbConnect();

		// Antidote_Statistics.db의 total_statistics테이블에 값 넣기 (날짜, 총 이행률)
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy.MM.dd-HH:mm");
		Date date_time = new Date();
		String time = format1.format(date_time);

		ArrayList<Object> total_statistics_argData = new ArrayList<>();
		total_statistics_argData.add(time);
		total_statistics_argData.add((int) indicatorPercentage);
		db.setQuery("INSERT INTO total_statistics VALUES (?, ?)", total_statistics_argData);

		// Antidote_Statistics.db의 detailed_statistics테이블에 값 넣기 (날짜, 위반된파일경로, 위반되룰,
		// 발생횟수)
		String violatedFilePath, violatedRuleKey;
		int count;
		//System.out.println("Main.violatedFileList.size() : " + Main.violatedFileList.size());
		for (int i = 0; i < Main.violatedFileList.size(); i++) {
			//System.out.println("i : " + i);

			ViolatedFile vf = Main.violatedFileList.get(i);
			violatedFilePath = vf.getFilePath();
			
			//System.out.println("vf.getViolationCount() : " + vf.getViolationCount());
			for (int j = 0; j < vf.getViolationCount(); j++) {
				//System.out.println("j : " + j);
				ViolatedRule vr = vf.getViolatedRuleList().get(j);
				violatedRuleKey = vr.getRuleKey();
				count = vr.getErrorOccurenceCount();
				
				ArrayList<Object> detailed_statistics_argData = new ArrayList<>();
				detailed_statistics_argData.add(time);
				detailed_statistics_argData.add(violatedFilePath);
				detailed_statistics_argData.add(violatedRuleKey);
				detailed_statistics_argData.add(count);
				db.setQuery("INSERT INTO detailed_statistics VALUES (?, ?, ?, ?)", detailed_statistics_argData);
			}
			
		}
		//db.close();
		// db select 테스트
		//ArrayList<Object[]> dbTest = db.getSearchTotalStatistics();
		//System.out.println(((String) dbTest.get(0)[0]));
		
	}
	
	public void allCheckBoxDisable(boolean flag) {
		checkBoxAllCheck.setDisable(flag);
		
		for (RuleListItem item : inputDataVerificationAndExpressionList) {
			item.checkbox.setDisable(flag);
		}
		for (RuleListItem item : securityFunctionList) {
			item.checkbox.setDisable(flag);
		}
		for (RuleListItem item : timeAndStateList) {
			item.checkbox.setDisable(flag);
		}
		for (RuleListItem item : errorHandlingList) {
			item.checkbox.setDisable(flag);
		}
		for (RuleListItem item : codeErrorList) {
			item.checkbox.setDisable(flag);
		}
		for (RuleListItem item : encapsulationList) {
			item.checkbox.setDisable(flag);
		}
		for (RuleListItem item : apiMisuseList) {
			item.checkbox.setDisable(flag);
		}
		
	}

	// ObservalbeList에 제네릭시킬 클래스 (내부클래스임)
	// 나중에 다른 클래스에서 쓰이게 될거라면 해당 클래스를 밖으로(파일로) 빼기
	public class RuleListItem {

		private ReadOnlyStringWrapper name = new ReadOnlyStringWrapper();
		private BooleanProperty selected = new SimpleBooleanProperty(false);
		private ReadOnlyStringWrapper type = new ReadOnlyStringWrapper();
		private ReadOnlyStringWrapper severity = new ReadOnlyStringWrapper();
		private CheckBox checkbox;

		@Override
		public String toString() {
			return Main.repos.rule(name.get()).name();
		}

		public RuleListItem(String name, String type, String severity) {
			this.name.set(name);
			
			if(type.contains("CODE_SMELL")) {
				this.type.set("종류: 코드 스멜");
			}else if(type.contains("BUG")) {
				this.type.set("종류: 버그");
			}else if(type.contains("VULNERABILITY")) {
				this.type.set("종류: 취약점");
			}else if(type.contains("SECURITY_HOTSPOT")) {
				this.type.set("종류: 보안 핫스팟");
			}else {
				this.type.set("종류: 알수없음");
			}
			
			if(severity.contains("INFO")) {
				this.severity.set("심각도: 유의");
			}else if(severity.contains("MINOR")) {
				this.severity.set("심각도: 낮음");
			}else if(severity.contains("MAJOR")) {
				this.severity.set("심각도: 중간");
			}else if(severity.contains("CRITICAL")) {
				this.severity.set("심각도: 높음");
			}else if(severity.contains("BLOCKER")) {
				this.severity.set("심각도: 매우 심각");
			}else {
				this.severity.set("심각도: 알수없음");
			}
			
			checkbox = new CheckBox(Main.repos.rule(this.name.get()).name());
			selectedProperty().bind(checkbox.selectedProperty());

		}

		public String getName() {
			return name.get();
		}

		public String getType() {
			return type.get();
		}

		public String getSeverity() {
			return severity.get();
		}

		public ReadOnlyStringProperty nameProperty() {
			return name.getReadOnlyProperty();
		}

		public BooleanProperty selectedProperty() {
			return selected;
		}

		public boolean isSelected() {
			return selected.get();
		}

		public void setSelected(boolean selected) {
			this.selected.set(selected);
		}

	}

	private class CustomListCell extends ListCell<RuleListItem> {
		private VBox root;

		public CustomListCell() {
			super();

		}

		@Override
		public void updateItem(RuleListItem item, boolean empty) {

			super.updateItem(item, empty);

			if (item != null && !empty) {

				item.checkbox.setStyle("-fx-font-weight: bold;");

				Label labelType = new Label(item.getType());
				labelType.setMaxWidth(Double.MAX_VALUE);
				AnchorPane.setLeftAnchor(labelType, 0.0);
				AnchorPane.setRightAnchor(labelType, 0.0);
				labelType.setAlignment(Pos.CENTER_RIGHT);
				labelType.setStyle("-fx-font-style: italic;");

				Label labelSeverity = new Label(item.getSeverity());
				labelSeverity.setMaxWidth(Double.MAX_VALUE);
				AnchorPane.setLeftAnchor(labelSeverity, 0.0);
				AnchorPane.setRightAnchor(labelSeverity, 0.0);
				labelSeverity.setAlignment(Pos.CENTER_RIGHT);
				labelSeverity.setStyle("-fx-font-style: italic;");

				root = new VBox();
				root.setPadding(new Insets(1));
				root.getChildren().addAll(item.checkbox, labelType, labelSeverity);

				setGraphic(root);

			} else {
				setGraphic(null);
			}
		}

	}

}
