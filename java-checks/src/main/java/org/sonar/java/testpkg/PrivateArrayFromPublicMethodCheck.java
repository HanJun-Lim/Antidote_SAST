package org.sonar.java.testpkg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.*;

@Rule(key = "C604")
public class PrivateArrayFromPublicMethodCheck extends IssuableSubscriptionVisitor {

   // private인 배열 타입 변수들을 리스트에 저장
   private List<Symbol> suspectVarList = new ArrayList<Symbol>();

   @Override
   public List<Tree.Kind> nodesToVisit() {
      return Arrays.asList(Tree.Kind.VARIABLE, Tree.Kind.METHOD);
   }

   @Override
   public void visitNode(Tree tree) {

      if (tree.is(Tree.Kind.VARIABLE)) {
         VariableTree var = (VariableTree) tree;

         // 의심가는 변수라면 리스트에 변수 정보 삽입
         if (isSuspectMemberVariable(var)) {
            suspectVarList.add(var.symbol());
         }
      } else if (tree.is(Tree.Kind.METHOD)) {
         MethodTree mt = (MethodTree) tree;
         SafeHandleChecker safeHandleChecker = new SafeHandleChecker(); // 새로운 Visitor 객체 생성
         List<ModifierKeywordTree> mkt;

         mkt = mt.modifiers().modifiers();
         if (!mkt.isEmpty()) {
            // 메소드의 public 키워드 검사 - 해당 메소드가 public인가 검사
            if (mkt.get(0).keyword().text().equals("public")) {
               mt.accept(safeHandleChecker);
            }
         }
      }
   }

   private boolean isSuspectMemberVariable(VariableTree var) {
      boolean privateTypeOn = false;
      boolean arrayTypeOn = false;
      TypeTree vartype = var.type();

      // 1. 변수의 접근 지정자(access modifier) 검사
      Iterator<ModifierKeywordTree> it = var.modifiers().modifiers().iterator();
      while (it.hasNext()) {
         if (it.next().keyword().text().matches("private")) {
            privateTypeOn = true;
         }
      }

      // 2. 변수의 Array 타입 여부 검사
      if (vartype.symbolType().isArray()) {
         arrayTypeOn = true;
      }

      return (privateTypeOn && arrayTypeOn);
   }

   // 메소드의 body를 검사하여 안전하게 처리되었는가 체크
   private class SafeHandleChecker extends BaseTreeVisitor {
      
      @Override
      public void visitIdentifier(IdentifierTree tree) {
         super.visitIdentifier(tree);
         
         if (isInReturnStatement(tree) && isSuspectVariable(tree.symbol(), suspectVarList)) {
            reportIssue(tree, "Private 배열이 Public 메소드로부터 직접적으로 반환되었습니다");
         }
      }

      private boolean isSuspectVariable(Symbol var, List<Symbol> suspectList) {
         boolean result = false;
         Iterator<Symbol> it = suspectList.iterator();
         Symbol next;

         while (it.hasNext()) {
            next = it.next();

            // 비교할 심볼의 이름/스코프와 의심가는 심볼의 이름/스코프 비교하여 하나라도 해당되면 위험하다 판단
            if (next.name().matches(var.name()) && next.owner().name().matches(var.owner().name())) {
               result = true;
               break;
            }
         }

         return result;
      }
      
      private boolean isInReturnStatement(IdentifierTree tree) {
         boolean inReturnStatement = false;
         
         if(tree.parent().is(Tree.Kind.RETURN_STATEMENT))
         {
            inReturnStatement = true;
         } 
         
         return inReturnStatement;
      }
   }
}