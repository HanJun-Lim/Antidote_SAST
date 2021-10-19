package org.java.gui.analysisstatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.java.gui.Main;
import org.java.gui.util.data.RingProgressIndicator;
import org.java.gui.util.data.ToolTipDefaultsFixer;
import org.java.gui.util.data.ViolatedFile;
import org.java.gui.util.data.ViolatedRule;
import org.java.gui.util.db.AntidoteDB;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

public class analysisStatisticsController {

	// --------------- 멤버 변수 ---------------
	@FXML
	private AnchorPane analysis_tab;

	@FXML
	public WebView webView_statistics;
	
	@FXML
	public ChoiceBox<String> choiceBox_date;

	// 초이스박스에 있는 이행률목록을 저장할 배열
	int[] ia_compliantPer = new int[30];

	// 이행률 프로그래스바
	//private RingProgressIndicator compliantPercentage = new RingProgressIndicator();

	private Map<String, Integer> allViolatedRules = new HashMap<>();
	private Map<String, Integer> allSeverityRules = new HashMap<>();
	Map<String, Integer> total_st_map = new HashMap<String, Integer>(); // 날짜와 이행률이 매핑된 변수(해시맵) <date_time, percent>

	// ------------리스너 목록-----------------
	//private lv_Listener listener;
	//private lv_resetListener resetlistener;
	private choiceBox_listener cb_listener;

	String choicedDate; // 초이스박스에서 클릭된 날짜를 저장할 전역변수

	// --------------- 멤버 메소드 ---------------
	// 컨트롤러 클래스를 초기화. FXML 파일이 로드된 후 자동으로 호출됨
	@FXML
	private void initialize() {
		
		//팝업핸들러 추가
		 webView_statistics.getEngine().setCreatePopupHandler(new Callback<PopupFeatures, WebEngine>() {
		        @Override
		        public WebEngine call(PopupFeatures p) {
		            Stage stage = new Stage(StageStyle.UTILITY);
		            WebView wv2 = new WebView();
		            stage.setScene(new Scene(wv2));
		            stage.show();
		            return wv2.getEngine();
		        }
		    });

		Main.controllerList.add(this);
		
		resetPage();
	}
	@FXML
	private Button testButton;
	@FXML
	public void testButtonClick() {
		//webView_statistics.getEngine().load("https://startbootstrap.com/previews/sb-admin-2");
		
		setTotalSafePer(0);
		setLineChart();
		setChoiceBox();
		
		//webView_statistics.getEngine().executeScript(" document.getElementById('ruleCategory').innerHTML=\"" + allRuletag + "\" ");
		webView_statistics.getEngine().executeScript(tableTag_script);
		webView_statistics.getEngine().executeScript(pie_script);
		testButton.setVisible(false);
	}
	public void webLaunch() {
		webView_statistics.getEngine().executeScript(tableTag_script);
		webView_statistics.getEngine().executeScript(pie_script);
	}
	
	public void resetPage() {
		tableTag_script ="";
		tableTag = "";
		pie_script="";
		
		this.allSeverityRules.clear();
		this.allViolatedRules.clear();
		String url = getClass().getResource("/org/java/gui/statistics_page/index.html").toExternalForm();
		webView_statistics.getEngine().load(url);
	}
	
	public void setTotalSafePer(double per) {
		//setDatabaseAndLinechart
		String per_s = String.valueOf((int)per);
	      webView_statistics.getEngine().executeScript(" document.getElementById('total_per').innerText='" + per_s + "%' ");
	}
	
	public void setChoiceBox() {
		//초이스박스 셋팅
		choiceBox_date.getItems().clear();
		if(cb_listener!=null)
			choiceBox_date.getSelectionModel().selectedItemProperty().removeListener(cb_listener);
		
		AntidoteDB db = new AntidoteDB();
		db.dbConnect();
		ArrayList<Object[]> total_st = db.getSearchTotalStatistics();
		//ArrayList<String> days = new ArrayList<>();

		for (Object[] obArrData : total_st) {
			
			choiceBox_date.getItems().add((String)obArrData[0]);
		}
		
		cb_listener = new choiceBox_listener();

		choiceBox_date.getSelectionModel().selectedItemProperty().addListener(cb_listener);
		
	}
	
