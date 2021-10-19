package org.java.gui.verifyproject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import org.java.gui.Main;
import org.java.gui.selectrules.selectRulesController;
import org.java.gui.util.data.ViolatedFile;
import org.java.gui.util.data.ViolatedRule;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.java.checks.verifier.ErrorInfo;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.web.WebView;
import javafx.util.Callback;

public class verifyProjectController {

   @FXML
   public WebView codeViewer;

   @FXML
   private ListView<ViolatedFile> lv_violatedFiles;

   @FXML
   private ListView<ViolatedRule> lv_violatedRules;

   @FXML
   private Label lb_violatedFilesCount;

   @FXML
   private Label lb_violatedRulesCount;

   private String viewerTemplate = "", code = "";

   @FXML
   private void initialize() {
		Main.controllerList.add(this);

      // --------------- lv_violatedFiles 초기화 ---------------

      // violatedFiles 리스트 뷰는 Main의 violatedFileList 를 Observe 하도록 한다
      // 각 항목에 대해 Listener 추가
      lv_violatedFiles.setPlaceholder(new Label("취약한 파일이 발견되지 않았습니다"));
      lv_violatedFiles.setItems(Main.violatedFileList);
      
      lv_violatedFiles.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ViolatedFile>() {

         @Override
         public void changed(ObservableValue<? extends ViolatedFile> observable, ViolatedFile oldValue,
               ViolatedFile newValue) {
            if (newValue == null) {
               lv_violatedRules.setItems(null);
               lb_violatedRulesCount.setText("0");
               return;
            }

            InputStream is = getClass().getResourceAsStream("/org/java/gui/codeviewer/codeviewer.html");
            viewerTemplate = "";
            code = "";
            // codeviewer.html 불러오기
            try {
               BufferedReader br = new BufferedReader(new InputStreamReader(is));
               String readLine = null;
               while ((readLine = br.readLine()) != null) {
            	   viewerTemplate = viewerTemplate.concat(readLine+"\n");
               }
               br.close();
            } catch (Exception ex) {
               ex.printStackTrace();
            }

            // 가져온 아이템에서 리스트를 가져옴 - 메소드 이용
            ObservableList<ViolatedRule> vrList = FXCollections.observableArrayList(newValue.getViolatedRuleList());

            // lv_violatedRules 리스트 뷰에 가져온 리스트를 setItem
            lv_violatedRules.setItems(vrList);
            int vrListSize = lv_violatedRules.itemsProperty().get().size();
            lb_violatedRulesCount.setText(Integer.toString(vrListSize));

            
            // 파일을 읽어와서 웹 뷰어에 세팅
            try {
               FileReader rw = new FileReader(newValue.getFilePath());
               BufferedReader br = new BufferedReader(rw);
               String readLine = null;
               while ((readLine = br.readLine()) != null) {
                  code += readLine + "\n";
               }
               rw.close();
               br.close();
            } catch (Exception ex) {
               ex.printStackTrace();
            }
            setCodeViewerCode(code);
            
            
            codeViewer.getEngine().loadContent(viewerTemplate);
            //코드뷰어에서 하이라이트된 라인을 담을 배열 선언
            
         }
      });
      // --------------------------------------------------

      // --------------- lv_violatedRules 초기화 ---------------
      lv_violatedRules.setPlaceholder(new Label("취약한 파일을 선택하십시오"));
      lv_violatedRules.setCellFactory(new Callback<ListView<ViolatedRule>, ListCell<ViolatedRule>>() {

         @Override
         public ListCell<ViolatedRule> call(ListView<ViolatedRule> param) {
            ListCell<ViolatedRule> cell = new ListCell<ViolatedRule>() {
               @Override
               protected void updateItem(ViolatedRule item, boolean empty) {
                  super.updateItem(item, empty);
                  if (item != null) {
                     setText(Main.repos.rule(item.getRuleKey()).name());
                  } else {
                     setText(null);
                  }
               }
            };
            return cell;
         }
      });

