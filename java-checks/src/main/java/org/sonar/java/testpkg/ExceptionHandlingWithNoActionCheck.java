package org.sonar.java.testpkg;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "C402")
public class ExceptionHandlingWithNoActionCheck extends IssuableSubscriptionVisitor {

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.CATCH);
	}

	@Override
	public void visitNode(Tree tree) {
		CatchTree ct = (CatchTree)tree;
		
		// 블록에 아무 처리도 되어 있지 않으면 위반 발생
		if(ct.block().body().size() == 0) {
			reportIssue(ct.block(), "해당 예외에 대한 아무런 조치가 없습니다");
		}
	}
}