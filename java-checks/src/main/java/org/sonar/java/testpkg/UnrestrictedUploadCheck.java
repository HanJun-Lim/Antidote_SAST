package org.sonar.java.testpkg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "C105")
public class UnrestrictedUploadCheck extends IssuableSubscriptionVisitor {

	// Watch List : 외부 입력값을 담는 변수
	private List<Symbol> VAR_WATCH_LIST = new ArrayList<Symbol>();

	// 외부 입력값을 받는 메소드 모음
	private static String[] GET_FILE_METHOD = { "getFileSystemName", "getOriginalFilename" };

	// 파일을 로드하는 메소드 모음
	private static String[] FILE_IO_METHOD = { "create", "write", "transferTo" };

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
	}

	@Override
	public void visitNode(Tree tree) {
		// 의심되는 변수 체크
		if (tree.is(Tree.Kind.VARIABLE)) {
			tree.accept(new SuspectVariableChecker());
		} else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
			tree.accept(new MethodInvocationChecker());
		}
	}

	// 변수 초기화 값을 검사하여 외부 값을 받는 경우 Watch List에 추가
	private class SuspectVariableChecker extends BaseTreeVisitor {
		private boolean hasExternalValue = false;
		private boolean suspectVarConcatenated = false;

		@Override
		public void visitVariable(VariableTree tree) {
			super.visitVariable(tree);

			// 외부 값을 받는다고 판명날 경우 Watch List에 추가
			if (hasExternalValue || suspectVarConcatenated) {
				VAR_WATCH_LIST.add(tree.symbol());
				System.out.println("다음 변수가 의심 리스트에 추가되었습니다 : " + tree.symbol().name());
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

			for (int i = 0; i < GET_FILE_METHOD.length; i++) {
				if (methodName.equals(GET_FILE_METHOD[i])) {
					this.hasExternalValue = true;
					break;
				}
			}
		}

		@Override
		public void visitBinaryExpression(BinaryExpressionTree tree) {
			super.visitBinaryExpression(tree);

			String strVarName = tree.lastToken().text();

			for (Symbol suspectVar : VAR_WATCH_LIST) {
				if (strVarName.contentEquals(suspectVar.name())) {
					this.suspectVarConcatenated = true;
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

			//String argName = tree.name();
			Symbol arg = tree.symbol();
			
			for (Symbol var : VAR_WATCH_LIST) {
				//if (argName.equals(var.name()) && this.suspiciousMethod) {
				if (arg.equals(var) && this.suspiciousMethod) {
					reportIssue(tree, "해당 변수의 값이 파일 확장자를 적절히 필터링하였는지 확인하십시오");
					break;
				}
			}
		}

		// 메소드 이름 검사
		@Override
		public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
			super.visitMemberSelectExpression(tree);

			String methodName = tree.lastToken().text();

			for (int i = 0; i < FILE_IO_METHOD.length; i++) {
				if (methodName.equals(FILE_IO_METHOD[i])) {
					this.suspiciousMethod = true;
					break;
				}
			}
		}
	}
}