      // violatedRules 리스트 뷰는 내부 항목을 클릭할 시 취약점이 탐지된 부분을 하이라이팅 하도록 한다
      // 각 항목에 대해 Listener 추가
      lv_violatedRules.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ViolatedRule>() {

         @Override
         public void changed(ObservableValue<? extends ViolatedRule> observable, ViolatedRule oldValue,
               ViolatedRule newValue) {
            if (newValue == null) {
               return;
            }
            
            //코드뷰어에서 하이라이트 된 텍스트 다시 초기화
            codeViewer.getEngine().executeScript("markers.forEach(marker => marker.clear());");
            codeViewer.getEngine().executeScript("linemarkers.forEach(linemarker => linemarker.clear());");
            
            // 가져온 아이템에서 ErrorInfo 리스트를 가져옴 - 메소드 이용
            ObservableList<ErrorInfo> eilist = FXCollections.observableArrayList(newValue.getErrorInfoList());
            
            String severity;
            List<RulesDefinition.Rule> rulesInRepos = Main.repos.rules();
    		Iterator<RulesDefinition.Rule> it = rulesInRepos.iterator();
    		while (it.hasNext()) {
    			RulesDefinition.Rule rule = it.next();
    			if(rule.key().contains(newValue.getRuleKey())) {
    				severity=rule.severity();
    				
    	            // 코드 하이라이팅
    	            for (Iterator<ErrorInfo> itr = eilist.iterator(); itr.hasNext();) {
    	               ErrorInfo ei = itr.next();
    	               
    	               int startLine = ei.getStartLine();
    	               int startCharacter = ei.getStartCharacter();
    	               int endLine = ei.getEndLine();
    	               int endCharacter = ei.getEndCharacter();
    	               String msg = ei.getErrorMessage();
    	               // for debug
    	               System.out.println(
    	                     "에러 정보 : " + startLine + ' ' + startCharacter + ' ' + endLine + ' ' + endCharacter + ' ');
    	               setCodeViewerMarkText(startLine, startCharacter, endLine, endCharacter, "lightpink");
    	               setCodeViewerMessageLine(endLine, msg, severity);
    	            }
    				
    				break;
    			}
    			
    		}
            

         }
      });
      // --------------------------------------------------

      lb_violatedFilesCount.textProperty().bind(Bindings.size(Main.violatedFileList).asString());
      
      
   }

   private void setCodeViewerCode(String code) {
      viewerTemplate = viewerTemplate.replace("${CODE}", code);
      viewerTemplate = viewerTemplate.replace("${HEIGHT}", Double.toString(codeViewer.getHeight() - 25));
      
   }

   //Object beforeMarkText;
   private void setCodeViewerMarkText(int startLine, int startPoint, int endLine, int endPoint,
         String color) {
      String start = "{line: " + Integer.toString(startLine - 1) + ", ch: " + Integer.toString(startPoint) + "}";
      String end = "{line: " + Integer.toString(endLine - 1) + ", ch: " + Integer.toString(endPoint) + "}";
      String colorsheet = "{css: \"background-color: " + color + "\"}";

      // for debug
      System.out.println("editor.markText(" + start + "," + end + "," + colorsheet + ")");
      codeViewer.getEngine().executeScript("markers.push(editor.markText(" + start + "," + end + "," + colorsheet + "));");
   }
   
   private void setCodeViewerMessageLine(int line, String msg, String severity) {
	      
	      //TODO 리스트박스를 눌렀을때 보여질 것인가 한방에 보여질 것인가 회의하기
	      //TODO 주석색깔이랑 메시지 색깔이 같은거 바꾸기
	      String errorIcon;
	     
	      if(severity.equals("CRITICAL")) {
	         errorIcon = "lint-error-icon-red";
	         severity="HIGH";
	      } else if (severity.equals("MAJOR")) {
	         errorIcon = "lint-error-icon-orange";
	         severity="MEDIUM";
	      } else if (severity.equals("MINOR")) {
	         errorIcon = "lint-error-icon-green";
	         severity="LOW";
	      } else if (severity.equals("INFO")) {
	         errorIcon = "lint-error-icon-blue";
	      } else {
	         errorIcon = "lint-error-icon-red";
	      }
	      
	      
	      codeViewer.getEngine().executeScript("var msg = document.createElement(\"div\");" + 
	            "var icon = msg.appendChild(document.createElement(\"span\"));" + 
	            "icon.innerHTML = \""+severity+"\";" + 
	            "icon.className = \""+errorIcon+"\";" + 
	            "msg.appendChild(document.createTextNode(\""+msg+"\"));" + 
	            "msg.className = \"lint-error\";" + 
	            "linemarkers.push(editor.addLineWidget("+(line-1)+", msg, {coverGutter: false, noHScroll: true}));");
	            //"editor.addLineWidget("+(line-1)+", msg, {coverGutter: false, noHScroll: true});");
	      
	   }

}