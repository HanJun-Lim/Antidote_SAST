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
package org.sonar.java.checks.verifier;

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.assertj.core.api.Fail;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * It is possible to specify the absolute line number on which the issue should appear by appending {@literal "@<line>"} to "Noncompliant".
 * But usually better to use line number relative to the current, this is possible to do by prefixing the number with either '+' or '-'.
 * For example:
 * <pre>
 *   // Noncompliant@+1 {{do not import "java.util.List"}}
 *   import java.util.List;
 * </pre>
 * Full syntax:
 * <pre>
 *   // Noncompliant@+1 [[startColumn=1;endLine=+1;endColumn=2;effortToFix=4;secondary=3,4]] {{issue message}}
 * </pre>
 * Attributes between [[]] are optional:
 * <ul>
 *   <li>startColumn: column where the highlight starts</li>
 *   <li>endLine: relative endLine where the highlight ends (i.e. +1), same line if omitted</li>
 *   <li>endColumn: column where the highlight ends</li>
 *   <li>effortToFix: the cost to fix as integer</li>
 *   <li>secondary: a comma separated list of integers identifying the lines of secondary locations if any</li>
 * </ul>
 */
@Beta
public class JavaCheckVerifier extends CheckVerifier {

  /**
   * Default location of the jars/zips to be taken into account when performing the analysis.
   */
  public static final String DEFAULT_TEST_JARS_DIRECTORY = "../java-checks-test-sources/target/test-jars";
  private String testJarsDirectory;
  private boolean providedJavaVersion = false;
  private JavaVersion javaVersion = new JavaVersionImpl();

  private JavaCheckVerifier() {
    this.testJarsDirectory = DEFAULT_TEST_JARS_DIRECTORY;
  }

  @Override
  public String getExpectedIssueTrigger() {
    return "// " + ISSUE_MARKER;
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check.
   *
   * <br /><br />
   *
   * By default, any jar or zip archive present in the folder defined by {@link JavaCheckVerifier#DEFAULT_TEST_JARS_DIRECTORY} will be used
   * to add extra classes to the classpath. If this folder is empty or does not exist, then the analysis will be based on the source of
   * the provided file.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   */
  public static List<ErrorInfo> verify(String filename, JavaFileScanner check) {
   System.out.println("verify진입했다. 이거뜨면 내부 java-testkit 쓴거다");
    return scanFile(filename, check, new JavaCheckVerifier());
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check and a given
   * java version used for the sources.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param javaVersion The version to consider for the analysis (6 for java 1.6, 7 for 1.7, etc.)
   */
  public static List<ErrorInfo> verify(String filename, JavaFileScanner check, int javaVersion) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.providedJavaVersion = true;
    javaCheckVerifier.javaVersion = new JavaVersionImpl(javaVersion);
    return scanFile(filename, check, javaCheckVerifier);
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check,
   * but using having the classpath extended with a collection of files (classes/jar/zip).
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param classpath The files to be used as classpath
   */
  public static List<ErrorInfo> verify(String filename, JavaFileScanner check, Collection<File> classpath) {
    return scanFile(filename, check, new JavaCheckVerifier(), classpath);
  }

  /**
   * Verifies that the provided file will raise all the expected issues when analyzed with the given check,
   * using jars/zips files from the given directory to extends the classpath.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param testJarsDirectory The directory containing jars and/or zip defining the classpath to be used
   */
  public static List<ErrorInfo> verify(String filename, JavaFileScanner check, String testJarsDirectory) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.testJarsDirectory = testJarsDirectory;
    return scanFile(filename, check, javaCheckVerifier);
  }

  /**
   * Verifies that the provided file will not raise any issue when analyzed with the given check.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   */
  public static List<ErrorInfo> verifyNoIssue(String filename, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.expectNoIssues();
    return scanFile(filename, check, javaCheckVerifier);
  }

  public static void verifyNoIssueWithoutSemantic(String filename, JavaFileScanner check, int javaVersion) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier() {
      @Override
      public String getExpectedIssueTrigger() {
        return "// NOSEMANTIC_ISSUE";
      }
    };
    javaCheckVerifier.expectNoIssues();
    javaCheckVerifier.providedJavaVersion = true;
    javaCheckVerifier.javaVersion = new JavaVersionImpl(javaVersion);
    scanFile(filename, check, javaCheckVerifier, Collections.<File>emptyList(), false);
  }

  public static void verifyNoIssueWithoutSemantic(String filename, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier() {
      @Override
      public String getExpectedIssueTrigger() {
        return "// NOSEMANTIC_ISSUE";
      }
    };
    javaCheckVerifier.expectNoIssues();
    scanFile(filename, check, javaCheckVerifier, Collections.<File>emptyList(), false);
  }

  /**
   * Verifies that the provided file will not raise any issue when analyzed with the given check.
   *
   * @param filename The file to be analyzed
   * @param check The check to be used for the analysis
   * @param javaVersion The version to consider for the analysis (6 for java 1.6, 7 for 1.7, etc.)
   */
  public static List<ErrorInfo> verifyNoIssue(String filename, JavaFileScanner check, int javaVersion) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.expectNoIssues();
    javaCheckVerifier.providedJavaVersion = true;
    javaCheckVerifier.javaVersion = new JavaVersionImpl(javaVersion);
    return scanFile(filename, check, javaCheckVerifier);
  }

  /**
   * Verifies that the provided file will only raise an issue on the file, with the given message, when analyzed using the given check.
   *
   * @param filename The file to be analyzed
   * @param message The message expected to be raised on the file
   * @param check The check to be used for the analysis
   */
  public static List<ErrorInfo> verifyIssueOnFile(String filename, String message, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.setExpectedFileIssue(message);
    return scanFile(filename, check, javaCheckVerifier);
  }

