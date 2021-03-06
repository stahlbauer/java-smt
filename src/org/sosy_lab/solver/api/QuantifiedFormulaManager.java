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
package org.sosy_lab.solver.api;

import org.sosy_lab.solver.SolverException;

import java.util.List;

/**
 * This interface contains methods for working with any theory with quantifiers.
 *
 * <p>ATTENTION: Not every theory has a quantifier elimination property!
 */
public interface QuantifiedFormulaManager {

  enum Quantifier {
    FORALL,
    EXISTS
  }

  /**
   * @return An existentially quantified formula.
   *
   * @param pVariables  The variables that will get bound (variables) by the quantification.
   * @param pBody       The {@link BooleanFormula}} within that the binding will be performed.
   */
  BooleanFormula exists(List<? extends Formula> pVariables, BooleanFormula pBody);

  /**
   * @return A universally quantified formula.
   *
   * @param pVariables  The variables that will get bound (variables) by the quantification.
   * @param pBody       The {@link BooleanFormula}} within that the binding will be performed.
   */
  BooleanFormula forall(List<? extends Formula> pVariables, BooleanFormula pBody);

  /**
   * Eliminate the quantifiers for a given formula.
   *
   * @param pF Formula with quantifiers.
   * @return  New formula without quantifiers.
   */
  BooleanFormula eliminateQuantifiers(BooleanFormula pF)
      throws InterruptedException, SolverException;

  /**
   * @return Whether {@code pF} is a quantifier.
   */
  @Deprecated
  boolean isQuantifier(BooleanFormula pF);

  /**
   * @return Whether {@code pF} is a forall-quantifier.
   */
  @Deprecated
  boolean isForall(BooleanFormula pF);

  /**
   * @return Whether {@code pF} is an exists-quantifier.
   */
  @Deprecated
  boolean isExists(BooleanFormula pF);

  /**
   * @return Number of variables bound by the quantifier.
   */
  @Deprecated
  int numQuantifierBound(BooleanFormula pF);

  /**
   * @return Body of the quantifier.
   */
  @Deprecated
  BooleanFormula getQuantifierBody(BooleanFormula pF);

  /**
   * @return Whether a symbol {@code pF} is bound by a quantifier.
   * @deprecated use {@link UnsafeFormulaManager#isBoundVariable(Formula)} instead.
   */
  @Deprecated
  boolean isBoundByQuantifier(Formula pF);
}
