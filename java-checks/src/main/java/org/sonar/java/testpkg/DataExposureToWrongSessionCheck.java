package org.sonar.java.testpkg;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "C601")
public class DataExposureToWrongSessionCheck extends IssuableSubscriptionVisitor {

	// TODO
	// 1. HttpServlet을 extend 하는지?
	// 2. 하위 멤버 필드 있는지 검사
	// 3. final인지 검사

	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.CLASS);
	}

	@Override
	public void visitNode(Tree tree) {
		if (tree.is(Tree.Kind.CLASS)) {
			ClassTree ct = (ClassTree) tree;
			
			// 클래스 검사
			//	HttpServlet을 Extend 하고 있는지?, @Controller를 받고 있는지?
			if (hasControllerAnnotation(ct) || isExtendingHttpServlet(ct)) {

				// 해당 클래스의 멤버 검사 (멤버 변수, 멤버 메소드)
				List<Tree> lt = ct.members();
				for (Tree t : lt) {
					// 멤버 변수면 해당 변수의 타입 검사
					if(t instanceof VariableTree) {
						VariableTree vt = (VariableTree)t;
						
						if(!vt.symbol().isFinal()) {
							reportIssue(t, "해당 변수는 다른 세션에 의해 공유될 수 있습니다");
						}
					}
				}
			}
		}
	}
	
	private boolean isExtendingHttpServlet(ClassTree ct) {
		TypeTree superClass = ct.superClass();
		if(superClass != null && superClass.symbolType().name().equals("HttpServlet")) {
			return true;
		}
		
		return false;
	}
	
	private boolean hasControllerAnnotation(ClassTree ct) {
		for(AnnotationTree at: ct.modifiers().annotations()) {
			if(at.symbolType().name().equals("Controller")) {
				return true;
			}
		}
		
		return false;
	}
}