	Map<String, Integer> total_map = new HashMap<>();
	public void setLineChart() {
		
		
		// DB연결 후 total_statistics 테이블 가져오기 (최근 30개)
		AntidoteDB db = new AntidoteDB();
		db.dbConnect();
		ArrayList<Object[]> total_st = db.getSearchTotalStatistics();
		
		//ArrayList<String> days = new ArrayList<>();
		String days = "";
		String pers = "";
		for (Object[] obArrData : total_st) {
			//days.add((String)obArrData[0]);
			total_map.put((String)obArrData[0], (int)obArrData[1]);
			days += "\"" + (String)obArrData[0] + "\", ";
			pers += (int)obArrData[1] + ", ";
			
		}
		
		
				
		String linechart_script = "var ctx = document.getElementById(\"myAreaChart\");\n" + 
				"var myLineChart = new Chart(ctx, {\n" + 
				"  type: 'line',\n" + 
				"  data: {\n" + 
				"    labels: [" + days + "],\n" + 
				"    datasets: [{\n" + 
				"      label: \"Earnings\",\n" + 
				"      lineTension: 0.3,\n" + 
				"      backgroundColor: \"rgba(78, 115, 223, 0.05)\",\n" + 
				"      borderColor: \"rgba(78, 115, 223, 1)\",\n" + 
				"      pointRadius: 3,\n" + 
				"      pointBackgroundColor: \"rgba(78, 115, 223, 1)\",\n" + 
				"      pointBorderColor: \"rgba(78, 115, 223, 1)\",\n" + 
				"      pointHoverRadius: 3,\n" + 
				"      pointHoverBackgroundColor: \"rgba(78, 115, 223, 1)\",\n" + 
				"      pointHoverBorderColor: \"rgba(78, 115, 223, 1)\",\n" + 
				"      pointHitRadius: 10,\n" + 
				"      pointBorderWidth: 2,\n" + 
				"      data: [" + pers + "],\n" + 
				"    }],\n" + 
				"  },\n" + 
				"  options: {\n" + 
				"    maintainAspectRatio: false,\n" + 
				"    layout: {\n" + 
				"      padding: {\n" + 
				"        left: 10,\n" + 
				"        right: 25,\n" + 
				"        top: 25,\n" + 
				"        bottom: 0\n" + 
				"      }\n" + 
				"    },\n" + 
				"    scales: {\n" + 
				"      xAxes: [{\n" + 
				"        time: {\n" + 
				"          unit: 'date'\n" + 
				"        },\n" + 
				"        gridLines: {\n" + 
				"          display: false,\n" + 
				"          drawBorder: false\n" + 
				"        },\n" + 
				"        ticks: {\n" + 
				"          maxTicksLimit: 30\n" + 
				"        }\n" + 
				"      }],\n" + 
				"      yAxes: [{\n" + 
				"        ticks: {\n" + 
				"          maxTicksLimit: 10,\n" + 
				"          padding: 10,\n" + 
				"          // Include a dollar sign in the ticks\n" + 
				"          callback: function(value, index, values) {\n" + 
				"            return number_format(value);\n" + 
				"          }\n" + 
				"        },\n" + 
				"        gridLines: {\n" + 
				"          color: \"rgb(234, 236, 244)\",\n" + 
				"          zeroLineColor: \"rgb(234, 236, 244)\",\n" + 
				"          drawBorder: false,\n" + 
				"          borderDash: [2],\n" + 
				"          zeroLineBorderDash: [2]\n" + 
				"        }\n" + 
				"      }],\n" + 
				"    },\n" + 
				"    legend: {\n" + 
				"      display: false\n" + 
				"    },\n" + 
				"    tooltips: {\n" + 
				"      backgroundColor: \"rgb(255,255,255)\",\n" + 
				"      bodyFontColor: \"#858796\",\n" + 
				"      titleMarginBottom: 10,\n" + 
				"      titleFontColor: '#6e707e',\n" + 
				"      titleFontSize: 14,\n" + 
				"      borderColor: '#dddfeb',\n" + 
				"      borderWidth: 1,\n" + 
				"      xPadding: 15,\n" + 
				"      yPadding: 15,\n" + 
				"      displayColors: false,\n" + 
				"      intersect: false,\n" + 
				"      mode: 'index',\n" + 
				"      caretPadding: 10,\n" + 
				"      callbacks: {\n" + 
				"        label: function(tooltipItem, chart) {\n" + 
				"          return '안전도: ' + number_format(tooltipItem.yLabel) + '%';\n" + 
				"        }\n" + 
				"      }\n" + 
				"    }\n" + 
				"  }\n" + 
				"});\n";
		webView_statistics.getEngine().executeScript(linechart_script);
	
		//맨위에 날짜 보여주는곳
		Object[] tmpOb = total_st.get(0);
		String settingDate = (String)tmpOb[0];
		webView_statistics.getEngine().executeScript("document.getElementById('select_date').innerText='"+settingDate+"'");
	}

