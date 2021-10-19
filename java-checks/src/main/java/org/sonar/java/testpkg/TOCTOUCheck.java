package org.sonar.java.testpkg;

import java.util.Arrays;
import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "C301")
public class TOCTOUCheck extends IssuableSubscriptionVisitor {
	
	@Override
	public List<Tree.Kind> nodesToVisit() {
		return Arrays.asList(Tree.Kind.METHOD);
	}

	@Override
	public void visitNode(Tree tree) {
		MethodTree mt = (MethodTree) tree;

		// 1. 동기화가 되어있지 않은 public void run() 메소드 검사
		if (isThreadRunMethod(mt) && !isSynchronized(mt)) {

			// 1-1. 우선 블록 전체 영역을 임계 영역으로 설정
			List<StatementTree> criticalSection = mt.block().body();

			// 1-2. synchronized 처리가 되어 있는 부분을 임계 영역에서 제거
			for (StatementTree st : criticalSection) {
				if (st.is(Tree.Kind.SYNCHRONIZED_STATEMENT)) {
					criticalSection.remove(st);
				}
			}

			// 1-3. 임계 영역 내에서 공유 자원 할당 여부 검사
			for (StatementTree st : criticalSection) {
				st.accept(new SharedResourceAllocationChecker());
			}
		}
	}

	private boolean isSynchronized(MethodTree mt) {
		List<ModifierKeywordTree> lmkt = mt.modifiers().modifiers();

		// "synchronized" modifier 검사
		for (ModifierKeywordTree mkt : lmkt) {
			if (mkt.keyword().text().equals("synchronized")) {
				return true;
			}
		}

		return false;
	}

	private boolean isThreadRunMethod(MethodTree mt) {
		// public void run() 체크

		// 1. 메소드 이름 검사
		if (!mt.simpleName().name().equals("run")) {
			return false;
		}

		// 2. 리턴 타입 검사
		if (!mt.returnType().symbolType().isVoid()) {
			return false;
		}

		// 3. modifier 검사
		List<ModifierKeywordTree> lmkt = mt.modifiers().modifiers();

		for (ModifierKeywordTree mkt : lmkt) {
			if (mkt.keyword().text().equals("public")) {
				return true;
			}
		}

		return false;
	}

	// 공유 자원 할당 여부 검사
	private class SharedResourceAllocationChecker extends BaseTreeVisitor {
		
		private final String[] SHARED_RESOURCE_ALLOCATION = {
				"File",				// 파일
				"ServerSocket"		// 소켓
		};
		
		@Override
		public void visitNewClass(NewClassTree tree) {
			super.visitNewClass(tree);

			for (int i = 0; i < SHARED_RESOURCE_ALLOCATION.length; i++) {
				if (tree.symbolType().name().equals(SHARED_RESOURCE_ALLOCATION[i])) {
					reportIssue(tree, "자원이 임계 영역에 할당되었습니다. 해당 자원이 동시에 접근될 가능성이 존재하는지 확인하십시오");
					break;
				}
			}
		}
	}
}