  public static List<ErrorInfo> verifyIssueOnProject(String filename, String message, JavaFileScanner check) {
    JavaCheckVerifier javaCheckVerifier = new JavaCheckVerifier();
    javaCheckVerifier.setExpectedProjectIssue(message);
    return scanFile(filename, check, javaCheckVerifier);
  }

  private static List<ErrorInfo> scanFile(String filename, JavaFileScanner check, JavaCheckVerifier javaCheckVerifier) {
    List<File> classpath = getClassPath(javaCheckVerifier.testJarsDirectory);
    return scanFile(filename, check, javaCheckVerifier, classpath);
  }

  static List<File> getClassPath(String jarsDirectory) {
    List<File> classpath = new LinkedList<>();
    Path testJars = Paths.get(jarsDirectory);
    if (testJars.toFile().exists()) {
      classpath = getFilesRecursively(testJars, new String[] {"jar", "zip"});
    } else if (!DEFAULT_TEST_JARS_DIRECTORY.equals(jarsDirectory)) {
      Fail.fail("The directory to be used to extend class path does not exists (" + testJars.toAbsolutePath() + ").");
    }
    classpath.add(new File("target/test-classes"));
    return classpath;
  }

  static List<File> getFilesRecursively(Path root, final String[] extensions) {
    final List<File> files = new ArrayList<>();

    FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
        for (String extension : extensions) {
          if (filePath.toString().endsWith("." + extension)) {
            files.add(filePath.toFile());
            break;
          }
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
      }
    };

    try {
      Files.walkFileTree(root, visitor);
    } catch (IOException e) {
      // we already ignore errors in the visitor
    }

    return files;
  }

  private static List<ErrorInfo> scanFile(String filename, JavaFileScanner check, JavaCheckVerifier javaCheckVerifier, Collection<File> classpath) {
    return scanFile(filename, check, javaCheckVerifier, classpath, true);
  }

  private static List<ErrorInfo> scanFile(String filename, JavaFileScanner check, JavaCheckVerifier javaCheckVerifier, Collection<File> classpath, boolean withSemantic) {
    System.out.println("scanFile함수 진입했다. 이것은 내부 JavaCheckVerifier이다");
     // 1. filename : 분석할 파일 이름 (취약한 소스 파일)
    // 2. check : 분석에 사용할 Rule
    // 3. javaCheckVerifier : Rule 위반 여부 검사 객체
    // 4. classpath : 클래스의 path 로써 쓰일 파일 모음
    // 5. withSemantic : (추측) 의미 분석이 완료되었는가?


    // -------------------- 필요한 변수 선언 및 초기화 ---------------

    // 컴파일 및 에러 발생 정보 수집
    //    expectedIssueCollector : 룰 위반 정보 여기에 있을 것으로 추정
    //    visitorBridge : 컴파일 후 Syntax Tree 생성한 정보가 담겨있을 것
    JavaFileScanner expectedIssueCollector = new ExpectedIssueCollector(javaCheckVerifier);
    VisitorsBridgeForTests visitorsBridge;

    // 파일 경로 및 부모 디렉터리 정보 수집
    //    filePath : 취약한 소스 파일의 절대 경로 얻어옴
    //    moduleBaseDir : 취약한 소스 파일이 위치한 디렉토리를 얻어옴
    File filePath = new File(filename).getAbsoluteFile();
    File moduleBaseDir = filePath.getParentFile();

    // 해당 취약한 파일에 대한 정보 세팅
    //    inputFile : 해당 취약 소스 파일의 메타 데이터를 불러와 세팅하는 듯 하다
    //    sonarComponents : 해당 취약 소스 파일의 메타 데이터를 이용하여 분석을 위한 정보를 세팅하는 듯 하다
    DefaultInputFile inputFile = new TestInputFileBuilder("", moduleBaseDir, filePath).setCharset(StandardCharsets.UTF_8).build();
    SonarComponents sonarComponents = CheckVerifier.sonarComponents(inputFile);


    // --------------- 파일 스캔 : 룰 정보와 룰 위반 정보를 넘겨주고 컴파일 및 룰 위반 스캔 ---------------
    if (withSemantic) {
      visitorsBridge = new VisitorsBridgeForTests(Lists.newArrayList(check, expectedIssueCollector), Lists.newArrayList(classpath), sonarComponents);
    } else {
      visitorsBridge = new VisitorsBridgeForTests(Lists.newArrayList(check, expectedIssueCollector), sonarComponents);
    }

    JavaAstScanner.scanSingleFileForTests(inputFile, visitorsBridge, javaCheckVerifier.javaVersion, null);
    VisitorsBridgeForTests.TestJavaFileScannerContext testJavaFileScannerContext = visitorsBridge.lastCreatedTestContext();

    if (testJavaFileScannerContext == null) {
      Fail.fail("Semantic was required but it was not possible to create it. Please checks the logs to find out the reason.");
    }

    // --------------- 룰 위반(Issue)을 발생시킴 ---------------
    List<ErrorInfo> occuredErrorList;
    occuredErrorList = javaCheckVerifier.checkIssues(testJavaFileScannerContext.getIssues(), javaCheckVerifier.providedJavaVersion);
    
    return occuredErrorList;
  }

  static class ExpectedIssueCollector extends SubscriptionVisitor {

    private final CheckVerifier verifier;

    public ExpectedIssueCollector(CheckVerifier verifier) {
      this.verifier = verifier;
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.TRIVIA);
    }

    @Override
    public void visitTrivia(SyntaxTrivia syntaxTrivia) {
      verifier.collectExpectedIssues(syntaxTrivia.comment(), syntaxTrivia.startLine());
    }
  }
}