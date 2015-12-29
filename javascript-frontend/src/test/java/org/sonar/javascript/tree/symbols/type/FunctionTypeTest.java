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
package org.sonar.javascript.tree.symbols.type;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.Type;

import static org.fest.assertions.Assertions.assertThat;

public class FunctionTypeTest extends TypeTest {

  @Before
  public void setUp() throws Exception {
    super.setUp("functionType.js");
  }

  @Test
  public void function_declaration() {
    Symbol f1 = getSymbol("f1");
    assertThat(f1.types().containsOnlyAndUnique(Type.Kind.FUNCTION)).isTrue();
  }

  @Test
  public void function_expression() {
    Symbol f2 = getSymbol("f2");
    assertThat(f2.types().containsOnlyAndUnique(Type.Kind.FUNCTION)).isTrue();
  }

  @Test
  public void function_assignment() {
    Symbol f3 = getSymbol("f3");
    assertThat(f3.types().containsOnlyAndUnique(Type.Kind.FUNCTION)).isTrue();
  }

  @Test
  public void parameter_types_inferred_from_call() {
    Symbol p1 = getSymbol("p1");
    assertThat(p1.types().containsOnlyAndUnique(Type.Kind.NUMBER)).isTrue();

    Symbol p2 = getSymbol("p2");
    assertThat(p2.types()).hasSize(3);
    assertThat(p2.types().contains(Type.Kind.STRING)).isTrue();
    assertThat(p2.types().contains(Type.Kind.NUMBER)).isTrue();
    assertThat(p2.types().contains(Type.Kind.BOOLEAN)).isTrue();

    Symbol p3 = getSymbol("p3");
    assertThat(p3.types().containsOnlyAndUnique(Type.Kind.BOOLEAN)).isTrue();

    Symbol n = getSymbol("n");
    assertThat(n.types()).hasSize(0);

    Symbol a = getSymbol("a");
    assertThat(a.types()).hasSize(0);

    Symbol msg = getSymbol("msg");
    assertThat(msg.types().containsOnlyAndUnique(Type.Kind.STRING)).isTrue();

  }

  @Test
  public void test_parameter_with_unknown() throws Exception {
    Symbol p4 = getSymbol("p4");
    assertThat(p4.types()).containsOnly(PrimitiveType.UNKNOWN);

    Symbol p5 = getSymbol("p5");
    assertThat(p5.types()).hasSize(3);
    assertThat(p5.types()).contains(PrimitiveType.UNKNOWN);
    assertThat(p5.types()).contains(PrimitiveType.NUMBER);
    assertThat(p5.types()).contains(PrimitiveType.BOOLEAN);

    Symbol p6 = getSymbol("p6");
    assertThat(p6.types()).hasSize(2);
    assertThat(p6.types()).contains(PrimitiveType.UNKNOWN);
    assertThat(p6.types()).contains(PrimitiveType.NUMBER);

    Symbol p7 = getSymbol("p7");
    assertThat(p7.types()).hasSize(2);
    assertThat(p7.types()).contains(PrimitiveType.STRING);
    assertThat(p7.types()).contains(PrimitiveType.NUMBER);

    Symbol p8 = getSymbol("p8");
    assertThat(p8.types()).hasSize(3);
    assertThat(p8.types()).contains(PrimitiveType.UNKNOWN);
    assertThat(p8.types()).contains(PrimitiveType.BOOLEAN);
    assertThat(p8.types()).contains(PrimitiveType.NUMBER);
  }

}
