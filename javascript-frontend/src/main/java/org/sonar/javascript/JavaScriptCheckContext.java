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
package org.sonar.javascript;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.config.Settings;
import org.sonar.plugins.javascript.api.symbols.SymbolModel;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.visitors.Issue;
import org.sonar.plugins.javascript.api.visitors.TreeVisitorContext;

public class JavaScriptCheckContext implements TreeVisitorContext {

  private final ScriptTree tree;
  private final File file;
  private final SymbolModel symbolModel;
  private final Settings settings;
  private List<Issue> issues;

  public JavaScriptCheckContext(
    ScriptTree tree, File file, SymbolModel symbolModel,
    Settings settings
  ) {
    this.tree = tree;
    this.file = file;
    this.symbolModel = symbolModel;
    this.settings = settings;
    this.issues = new ArrayList<>();
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
  public ImmutableList<Issue> getIssues() {
    return ImmutableList.copyOf(issues);
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
  public <T extends Issue> T addIssue(T issue) {
    issues.add(issue);
    return issue;
  }

}
