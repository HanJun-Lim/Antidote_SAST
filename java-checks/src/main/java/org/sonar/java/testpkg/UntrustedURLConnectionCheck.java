package org.sonar.java.testpkg;


import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "C106")
public class UntrustedURLConnectionCheck extends IssuableSubscriptionVisitor {

	// TODO : 해당 변수가 sendRedirect()에서 쓰이는지 확인
	// sendRedirect(String url)

	// 의심가는 메소드의 이름
	private static String[] URL_CONNECTION_SUSPECTS = { "sendRedirect" };

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.MEMBER_SELECT);
	}

	@Override
	public void visitNode(Tree tree) {
		// 메소드 호출인지 판별
		if (isMethodInvocation(tree)) {
			String methodName = tree.lastToken().text();

			if (checkIfSuspectMethods(methodName)) {
				MethodInvocationTree mit = (MethodInvocationTree) tree.parent();

				mit.arguments().accept(new ArgumentVisitor());
			}
		}
	}

	private boolean isMethodInvocation(Tree tree) {
		if (tree.parent() != null && tree.parent().is(Tree.Kind.METHOD_INVOCATION)) {
			return true;
		}
		return false;
	}

	private boolean checkIfSuspectMethods(String methodName) {
		for (int i = 0; i < URL_CONNECTION_SUSPECTS.length; i++) {
			if (methodName.equals(URL_CONNECTION_SUSPECTS[i])) {
				return true;
			}
		}
		return false;
	}

	// 메소드의 body를 검사하여 안전하게 처리되었는가 체크
	private class ArgumentVisitor extends BaseTreeVisitor {

		@Override
		public void visitIdentifier(IdentifierTree tree) {
			super.visitIdentifier(tree);

			reportIssue(tree, "해당 URL 변수가 안전하게 처리되었는지 확인하십시오");
		}

	}

}
