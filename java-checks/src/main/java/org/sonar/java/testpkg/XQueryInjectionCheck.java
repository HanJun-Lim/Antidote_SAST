package org.sonar.java.testpkg;

import static org.sonar.java.checks.helpers.ReassignmentFinder.getInitializerOrExpression;
import static org.sonar.java.checks.helpers.ReassignmentFinder.getReassignments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "C107")
public class XQueryInjectionCheck extends IssuableSubscriptionVisitor {

	// Watch List : XQuery Expression을 담는 변수의 리스트
	private List<Symbol> XQUERY_WATCH_LIST = new ArrayList<Symbol>();

	// XQuery Expression을 담는 메소드 모음
	private static String[] XQUERY_EXPRESSION_METHODS = { "prepareExpression" };
	private static String[] XQUERY_EXECQUERY_METHODS = { "executeQuery" };
	private static String[] XQUERY_SAFE_HANDLE_METHOD = { "bindString" };

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.VARIABLE, Tree.Kind.MEMBER_SELECT);
	}

	@Override
	public void visitNode(Tree tree) {
		// Variable 검사
		// 대상 : query를 준비하는 부분
		if (tree.is(Tree.Kind.VARIABLE)) {
			tree.accept(new SuspectVariableChecker());
		}
		// Method 검사
		// 안전한 처리가 되었는지 판단
		else if (tree.is(Tree.Kind.MEMBER_SELECT)) {
			MemberSelectExpressionTree mset = (MemberSelectExpressionTree) tree;
			String methodName = mset.lastToken().text();
			String varName = mset.firstToken().text();

			// 안전한 처리가 되었을 경우 검사 대상(Watch List)에서 제외
			if (methodName.equals(XQUERY_SAFE_HANDLE_METHOD[0])) {
				Iterator<Symbol> iter = XQUERY_WATCH_LIST.iterator();
				while (iter.hasNext()) {
					Symbol var = iter.next();

					if (var.name().equals(varName)) {
						iter.remove();
					}
				}
			}

		}
	}

	// 변수 초기화 값을 검사하여 외부 값을 받는 경우 Watch List에 추가
	private class SuspectVariableChecker extends BaseTreeVisitor {
		// 체크 순서
		// Method Invocation -> Member Select -> Variable

		private boolean suspiciousInvocation = false;
		private boolean suspiciousMethod = false;

		// 위 플래그가 모두 true라면 해당 변수를 Watch List에 추가한다
		@Override
		public void visitVariable(VariableTree tree) {
			super.visitVariable(tree);

			// 아래 두 개의 검사가 모두 일치할 경우 Watch List에 등록
			if (suspiciousInvocation) {
				XQUERY_WATCH_LIST.add(tree.symbol());
			}
		}

		// 메소드 이름 검사
		@Override
		public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
			super.visitMemberSelectExpression(tree);

			String varName = tree.firstToken().text();
			String methodName = tree.lastToken().text();

			// XQuery Expression을 담는지 여부 검사
			for (int i = 0; i < XQUERY_EXPRESSION_METHODS.length; i++) {
				if (methodName.equals(XQUERY_EXPRESSION_METHODS[i])) {
					this.suspiciousMethod = true;
				}
			}

			// XQuery를 실행하는지 여부 검사
			if (isSuspiciousVariable(varName) && isSuspiciousMethod(methodName)) {
				reportIssue(tree, "해당 XQuery를 담는 변수가 안전하게 처리되었는지 확인하십시오");
			}
		}

		// 메소드 argument 검사
		@Override
		public void visitMethodInvocation(MethodInvocationTree tree) {
			super.visitMethodInvocation(tree);

			// argument가 동적 문자열인지 검사
			// argument가 동적 문자열이라면 의심 메소드로 판별
			Optional<ExpressionTree> externalValueArg = arguments(tree)
					.filter(arg -> arg.symbolType().is("java.lang.String")).findAny();

			Optional<ExpressionTree> filteredArg;
			filteredArg = externalValueArg.filter(XQueryInjectionCheck::isDynamicString);

			boolean dynamicStr = filteredArg.isPresent();

			if (dynamicStr) {
				this.suspiciousInvocation = true;
			}
		}

		private boolean isSuspiciousVariable(String varName) {
			for (Symbol e : XQUERY_WATCH_LIST) {
				if (e.name().equals(varName)) {
					return true;
				}
			}
			return false;
		}

		private boolean isSuspiciousMethod(String methodName) {
			for (int i = 0; i < XQUERY_EXECQUERY_METHODS.length; i++) {
				if (methodName.equals(XQUERY_EXECQUERY_METHODS[i])) {
					return true;
				}
			}
			return false;
		}
	}

	private static Stream<ExpressionTree> arguments(Tree methodTree) {
		if (methodTree.is(Tree.Kind.METHOD_INVOCATION)) {
			return ((MethodInvocationTree) methodTree).arguments().stream();
		}
		return Stream.empty();
	}

	private static boolean isDynamicString(ExpressionTree arg) {
		if (arg.is(Tree.Kind.PLUS_ASSIGNMENT)) { // PLUS_ASSIGNMENT : +=
			return !((AssignmentExpressionTree) arg).expression().asConstant().isPresent(); // false if the value is
			// present..
		}
		if (arg.is(Tree.Kind.IDENTIFIER)) {
			Symbol symbol = ((IdentifierTree) arg).symbol();
			ExpressionTree initializerOrExpression = getInitializerOrExpression(symbol.declaration());

			return (initializerOrExpression != null && isDynamicString(initializerOrExpression))
					|| getReassignments(symbol.owner().declaration(), symbol.usages()).stream()
							.anyMatch(XQueryInjectionCheck::isDynamicString);
		}

		// argument의 Tree의 종류가 +인가 (concat) / !(상수로서 표현 가능한가) -> 변수가 1개라도 있을 경우 상수로서 표현
		// 불가
		return arg.is(Tree.Kind.PLUS) && !arg.asConstant().isPresent();
	}
}
