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
package org.sonar.plugins.javascript.api.tests;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.javascript.metrics.ComplexityVisitor;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.javascript.tree.symbols.SymbolModelImpl;
import org.sonar.plugins.javascript.api.visitors.FileIssue;
import org.sonar.plugins.javascript.api.visitors.LineIssue;
import org.sonar.plugins.javascript.api.JavaScriptCheck;
import org.sonar.plugins.javascript.api.symbols.SymbolModel;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.visitors.Issue;
import org.sonar.plugins.javascript.api.visitors.TreeVisitorContext;
import org.sonar.squidbridge.api.CheckMessage;

public class TestCheckContext implements TreeVisitorContext {

  private static final Logger LOG = LoggerFactory.getLogger(TestCheckContext.class);
  protected static final ActionParser<Tree> p = JavaScriptParserBuilder.createParser(Charsets.UTF_8);

  private final File file;
  private final ComplexityVisitor complexity;
  private final Settings settings;

  private ScriptTree tree = null;
  private SymbolModel symbolModel = null;

  List<CheckMessage> issues = new LinkedList<>();

  public TestCheckContext(File file, Settings settings, JavaScriptCheck check) {
    this.file = file;
    this.complexity = new ComplexityVisitor();
    this.settings = settings;

    try {
      this.tree = (ScriptTree) p.parse(file);
      this.symbolModel = SymbolModelImpl.create(tree, null, null);

    } catch (RecognitionException e) {
      LOG.error("Unable to parse file: " + file.getAbsolutePath());
      LOG.error(e.getMessage());

      if ("ParsingErrorCheck".equals(check.getClass().getSimpleName())) {
        this.addIssue(new LineIssue(null, e.getLine(), e.getMessage()));
      }
    }

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
  public String[] getPropertyValues(String name) {
    return settings.getStringArray(name);
  }

  @Override
  public int getComplexity(Tree tree) {
    return complexity.getComplexity(tree);
  }

  @Override
  public void addIssue(Issue issue) {
    CheckMessage checkMessage;
    if (issue instanceof FileIssue) {
      FileIssue fileIssue = (FileIssue)issue;
      checkMessage = new CheckMessage(fileIssue.check(), fileIssue.message());

    } else if (issue instanceof LineIssue) {
      LineIssue lineIssue = (LineIssue)issue;
      checkMessage = new CheckMessage(lineIssue.check(), lineIssue.message());
      checkMessage.setLine(lineIssue.line());

    } else {
      throw new IllegalStateException("To test rules which provide precise issue locations use JavaScriptCheckVerifier#verify()");
    }

    if (issue.cost() != null) {
      checkMessage.setCost(issue.cost());
    }

    issues.add(checkMessage);
  }

  public List<CheckMessage> getIssues() {
    return issues;
  }

}
