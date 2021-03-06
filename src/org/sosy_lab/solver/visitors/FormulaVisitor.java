/*
 *  JavaSMT is an API wrapper for a collection of SMT solvers.
 *  This file is part of JavaSMT.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.solver.visitors;

import com.google.common.base.Function;

import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaManager;
import org.sosy_lab.solver.api.QuantifiedFormulaManager.Quantifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Visitor iterating through entire formula.
 * Use {@link FormulaManager#visit(FormulaVisitor, Formula)} for visiting formulas.
 *
 * @param <R> Desired return type.
 */
public interface FormulaVisitor<R> {

  /**
   * Visit a free variable (such as "x", "y" or "z"), not bound by a quantifier.
   * The variable can have any sort (both boolean and non-boolean).
   *
   * @param f Formula representing the variable.
   * @param name Variable name.
   */
  R visitFreeVariable(Formula f, String name);

  /**
   * Visit a variable bound by a quantifier.
   * The variable can have any sort (both boolean and non-boolean).
   *
   * @param f Formula representing the variable.
   * @param name Variable name
   * @param deBruijnIdx de-Bruijn index of the bound variable, which can be used
   *                    to find the matching quantifier.
   */
  R visitBoundVariable(Formula f, String name, int deBruijnIdx);

  /**
   * Visit a constant, such as "true"/"false" or a numeric constant like "1" or "1.0".
   *
   * @param f Formula representing the constant.
   * @param value The value of the constant. It is either of type {@link Boolean} or of a subtype
   *     of {@link Number}, in most cases a {@link BigInteger}, {@link BigDecimal},
   *     or {@link Rational}.
   *
   * @return An arbitrary return value that is be passed to the caller.
   */
  R visitConstant(Formula f, Object value);

  /**
   * Visit an arbitrary, potentially uninterpreted function.
   *
   * @param f Input function.
   * @param args List of arguments
   * @param functionName Name of the function (such as "and" or "or")
   * @param newApplicationConstructor Construct a new function of the same type,
   *                                  with the new arguments as given.
   * @param isUninterpreted Special flag for UFs.
   */
  R visitFunction(
      Formula f,
      List<Formula> args,
      String functionName,
      Function<List<Formula>, Formula> newApplicationConstructor,
      boolean isUninterpreted);

  /**
   * Visit a quantified node.
   *
   * @param f Quantifier
   * @param quantifier Quantifier type: either {@code FORALL} or {@code EXISTS}.
   * @param body Body of the quantifier.
   * @param newBodyConstructor Constructor to replace a quantified body.
   */
  R visitQuantifier(
      BooleanFormula f,
      Quantifier quantifier,
      BooleanFormula body,
      Function<BooleanFormula, BooleanFormula> newBodyConstructor);
}
