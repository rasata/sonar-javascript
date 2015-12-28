/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.javascript.checks.utils;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.javascript.metrics.ComplexityVisitor;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.javascript.tree.impl.JavaScriptTree;
import org.sonar.javascript.tree.symbols.SymbolModelImpl;
import org.sonar.plugins.javascript.api.JavaScriptCheck;
import org.sonar.plugins.javascript.api.symbols.SymbolModel;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.Tree.Kind;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxToken;
import org.sonar.plugins.javascript.api.tree.lexical.SyntaxTrivia;
import org.sonar.plugins.javascript.api.visitors.Issue;
import org.sonar.plugins.javascript.api.visitors.Issue.FileIssue;
import org.sonar.plugins.javascript.api.visitors.Issue.LegacyIssue;
import org.sonar.plugins.javascript.api.visitors.Issue.PreciseIssue;
import org.sonar.plugins.javascript.api.visitors.IssueLocation;
import org.sonar.plugins.javascript.api.visitors.SubscriptionBaseTreeVisitor;
import org.sonar.plugins.javascript.api.visitors.TreeVisitorContext;

import static org.fest.assertions.Assertions.assertThat;

public class JavaScriptCheckVerifier extends SubscriptionBaseTreeVisitor {

  private final List<TestIssue> expectedIssues = new ArrayList<>();

  public static void verify(JavaScriptCheck check, File file) {
    VerifierContext context = new VerifierContext(file);
    check.scanFile(context);
    new JavaScriptCheckVerifier().verify(context);
  }

  private void verify(VerifierContext context) {
    scanTree(context.getTopTree());
    List<TestIssue> sortedIssues = Ordering.natural().onResultOf(new IssueToLine()).sortedCopy(context.getIssues());
    Iterator<TestIssue> actualIssues = sortedIssues.iterator();

    for (TestIssue expected : expectedIssues) {
      if (actualIssues.hasNext()) {
        verifyIssue(expected, actualIssues.next());
      } else {
        throw new AssertionError("Missing issue at line " + expected.line());
      }
    }

    if (actualIssues.hasNext()) {
      TestIssue issue = actualIssues.next();
      throw new AssertionError("Unexpected issue at line " + issue.line() + ": \"" + issue.message() + "\"");
    }
  }

