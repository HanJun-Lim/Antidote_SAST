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
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "C108")
public class XPathInjectionCheck extends IssuableSubscriptionVisitor {

	// Watch List : 외부 입력값을 담는 변수
	private List<Symbol> VAR_WATCH_LIST = new ArrayList<Symbol>();

	// 외부 입력값을 받는 메소드 모음
	private static String[] GET_EXTERNAL_VALUE_METHOD = { "getParameter", "getProperty" };

	// 질의를 작성하는 메소드 모음
	private static String[] XPATH_EXPRESSION_METHOD = { "compile", "evaluate" };

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
	}

	@Override
	public void visitNode(Tree tree) {
		// 의심되는 변수 체크
		if (tree.is(Tree.Kind.VARIABLE)) {
			tree.accept(new SuspectVariableChecker());
		}
		// 의심 변수를 포함하고 있는지 / 호출하는 메소드는 무엇인지 검사
		else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
			tree.accept(new MethodInvocationChecker());
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

			// 인자값을 받는 경우
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

	// 메소드 이름과 내부 인자를 검사하여 위험하다 판단될 경우 이슈 발생
	private class MethodInvocationChecker extends BaseTreeVisitor {
		private boolean suspiciousMethod = false;

		// 내부 인자 검사
		@Override
		public void visitIdentifier(IdentifierTree tree) {
			super.visitIdentifier(tree);

			String argName = tree.name();

			for (Symbol var : VAR_WATCH_LIST) {
				if (argName.equals(var.name()) && this.suspiciousMethod) {
					reportIssue(tree, "해당 변수의 값이 적절히 필터링되었는지 확인하십시오");
					break;
				}
			}
		}

		// 메소드 이름 검사
		@Override
		public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
			super.visitMemberSelectExpression(tree);

			String methodName = tree.lastToken().text();

			for (int i = 0; i < XPATH_EXPRESSION_METHOD.length; i++) {
				if (methodName.equals(XPATH_EXPRESSION_METHOD[i])) {
					this.suspiciousMethod = true;
					break;
				}
			}
		}
	}
}
