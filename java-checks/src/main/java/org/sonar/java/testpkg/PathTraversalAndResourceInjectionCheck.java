package org.sonar.java.testpkg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "C102")
public class PathTraversalAndResourceInjectionCheck extends IssuableSubscriptionVisitor {

	// Watch List : 외부 입력값을 담는 변수
	private List<Symbol> VAR_WATCH_LIST = new ArrayList<Symbol>();

	// 외부 입력값을 받는 메소드 모음
	private static String[] GET_EXTERNAL_VALUE_METHOD = { "getParameter", "getProperty" };

	// 의심가는 클래스 선언
	private static String[] FILE_HANDLE_SUSPECTS = { "FileInputStream", "File", "FileReader" };

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.VARIABLE, Tree.Kind.NEW_CLASS);
	}

	@Override
	public void visitNode(Tree tree) {
		if (tree.is(Tree.Kind.VARIABLE)) {
			VariableTree var = (VariableTree) tree;
			tree.accept(new SuspectVariableChecker());
		} 
		else if (tree.is(Tree.Kind.NEW_CLASS)) {
			NewClassTree nc = (NewClassTree) tree;
			String className = nc.identifier().firstToken().text();

			for (int i = 0; i < FILE_HANDLE_SUSPECTS.length; i++) {
				if (className.equals(FILE_HANDLE_SUSPECTS[i])) {
					nc.accept(new NewClassChecker());
					break;
				}
			}
		}
	}

	// 변수 초기화 값을 검사하여 외부 값을 받는 경우 Watch List에 추가
	private class SuspectVariableChecker extends BaseTreeVisitor {
		private boolean hasExternalValue = false;

		@Override
		public void visitVariable(VariableTree tree) {
			super.visitVariable(tree);

			// 외부 값을 받는다고 판명날 경우 Watch List에 추가
			if (hasExternalValue) {
				VAR_WATCH_LIST.add(tree.symbol());
			}
		}

		@Override
		public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
			super.visitArrayAccessExpression(tree);

			if (tree.firstToken().text().matches("args")) {
				this.hasExternalValue = true;
			}
		}

		@Override
		public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
			super.visitMemberSelectExpression(tree);
			
			String methodName = tree.lastToken().text();
			
			for (int i = 0; i < GET_EXTERNAL_VALUE_METHOD.length; i++) {
				if (methodName.equals(GET_EXTERNAL_VALUE_METHOD[i])) {
					this.hasExternalValue = true;
					break;
				}
			}
		}
	}

	// 클래스 생성자를 검사하여 의심되는 값이 인자로 들어가는 경우 위반 발생
	private class NewClassChecker extends BaseTreeVisitor {
		@Override
		public void visitIdentifier(IdentifierTree tree) {
			super.visitIdentifier(tree);

			for (Symbol var : VAR_WATCH_LIST) {
				if (var.name().equals(tree.name())) {
					reportIssue(tree, "외부로부터 받는 파일명이 적절히 필터링되었는지 확인하십시오");
					break;
				}
			}
		}
	}
}