	static String tableTag_script;
	static String tableTag =" ";
	static String pie_script;	
	public void setFlieStatistics(ViolatedFile vf) {
		String fileName = vf.getFilePath();
		
		for(int i=0; i<vf.getViolatedRuleList().size();i++) {
			String ruleKey = vf.getViolatedRuleList().get(i).getRuleKey();
			String ruleName = Main.repos.rule(ruleKey).name();
			String ruleSeverity = Main.repos.rule(ruleKey).severity();
		      if(ruleSeverity.equals("CRITICAL")) {
			         ruleSeverity="HIGH";
			      } else if (ruleSeverity.equals("MAJOR")) {
			         ruleSeverity="MEDIUM";
			      } else if (ruleSeverity.equals("MINOR")) {
			         ruleSeverity="LOW";
			      }
				String ruleKind = Main.repos.rule(ruleKey).type().name();
				if(ruleKind.contains("CODE_SMELL")) {
					ruleKind=("코드 스멜");
				}else if(ruleKind.contains("BUG")) {
					ruleKind=("버그");
				}else if(ruleKind.contains("VULNERABILITY")) {
					ruleKind=("취약점");
				}else if(ruleKind.contains("SECURITY_HOTSPOT")) {
					ruleKind=("보안 핫스팟");
				}else {
					ruleKind=("알수없음");
				}
			String ruleHtml = Main.repos.rule(ruleKey).htmlDescription();
			int ruleCount = vf.getViolatedRuleList().get(i).getErrorOccurenceCount();
			tableTag += "<tr>"+
					"<td>"+fileName+"</td>"+
					"<td>"+"<a href='http://antidote.dothome.co.kr/"+ruleKey+"_java.html' target=_blank>"+ruleName+"</a></td>"+
					"<td>"+ruleSeverity+"</td>"+
					"<td>"+ruleKind+"</td>"+
					"<td>"+ruleCount+"</td>"+
					"</tr>";
		}
		tableTag_script =" document.getElementById('table_body').innerHTML=\"" + tableTag + " \";";
	}
	
