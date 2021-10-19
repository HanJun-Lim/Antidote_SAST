package org.java.gui.util.data;

import java.util.Collections;
import java.util.List;

import org.sonar.java.checks.verifier.ErrorInfo;


public class ViolatedRule {
   private String ruleKey = "";
   private List<ErrorInfo> errorInfoList = Collections.emptyList();
   
   public ViolatedRule() {
      
   }
   
   public ViolatedRule(String ruleKey) {
      this.ruleKey = ruleKey;
   }
   
   public ViolatedRule(String ruleKey, List<ErrorInfo> errorInfoList) {
      this.ruleKey = ruleKey;
      this.errorInfoList = errorInfoList;
   }
   
   public void setErrorInfoList(List<ErrorInfo> errorInfoList) {
      this.errorInfoList = errorInfoList;
   }
   
   public String getRuleKey() {
      return this.ruleKey;
   }
   
   public List<ErrorInfo> getErrorInfoList() {
      return this.errorInfoList;
   }
   
   @Override
   public String toString() {
      return this.ruleKey;
   }
   
   public int getErrorOccurenceCount() {
	   return this.errorInfoList.size();
   }
   
}

