package org.java.gui;

import java.net.URL;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.TextAlignment;

public class MainViewController {

	// --------------- 전체 탭에 대한 정보 ---------------
	@FXML
	private TabPane tabContainer;

	// --------------- 탭 1 : 프로젝트 선택 ---------------
	@FXML
	private Tab selectProjectTab;
	
	// --------------- 탭 2 : 룰 선택 ---------------
	@FXML
	private Tab selectRulesTab;

	// --------------- 탭 3 : 분석 결과 ---------------
	@FXML
	private Tab verifyProjectTab;

	// --------------- 탭 4 : 분석 통계 ---------------
	@FXML
	private Tab analysisStatisticsTab;
	
	
	private double tabWidth = 90.0;

	@FXML
	public void initialize() {
		Main.controllerList.add(this);
		configureView();		
	}

	private void configureView() {
		tabContainer.setTabMinWidth(tabWidth);
		tabContainer.setTabMaxWidth(tabWidth);
		tabContainer.setTabMinHeight(tabWidth);
		tabContainer.setTabMaxHeight(tabWidth);
		// tabContainer.setRotateGraphic(true);

		configureTab(selectProjectTab, "프로젝트 선택", "/org/java/gui/icon_project.png",
				getClass().getResource("selectproject/selectProjectContainer.fxml"));

		configureTab(selectRulesTab, "룰 선택", "/org/java/gui/icon_rule.png",
				getClass().getResource("selectrules/selectRulesContainer.fxml"));

		configureTab(verifyProjectTab, "분석 결과", "/org/java/gui/icon_verify.png",
				getClass().getResource("verifyproject/verifyProjectContainer.fxml"));

		configureTab(analysisStatisticsTab, "분석 통계", "/org/java/gui/icon_statistics.png",
				getClass().getResource("analysisstatistics/analysisStatisticsContainer.fxml"));
	}

	private void configureTab(Tab tab, String title, String iconPath, URL resourceURL) {
		double imageWidth = 40.0;

		ImageView imageView = new ImageView(new Image(iconPath));
		imageView.setFitHeight(imageWidth);
		imageView.setFitWidth(imageWidth);

		Label label = new Label(title);
		label.setMaxWidth(tabWidth - 20);
		label.setPadding(new Insets(5, 0, 0, 0));
		label.setStyle("-fx-text-fill: black; -fx-font-size: 8pt; -fx-font-weight: normal;");
		label.setTextAlignment(TextAlignment.CENTER);

		BorderPane tabDesign = new BorderPane();
		tabDesign.setMaxWidth(90.0);
		tabDesign.setCenter(imageView);
		tabDesign.setBottom(label);
		tabDesign.setRotate(tabWidth);

		tab.setText("");
		tab.setGraphic(tabDesign);

	}
	
	//분석통계탭 비활성화
	public void setDisableTabs(boolean flag) {
		
		if(flag==true) {
			verifyProjectTab.setDisable(true);
			analysisStatisticsTab.setDisable(true);
		}else {
			verifyProjectTab.setDisable(false);
			analysisStatisticsTab.setDisable(false);
		}
		
	}

}