	public void setFlieStatistics_tmp(ViolatedFile vf) {
		String fileName = vf.getFilePath();
		for(int i=0; i<vf.getTmpRuleKeys().size();i++) {
			String ruleKey = vf.getTmpRuleKeys().get(i);
			String ruleName = Main.repos.rule(ruleKey).name();
			String ruleSeverity = Main.repos.rule(ruleKey).severity();
		      if(ruleSeverity.equals("CRITICAL")) {
			         ruleSeverity="HIGH";
			      } else if (ruleSeverity.equals("MAJOR")) {
			         ruleSeverity="MEDIUM";
			      } else if (ruleSeverity.equals("MINOR")) {
			         ruleSeverity="LOW";
			      }
			String ruleKind = Main.repos.rule(ruleKey).type().name();
				if(ruleKind.contains("CODE_SMELL")) {
					ruleKind=("코드 스멜");
				}else if(ruleKind.contains("BUG")) {
					ruleKind=("버그");
				}else if(ruleKind.contains("VULNERABILITY")) {
					ruleKind=("취약점");
				}else if(ruleKind.contains("SECURITY_HOTSPOT")) {
					ruleKind=("보안 핫스팟");
				}else {
					ruleKind=("알수없음");
				}
			String ruleHtml = Main.repos.rule(ruleKey).htmlDescription();

			int ruleCount = vf.getTmpErrorCountList().get(i);
			tableTag += "<tr>"+
					"<td>"+fileName+"</td>"+
					"<td>"+"<a href='http://antidote.dothome.co.kr/"+ruleKey+"_java.html' target=_blank>"+ruleName+"</a></td>"+
					"<td>"+ruleSeverity+"</td>"+
					"<td>"+ruleKind+"</td>"+
					"<td>"+ruleCount+"</td>"+
					"</tr>";
		}
		tableTag_script =" document.getElementById('table_body').innerHTML=\"" + tableTag + " \";";
	}

	
	public void setRuleStatistics(Map<String, Integer> analysisData) {


		// 위반 룰 통계 데이터를 담는 자료구조(allViolatedRules) 업데이트
		for (String key : analysisData.keySet()) {
			// 중복되는 경우, 발생 빈도를 업데이트
			if (allViolatedRules.containsKey(key)) {
				int sum = allViolatedRules.get(key) + analysisData.get(key);
				allViolatedRules.put(key, sum);
			}
			// 중복되지 않는 경우, 새 항목 추가
			else {
				allViolatedRules.put(key, analysisData.get(key));
			}
		}
		/*
		String tableTag="";
		for(String key : allViolatedRules.keySet()) {
			String ruleName = Main.repos.rule(key).name();
			String severity = Main.repos.rule(key).severity();
			tableTag += "<tr>"+
							"<td>"+fileNameList.get(fileNameIndex)+"</td>"+
							"<td>"+ruleName+"</td>"+
							"<td>"+severity+"</td>"+
							"<td>"+allViolatedRules.get(key)+"</td>"+
							"</tr>";
		}
		tableTag_script =" document.getElementById('table_body').innerHTML=\"" + tableTag + " \" ";
		//allRuletag_script=" document.getElementById('ruleCategory').innerHTML=\"" + allRuletag + " \" ";
		//webView_statistics.getEngine().executeScript(" document.getElementById('ruleCategory').innerHTML=\"" + allRuletag + " \" ");##################3
*/
		
		//심각도 통계 데이터를 담는 자료구조 업데이트
		for (String key : analysisData.keySet()) {
			
			String ruleSeverity = Main.repos.rule(key).severity();

			// 중복되는 경우, 발생 빈도를 업데이트
			if (allSeverityRules.containsKey(ruleSeverity)) {
				int sum = allSeverityRules.get(ruleSeverity) + analysisData.get(key);
				allSeverityRules.put(ruleSeverity, sum);
			}
			// 중복되지 않는 경우, 새 항목 추가
			else {
				allSeverityRules.put(ruleSeverity, analysisData.get(key));
			}
		}
		pie_script = "var ctx = document.getElementById(\"myPieChart\");\n" + 
				"var myPieChart = new Chart(ctx, {\n" + 
				"  type: 'doughnut',\n" + 
				"  data: {\n" + 
				"    labels: [\"심각도(높음)\", \"심각도(중간)\", \"심각도(낮음)\"],\n" + 
				"    datasets: [{\n" + 
				"      data: ["+allSeverityRules.get("CRITICAL") +", "+ allSeverityRules.get("MAJOR")+", "+ allSeverityRules.get("MINOR")+"],\n" + 
				"      backgroundColor: ['#4e73df', '#1cc88a', '#36b9cc'],\n" + 
				"      hoverBackgroundColor: ['#2e59d9', '#17a673', '#2c9faf'],\n" + 
				"      hoverBorderColor: \"rgba(234, 236, 244, 1)\",\n" + 
				"    }],\n" + 
				"  },\n" + 
				"  options: {\n" + 
				"    maintainAspectRatio: false,\n" + 
				"    tooltips: {\n" + 
				"      backgroundColor: \"rgb(255,255,255)\",\n" + 
				"      bodyFontColor: \"#858796\",\n" + 
				"      borderColor: '#dddfeb',\n" + 
				"      borderWidth: 1,\n" + 
				"      xPadding: 15,\n" + 
				"      yPadding: 15,\n" + 
				"      displayColors: false,\n" + 
				"      caretPadding: 10,\n" + 
				"    },\n" + 
				"    legend: {\n" + 
				"      display: false\n" + 
				"    },\n" + 
				"    cutoutPercentage: 80,\n" + 
				"  },\n" + 
				"});";
		//webView_statistics.getEngine().executeScript(pie_script);#####################################
		
		int critical=0;
		int major=0;
		int minor=0;
		try {critical = allSeverityRules.get("CRITICAL");}catch(NullPointerException ex) { critical=0;}
		try {major = allSeverityRules.get("MAJOR");}catch(NullPointerException ex) { major=0;}
		try {minor = allSeverityRules.get("MINOR");}catch(NullPointerException ex) { minor=0;}
		
		webView_statistics.getEngine().executeScript(" document.getElementById('high_count').innerText='" + critical + "'");
		webView_statistics.getEngine().executeScript(" document.getElementById('normal_count').innerText='" + major + "'");
		webView_statistics.getEngine().executeScript(" document.getElementById('low_count').innerText='" + minor + "'");
		
	}
	
