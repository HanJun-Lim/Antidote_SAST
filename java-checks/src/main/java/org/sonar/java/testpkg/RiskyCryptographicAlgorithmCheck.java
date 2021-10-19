package org.sonar.java.testpkg;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.JavaPropertiesHelper;

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

import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "C204")
public class RiskyCryptographicAlgorithmCheck extends IssuableSubscriptionVisitor {

	private List<MethodMatcher> matchers;

	private static final String MESSAGE = "안전하지 않은 암호화 알고리즘입니다. 안전한 암호화 알고리즘을 사용하십시오";

	private static final Set<String> VULNERABLE_ALGORITHMS = Stream.of("DES", "DESede", "RC2", "RC4", "Blowfish")
			.map(name -> name.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());

	
	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
	}

	@Override
	public void visitNode(Tree tree) {
		if (hasSemantic()) {
			for (MethodMatcher invocationMatcher : matchers()) {
				checkInvocation(tree, invocationMatcher);
			}
		}
	}

	private void checkInvocation(Tree tree, MethodMatcher invocationMatcher) {
		if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
			MethodInvocationTree mit = (MethodInvocationTree) tree;
			if (invocationMatcher.matches(mit)) {
				onMethodInvocationFound(mit);
			}
		} else if (tree.is(Tree.Kind.NEW_CLASS)) {
			NewClassTree newClassTree = (NewClassTree) tree;
			if (invocationMatcher.matches(newClassTree)) {
				onConstructorFound(newClassTree);
			}
		}
	}
	
	private List<MethodMatcher> matchers() {
		if (matchers == null) {
			matchers = getMethodInvocationMatchers();
		}
		return matchers;
	}

	protected List<MethodMatcher> getMethodInvocationMatchers() {
		return Arrays.asList(
				MethodMatcher.create().typeDefinition("javax.crypto.Cipher").name("getInstance").withAnyParameters(),
				MethodMatcher.create().typeDefinition("javax.crypto.NullCipher").name("<init>").withAnyParameters());
	}

	protected void onConstructorFound(NewClassTree newClassTree) {
		reportIssue(newClassTree.identifier(), MESSAGE);
	}

	protected void onMethodInvocationFound(MethodInvocationTree mit) {
		ExpressionTree firstArg = mit.arguments().get(0);
		ExpressionTree defaultValue = JavaPropertiesHelper.retrievedPropertyDefaultValue(firstArg);
		String firstArgStringValue = ExpressionsHelper
				.getConstantValueAsString(defaultValue != null ? defaultValue : firstArg).value();
		if (firstArgStringValue != null) {
			checkIssue(firstArg, firstArgStringValue);
		}
	}

	private void checkIssue(ExpressionTree argumentForReport, String algorithm) {
		String[] transformationElements = algorithm.split("/");
		if (transformationElements.length > 0
				&& VULNERABLE_ALGORITHMS.contains(transformationElements[0].toUpperCase(Locale.ROOT))) {
			reportIssue(argumentForReport, MESSAGE);
		}
	}


}
