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
package org.sonar.plugins.java.api.tree;

import com.google.common.annotations.Beta;
import java.util.List;

/**
 * 'switch' expression.
 *
 * JLS 14.11
 *
 * <pre>
 *   switch ( {@link #expression()} ) {
 *     {@link #cases()}
 *   }
 * </pre>
 *
 * @since Java 12 (SonarJava 5.12 - Support of Java 12)
 */
@Beta
public interface SwitchExpressionTree extends ExpressionTree {

  SyntaxToken switchKeyword();

  SyntaxToken openParenToken();

  ExpressionTree expression();

  SyntaxToken closeParenToken();

  SyntaxToken openBraceToken();

  List<CaseGroupTree> cases();

  SyntaxToken closeBraceToken();
}
