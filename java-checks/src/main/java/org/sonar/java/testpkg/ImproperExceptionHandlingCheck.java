package org.sonar.java.testpkg;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "C403")
public class ImproperExceptionHandlingCheck extends IssuableSubscriptionVisitor {

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.CATCH);
	}

	@Override
	public void visitNode(Tree tree) {
		CatchTree ct = (CatchTree)tree;
		Symbol catchParam = ct.parameter().symbol();
		
		// Exception 발견 시 위반 발생
		if(catchParam.type().name().contentEquals("Exception")) {
			reportIssue(ct.parameter(), "광범위한 예외 클래스를 사용하고 있습니다");
		}
		
	}
}