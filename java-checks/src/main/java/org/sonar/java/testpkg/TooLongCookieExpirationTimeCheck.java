package org.sonar.java.testpkg;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;

@Rule(key = "C212")
public class TooLongCookieExpirationTimeCheck extends IssuableSubscriptionVisitor {

	private static final String SET_COOKIE_AGE_METHOD = "setMaxAge";
	private Stack<Integer> expressionElements = new Stack<Integer>();

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.METHOD_INVOCATION);
	}

	@Override
	public void visitNode(Tree tree) {
		MethodInvocationTree mit = (MethodInvocationTree) tree;

		if (isSetMaxAgeInvocation(mit)) {
			List<ExpressionTree> arguments = mit.arguments();
			if (arguments.size() == 1) {
				ExpressionTree exp = mit.arguments().get(0);

				// 내부 연산 항목을 스택에 저장하고 계산
				exp.accept(new ExpressionVisitor());

				// 계산된 시간 검사
				if (getExpCalculationResult(expressionElements) > 1200) {
					reportIssue(exp, "쿠키의 만료 시간이 비교적 길게 설정되어 있습니다");
				}

				// 스택 비움
				expressionElements.clear();
			}
		}
	}

	private boolean isSetMaxAgeInvocation(MethodInvocationTree mit) {
		if (mit.methodSelect().lastToken().text().equals(SET_COOKIE_AGE_METHOD)) {
			return true;
		}
		return false;
	}
	
	private int getExpCalculationResult(Stack<Integer> expressionStack) {
		if(expressionElements.isEmpty()) {
			return 0;
		} else {
			return expressionElements.get(0);
		}
	}

	private class ExpressionVisitor extends BaseTreeVisitor {
		@Override
		public void visitLiteral(LiteralTree tree) {
			super.visitLiteral(tree);
			
			expressionElements.add(Integer.parseInt(tree.value()));
		}

		@Override
		public void visitBinaryExpression(BinaryExpressionTree tree) {
			super.visitBinaryExpression(tree);

			int leftOperand = 0;
			int rightOperand = 0;
			
			switch(tree.operatorToken().text()) {
			case "+":
				rightOperand = expressionElements.pop();
				leftOperand = expressionElements.pop();
				
				expressionElements.push(leftOperand + rightOperand);
				break;
			case "-":
				rightOperand = expressionElements.pop();
				leftOperand = expressionElements.pop();
				
				expressionElements.push(leftOperand - rightOperand);
				break;
			case "*":
				rightOperand = expressionElements.pop();
				leftOperand = expressionElements.pop();
				
				expressionElements.push(leftOperand * rightOperand);
				break;
			case "/":
				rightOperand = expressionElements.pop();
				leftOperand = expressionElements.pop();
				
				expressionElements.push(leftOperand / rightOperand);
				break;
			}
		}
	}
}
