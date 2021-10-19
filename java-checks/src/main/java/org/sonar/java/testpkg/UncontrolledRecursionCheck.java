package org.sonar.java.testpkg;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "C302")
public class UncontrolledRecursionCheck extends IssuableSubscriptionVisitor {

	private String methodName;
	private List<VariableTree> methodParameter;

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.METHOD);
	}

	// 자기 자신을 호출하는 경우: 함수명, 파라미터의 개수, 파라미터의 자료형이 동일
	@Override
	public void visitNode(Tree tree) {
		MethodTree mt = (MethodTree) tree;
		
		// 현재 메소드의 이름과 파라미터 정보 세팅
		methodName = mt.simpleName().name();
		methodParameter = mt.parameters();

		// 메소드 내부 탐색
		mt.accept(new RecursionChecker());
	}

	private class RecursionChecker extends BaseTreeVisitor {
		
		private boolean controlled = false;
		
		@Override
		public void visitIfStatement(IfStatementTree tree) {
			System.out.println("제어문 발견됨, Line: " + ((Tree)tree).firstToken().line());
			this.controlled = true;
		}

		@Override
		public void visitMethodInvocation(MethodInvocationTree tree) {
			super.visitMethodInvocation(tree);

			System.out.println("함수 호출 발견됨, Line: " + ((Tree)tree).firstToken().line());
			/*
			 * System.out.println("메서드 호출 : " + tree.methodSelect().lastToken().text()); if
			 * (!tree.arguments().isEmpty()) { for (int i = 0; i < tree.arguments().size();
			 * i++) { System.out.println(i + "번째 인자의 타입: " +
			 * tree.arguments().get(i).symbolType().name()); } }
			 */

			if ((!controlled) && invocateItself(tree)) {
				reportIssue(tree, "귀납 조건 없이 자기 자신을 호출하고 있습니다");
			}
		}

		private boolean invocateItself(MethodInvocationTree mit) {
			// 1. 메소드 이름 검사
			if (!mit.methodSelect().lastToken().text().equals(methodName)) {
				System.out.println("메소드 이름이 다름");
				return false;
			}

			// 2. 파라미터 개수 검사
			if (mit.arguments().size() != methodParameter.size()) {
				System.out.println("파라미터 개수가 다름");
				return false;
			}

			// 3. 파라미터 타입 검사
			for (int i = 0; i < mit.arguments().size(); i++) {
				String argumentTypeName = mit.arguments().get(i).symbolType().name();
				String parameterTypeName = methodParameter.get(i).type().symbolType().name();

				if (!argumentTypeName.equals(parameterTypeName)) {
					System.out.println("파라미터 타입이 다름");
					return false;
				}
			}

			return true;
		}
	}
}
