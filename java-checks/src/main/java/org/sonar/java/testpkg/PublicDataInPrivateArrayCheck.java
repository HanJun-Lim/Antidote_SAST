package org.sonar.java.testpkg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.*;

@Rule(key = "C605")
public class PublicDataInPrivateArrayCheck extends IssuableSubscriptionVisitor {

	// private인 배열 타입 변수들을 리스트에 저장
	private List<Symbol> suspectVarList = new ArrayList<Symbol>();

	// 의심 파라미터 리스트를 저장
	private List<Symbol> suspectParamList = new ArrayList<Symbol>();
	
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
			ModifiersTree mkt = mt.modifiers();

			if (!mkt.isEmpty()) {
				// 메소드의 public 키워드 검사 - 해당 메소드가 public인가 검사
				if (isPublicMethod(mkt)) {
					// 1. 의심 파라미터를 담는 리스트 초기화
					suspectParamList.clear();
					
					// 2. 의심가는 파라미터를 검사하여 있는 경우 의심 파라미터 리스트에 추가
					for (VariableTree param : mt.parameters()) {
						if (isSuspectParameter(param)) {
							suspectParamList.add(param.symbol());
						}
					}
					
					mt.accept(new AssignmentChecker());
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

	private boolean isPublicMethod(ModifiersTree mt) {
		List<ModifierKeywordTree> lmkt = mt.modifiers();
		if (!lmkt.isEmpty()) {
			if (lmkt.get(0).keyword().text().matches("public")) {
				return true;
			}
		}
		return false;
	}

	private boolean isSuspectParameter(VariableTree param) {
		for (Symbol suspectVar : suspectVarList) {
			// 파라미터와 의심 변수의 타입 검사. 둘의 타입이 같을 경우 해당 파라미터는 의심된다 판단
			if (param.symbol().type().name().equals(suspectVar.type().name())) {
				return true;
			}
		}

		return false;
	}
	
	// 메소드를 검사하여 안전하게 처리되었는가 체크
	private class AssignmentChecker extends BaseTreeVisitor {

		@Override
		public void visitAssignmentExpression(AssignmentExpressionTree tree) {
			super.visitAssignmentExpression(tree);

			// 할당 대상 검사 && 할당 값 검사
			System.out.println(isAssigningToSuspectVar(tree.variable()) + " / " + isAssigningSuspectParam(tree.expression()));
			if (isAssigningToSuspectVar(tree.variable()) && isAssigningSuspectParam(tree.expression())) {
				reportIssue(tree, "Private 배열에 Public 데이터가 할당되었습니다");
			}
		}

		private boolean isAssigningToSuspectVar(ExpressionTree var) {
			for (Symbol suspectVar : suspectVarList) {
				if (var.lastToken().text().equals(suspectVar.name())) {
					return true;
				}
			}
			return false;
		}

		private boolean isAssigningSuspectParam(ExpressionTree exp) {

			for (Symbol param : suspectParamList) {
				if (exp.lastToken().text().equals(param.name())) {
					return true;
				}
			}
			return false;
		}
	}

}