  private void verifyIssue(TestIssue expected, TestIssue actual) {
    if (actual.line() > expected.line()) {
      throw new AssertionError("Missing issue at line " + expected.line());
    }
    if (actual.line() < expected.line()) {
      throw new AssertionError("Unexpected issue at line " + actual.line() + ": \"" + actual.message() + "\"");
    }
    if (expected.message() != null) {
      assertThat(actual.message()).as("Bad message at line " + expected.line()).isEqualTo(expected.message());
    }
    if (expected.effortToFix() != null) {
      assertThat(actual.effortToFix()).as("Bad effortToFix at line " + expected.line()).isEqualTo(expected.effortToFix());
    }
    if (expected.startColumn() != null) {
      assertThat(actual.startColumn()).as("Bad start column at line " + expected.line()).isEqualTo(expected.startColumn());
    }
    if (expected.endColumn() != null) {
      assertThat(actual.endColumn()).as("Bad end column at line " + expected.line()).isEqualTo(expected.endColumn());
    }
    if (expected.endLine() != null) {
      assertThat(actual.endLine()).as("Bad end line at line " + expected.line()).isEqualTo(expected.endLine());
    }
    if (expected.secondaryLines() != null) {
      assertThat(actual.secondaryLines()).as("Bad secondary locations at line " + expected.line()).isEqualTo(expected.secondaryLines());
    }
  }

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.TOKEN);
  }

  @Override
  public void visitNode(Tree tree) {
    SyntaxToken token = (SyntaxToken) tree;
    for (SyntaxTrivia trivia : token.trivias()) {

      String text = trivia.text().substring(2).trim();
      String marker = "Noncompliant";

      if (text.startsWith(marker)) {
        TestIssue issue = issue(null, trivia.line());
        String paramsAndMessage = text.substring(marker.length()).trim();

        if (paramsAndMessage.startsWith("[[")) {
          int endIndex = paramsAndMessage.indexOf("]]");
          addParams(issue, paramsAndMessage.substring(2, endIndex));
          paramsAndMessage = paramsAndMessage.substring(endIndex + 2).trim();
        }

        if (paramsAndMessage.startsWith("{{")) {
          int endIndex = paramsAndMessage.indexOf("}}");
          String message = paramsAndMessage.substring(2, endIndex);
          issue.message(message);
        }

        expectedIssues.add(issue);
      }
    }
  }

  private void addParams(TestIssue issue, String params) {
    for (String param : Splitter.on(';').split(params)) {
      int equalIndex = param.indexOf("=");
      if (equalIndex == -1) {
        throw new IllegalStateException("Invalid param at line 1: " + param);
      }
      String name = param.substring(0, equalIndex);
      String value = param.substring(equalIndex + 1);

      if ("effortToFix".equalsIgnoreCase(name)) {
        issue.effortToFix(Integer.valueOf(value));

      } else if ("sc".equalsIgnoreCase(name)) {
        issue.startColumn(Integer.valueOf(value));

      } else if ("ec".equalsIgnoreCase(name)) {
        issue.endColumn(Integer.valueOf(value));

      } else if ("el".equalsIgnoreCase(name)) {
        issue.endLine(lineValue(issue.line(), value));

      } else if ("secondary".equalsIgnoreCase(name)) {
        List<Integer> secondaryLines = new ArrayList<>();
        for (String secondary : Splitter.on(',').split(value)) {
          secondaryLines.add(lineValue(issue.line(), secondary));
        }
        issue.secondary(secondaryLines);

      } else {
        throw new IllegalStateException("Invalid param at line 1: " + name);
      }
    }
  }

  private int lineValue(int baseLine, String shift) {
    if (shift.startsWith("+")) {
      return baseLine + Integer.valueOf(shift.substring(1));
    }
    if (shift.startsWith("-")) {
      return baseLine - Integer.valueOf(shift.substring(1));
    }
    return Integer.valueOf(shift);
  }

  private static TestIssue issue(@Nullable String message, int lineNumber) {
    return TestIssue.create(message, lineNumber);
  }

  private static class VerifierContext implements TreeVisitorContext {

    protected static final ActionParser<Tree> p = JavaScriptParserBuilder.createParser(Charsets.UTF_8);

    private File file;
    private ComplexityVisitor complexityVisitor;
    private ScriptTree tree = null;
    private SymbolModel symbolModel = null;
    private List<TestIssue> issues = new ArrayList<>();

    public VerifierContext(File file) {
      this.file = file;
      this.complexityVisitor = new ComplexityVisitor();

      try {
        this.tree = (ScriptTree) p.parse(file);
        this.symbolModel = SymbolModelImpl.create(tree, null, null);

      } catch (RecognitionException e) {
        throw new IllegalArgumentException("Unable to parse file: " + file.getAbsolutePath(), e);
      }

    }

    public List<TestIssue> getIssues() {
      return issues;
    }

    @Override
    public ScriptTree getTopTree() {
      return tree;
    }

    @Override
    public File getFile() {
      return file;
    }

    @Override
    public SymbolModel getSymbolModel() {
      return symbolModel;
    }

    @Override
    public int getComplexity(Tree tree) {
      return complexityVisitor.getComplexity(tree);
    }

    @Override
    public void addIssue(Issue issue) {
      TestIssue testIssue;
      if (issue instanceof FileIssue) {
        throw new UnsupportedOperationException();

      } else if (issue instanceof LegacyIssue) {
        LegacyIssue legacyIssue = (LegacyIssue) issue;
        testIssue = issue(legacyIssue.message(), legacyIssue.line());

      } else {
        PreciseIssue preciseIssue = (PreciseIssue)issue;
        IssueLocation location = preciseIssue.primaryLocation();

        int startColumn = location.startLineOffset() + 1;
        int endColumn = location.endLineOffset() + 1;
        List<Integer> secondaryLines = new ArrayList<>();
        for (IssueLocation secondary : preciseIssue.secondaryLocations()) {
          secondaryLines.add(secondary.startLine());
        }
        testIssue = issue(location.message(), location.startLine())
          .columns(startColumn, endColumn)
          .endLine(location.endLine())
          .secondary(secondaryLines);
      }

      if (issue.cost() != null) {
        testIssue.effortToFix(issue.cost().intValue());
      }

      issues.add(testIssue);
    }

    private static int getLine(Tree tree) {
      return ((JavaScriptTree) tree).getLine();
    }

    @Override
    public String[] getPropertyValues(String name) {
      throw new UnsupportedOperationException();
    }

  }

  private static class IssueToLine implements Function<TestIssue, Integer> {
    @Override
    public Integer apply(TestIssue issue) {
      return issue.line();
    }
  }

}