	class choiceBox_listener implements ChangeListener<String>{

		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {

			//resetPage();
			tableTag="";
			allSeverityRules.clear();
			allViolatedRules.clear();
			webView_statistics.getEngine().executeScript(" document.getElementById('table_body').innerHTML=\"\";");
						
			//맨위에 날짜 보여주는곳
			AntidoteDB db = new AntidoteDB();
			db.dbConnect();

			ArrayList<Object[]> detailed_st = db.getSearchDetailedStatistics(newValue);
			Object[] tmpOb = detailed_st.get(0);
			String settingDate = (String)tmpOb[0];
			
			setTotalSafePer(total_map.get(settingDate));
			
			ArrayList<ViolatedFile> vfList = new ArrayList<>();
			
			
			for (Object[] obArrData : detailed_st) {
				boolean tmpFlag = false;

				String date = (String) obArrData[0];
				String filePath = (String) obArrData[1];
				String ruleKey = (String) obArrData[2];
				int count = (int) obArrData[3];


				for (int i = 0; i < vfList.size(); i++) {
					if (vfList.get(i).getFilePath().contains(filePath)) {
						tmpFlag = true;
						vfList.get(i).addTmpRule(ruleKey, count);
					}
				}

				if (tmpFlag == true) {
					continue;
				}

				vfList.add(new ViolatedFile(filePath, ruleKey, count));

			}
			
			
			setLineChart();

			
			for (int i=0; i < vfList.size(); i++) {
				setFlieStatistics_tmp(vfList.get(i));
			}
			
			for (int i = 0; i < vfList.size(); i++) {
				ViolatedFile vf = vfList.get(i);
				setRuleStatistics(vf.getTmpViolationResult());
			}
			
			
			//맨위 날짜라벨
			webView_statistics.getEngine().executeScript("document.getElementById('select_date').innerText='"+settingDate+"'");

			webView_statistics.getEngine().executeScript(tableTag_script);
			webView_statistics.getEngine().executeScript(pie_script);
			
			//TODO
			//날짜 셋팅하고
			//초이스박스 눌렀을 때 취해야 하는 행동 설정하기
		}
		
	}
}