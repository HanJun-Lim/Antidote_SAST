package org.sonar.java.testpkg;

/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "C210")
public class HardCodedCryptographicKeyCheck extends IssuableSubscriptionVisitor {

	private static final String DEFAULT_CREDENTIAL_WORDS = "key";
	private static final String JAVA_LANG_STRING = "java.lang.String";
	private static final int MINIMUM_PASSWORD_LENGTH = 1;

	private static final MethodMatcher STRING_TO_CHAR_ARRAY = MethodMatcher.create().typeDefinition(JAVA_LANG_STRING)
			.name("toCharArray").withoutParameter();

	@RuleProperty(key = "credentialWords", description = "Comma separated list of words identifying potential credentials", defaultValue = DEFAULT_CREDENTIAL_WORDS)
	public String credentialWords = DEFAULT_CREDENTIAL_WORDS;

	private List<Pattern> variablePatterns = null;

	private Stream<Pattern> variablePatterns() {
		if (variablePatterns == null) {
			variablePatterns = toPatterns("");
		}
		return variablePatterns.stream();
	}

	private List<Pattern> toPatterns(String suffix) {
		return Stream.of(credentialWords.split(",")).map(String::trim)
				.map(word -> Pattern.compile("(" + word + ")" + suffix, Pattern.CASE_INSENSITIVE))
				.collect(Collectors.toList());
	}

	@Override
	public List<Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.METHOD_INVOCATION);
	}

	@Override
	public void visitNode(Tree tree) {
		// handles each tree

		if (tree.is(Tree.Kind.VARIABLE)) {
			handleVariable((VariableTree) tree);
		} else if (tree.is(Tree.Kind.ASSIGNMENT)) {
			handleAssignment((AssignmentExpressionTree) tree);
		}
	}

	private Optional<String> isPasswordVariableName(IdentifierTree identifierTree) {
		return isPasswordLikeName(identifierTree.name());
	}

	private Optional<String> isPasswordLikeName(String name) {
		return variablePatterns().map(pattern -> pattern.matcher(name))
				// contains "pwd" or similar
				.filter(Matcher::find).map(matcher -> matcher.group(1)).findAny();
	}

	private Optional<String> isPasswordVariable(ExpressionTree variable) {
		if (variable.is(Tree.Kind.MEMBER_SELECT)) {
			return isPasswordVariableName(((MemberSelectExpressionTree) variable).identifier());
		} else if (variable.is(Tree.Kind.IDENTIFIER)) {
			return isPasswordVariableName((IdentifierTree) variable);
		}
		return Optional.empty();
	}

	private static boolean isCallOnStringLiteral(ExpressionTree expr) {
		return expr.is(Tree.Kind.MEMBER_SELECT)
				&& isNotExcludedString(((MemberSelectExpressionTree) expr).expression());
	}

	private void handleVariable(VariableTree tree) {
		IdentifierTree variable = tree.simpleName();
		isPasswordVariableName(variable).filter(passwordVariableName -> {
			ExpressionTree initializer = tree.initializer();
			return initializer != null && isNotExcluded(initializer) && isNotPasswordConst(initializer);
		}).ifPresent(passwordVariableName -> report(variable, passwordVariableName));
	}

	private void handleAssignment(AssignmentExpressionTree tree) {
		ExpressionTree variable = tree.variable();
		isPasswordVariable(variable).filter(passwordVariableName -> isNotExcluded(tree.expression()))
				.ifPresent(passwordVariableName -> report(variable, passwordVariableName));
	}

	private boolean isNotPasswordConst(ExpressionTree expression) {
		if (expression.is(Kind.METHOD_INVOCATION)) {
			ExpressionTree methodSelect = ((MethodInvocationTree) expression).methodSelect();
			return methodSelect.is(Kind.MEMBER_SELECT)
					&& isNotPasswordConst(((MemberSelectExpressionTree) methodSelect).expression());
		}
		String literal = ExpressionsHelper.getConstantValueAsString(expression).value();
		return literal == null || variablePatterns().map(pattern -> pattern.matcher(literal)).noneMatch(Matcher::find);
	}

	private static boolean isNotExcluded(ExpressionTree expression) {
		if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
			MethodInvocationTree mit = (MethodInvocationTree) expression;
			return STRING_TO_CHAR_ARRAY.matches(mit) && isCallOnStringLiteral(mit.methodSelect());
		} else {
			return isNotExcludedString(expression);
		}
	}

	private static boolean isNotExcludedString(ExpressionTree expression) {
		return isNotExcludedString(ExpressionsHelper.getConstantValueAsString(expression).value());
	}

	private static boolean isNotExcludedString(@Nullable String literal) {
		return literal != null && !literal.trim().isEmpty() && literal.length() > MINIMUM_PASSWORD_LENGTH;
	}

	private void report(Tree tree, String match) {
		reportIssue(tree, "'" + match + "' : 암호화 키 정보가 담긴 것으로 의심됩니다, 해당 부분을 재검토해 주십시오");
	}
}
