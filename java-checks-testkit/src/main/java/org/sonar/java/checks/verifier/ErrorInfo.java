package org.sonar.java.checks.verifier;

import org.sonar.java.AnalyzerMessage;

public class ErrorInfo {
   private AnalyzerMessage am;
   private AnalyzerMessage.TextSpan ts;
   
   public ErrorInfo() {
      this.am = null;
      this.ts = null;
   }
   
   public ErrorInfo(AnalyzerMessage am, AnalyzerMessage.TextSpan ts) {
      this.am = am;
      this.ts = ts;
   }
   
   public String getErrorMessage() {
      return this.am.getMessage();
   }
   
   public int getStartLine() {
      return this.ts.startLine;
   }
   
   public int getEndLine() {
      return this.ts.endLine;
   }
   
   public int getStartCharacter() {
      return this.ts.startCharacter;
   }
   
   public int getEndCharacter() {
      return this.ts.endCharacter;
   }
}