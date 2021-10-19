package org.sonar.java.testpkg;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "C401")
public class InformationExposureThroughErrorMsgCheck extends IssuableSubscriptionVisitor {
	
	// 외부 입력값을 받는 메소드 모음
	private static String[] PRINT_ERROR_METHOD = { "getMessage", "printStackTrace", "toString" };

	// 현재 탐색중인 예외의 변수
	private Symbol catchParam;
	
	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.CATCH);
	}

	@Override
	public void visitNode(Tree tree) {
		CatchTree ct = (CatchTree)tree;
		catchParam = ct.parameter().symbol();
		
		tree.accept(new ErrorInfoPrintChecker());
	}

	private class ErrorInfoPrintChecker extends BaseTreeVisitor {
		
		@Override
		public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
			super.visitMemberSelectExpression(tree);
			
			String varName = tree.firstToken().text();
			String invocationName = tree.lastToken().text();
			
			// 해당 메소드를 호출하는 변수와
			if(varName.equals(catchParam.name()) && isCallingPrintExceptionMethod(invocationName)) {
				reportIssue(tree.parent(), "에러 메시지를 통하여 정보가 노출되고 있습니다");
			}
		}
		
		private boolean isCallingPrintExceptionMethod(String invocationName) {
			for (int i = 0; i < PRINT_ERROR_METHOD.length; i++) {
				if (invocationName.equals(PRINT_ERROR_METHOD[i])) {
					return true;
				}
			}
			
			return false;
		}
	}
}