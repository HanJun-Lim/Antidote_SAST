package org.sonar.java.testpkg;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.*;

import static org.sonar.java.checks.helpers.ReassignmentFinder.getInitializerOrExpression;
import static org.sonar.java.checks.helpers.ReassignmentFinder.getReassignments;

@Rule(key = "C104")
public class CommandInjectionCheck extends IssuableSubscriptionVisitor {

   private static final String JAVA_LANG_RUNTIME = "java.lang.Runtime";

   // 의심가는 명령어 모음
   private static final MethodMatcherCollection COMMAND_EXECUTION_SUSPECTS = MethodMatcherCollection.create(

         /*
          * java.lang.Runtime.getRuntime().exec() ==
          * MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(typeFQN)).name(
          * "exec").withAnyParameters()
          * 
          * exec(String command) exec(String command, String[] envp) exec(String command,
          * String[] envp, File dir) exec(String[] cmdarray) exec(String[] cmdarray,
          * String[] envp) exec(String[] cmdarray, String[] envp, File dir)
          */
         matcherBuilder(JAVA_LANG_RUNTIME).name("exec").withAnyParameters());

   private static MethodMatcher matcherBuilder(String typeFQN) {
      return MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf(typeFQN));
   }

   @Override
   public List<Tree.Kind> nodesToVisit() {
      // checks the symbol of invocated method,
      return Arrays.asList(Tree.Kind.METHOD_INVOCATION);
   }

   @Override
   public void visitNode(Tree tree) {

      // 1. 의심가는 메서드 명인지 체크
      // Fully Qualified Name 으로 세부적인 서브 타입 및 메서드 이름 검사)
      if (anyMatch(tree)) {
         // 2. 메서드의 파라미터 검사. String 타입만 추출
         Optional<ExpressionTree> commandStringArg = arguments(tree)
               .filter(arg -> arg.symbolType().is("java.lang.String")).findAny();

         Optional<ExpressionTree> filteredArg;
         filteredArg = commandStringArg.filter(CommandInjectionCheck::isDynamicString);

         boolean isDynamicStr = filteredArg.isPresent();

         // 3. 해당 변수가 동적 문자열이라면 경고 발생
         if (isDynamicStr) {
            reportIssue(filteredArg.get(), "해당 명령어를 받는 변수가 적절히 필터링되었는지 확인하십시오");
         }
      }
   }

   private static Stream<ExpressionTree> arguments(Tree methodTree) {
      if (methodTree.is(Tree.Kind.METHOD_INVOCATION)) {
         return ((MethodInvocationTree) methodTree).arguments().stream();
      }

      return Stream.empty();
   }

   private static boolean anyMatch(Tree tree) {
      if (!hasArguments(tree)) {
         return false;
      }

      if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
         return COMMAND_EXECUTION_SUSPECTS.anyMatch((MethodInvocationTree) tree);
      }
      return false;
   }

   private static boolean hasArguments(Tree tree) {
      return arguments(tree).findAny().isPresent();
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
                     .anyMatch(CommandInjectionCheck::isDynamicString);
      }

      // argument의 Tree의 종류가 +인가 (concat) / !(상수로서 표현 가능한가) -> 변수가 1개라도 있을 경우 상수로서 표현
      // 불가
      return (arg.is(Tree.Kind.PLUS) && !arg.asConstant().isPresent());
   }

}