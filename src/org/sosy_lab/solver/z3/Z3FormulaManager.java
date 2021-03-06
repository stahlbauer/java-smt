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
package org.sosy_lab.solver.z3;

import static org.sosy_lab.solver.z3.Z3NativeApi.parse_smtlib2_string;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.basicimpl.AbstractFormulaManager;
import org.sosy_lab.solver.basicimpl.tactics.Tactic;
import org.sosy_lab.solver.visitors.FormulaVisitor;

import java.io.IOException;

final class Z3FormulaManager extends AbstractFormulaManager<Long, Long, Long> {

  @SuppressWarnings("checkstyle:parameternumber")
  Z3FormulaManager(
      Z3FormulaCreator pFormulaCreator,
      Z3UnsafeFormulaManager pUnsafeManager,
      Z3FunctionFormulaManager pFunctionManager,
      Z3BooleanFormulaManager pBooleanManager,
      Z3IntegerFormulaManager pIntegerManager,
      Z3RationalFormulaManager pRationalManager,
      Z3BitvectorFormulaManager pBitpreciseManager,
      Z3QuantifiedFormulaManager pQuantifiedManager,
      Z3ArrayFormulaManager pArrayManager,
      ShutdownNotifier.ShutdownRequestListener pInterruptListener,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger)
      throws InvalidConfigurationException {
    super(
        pFormulaCreator,
        pUnsafeManager,
        pFunctionManager,
        pBooleanManager,
        pIntegerManager,
        pRationalManager,
        pBitpreciseManager,
        null,
        pQuantifiedManager,
        pArrayManager);
  }

  @Override
  public BooleanFormula parse(String str) throws IllegalArgumentException {

    // TODO do we need sorts or decls?
    // the context should know them already,
    // TODO check this
    long[] sortSymbols = new long[0];
    long[] sorts = new long[0];
    long[] declSymbols = new long[0];
    long[] decls = new long[0];
    long e = parse_smtlib2_string(getEnvironment(), str, sortSymbols, sorts, declSymbols, decls);

    return getFormulaCreator().encapsulateBoolean(e);
  }

  static long getZ3Expr(Formula pT) {
    if (pT instanceof Z3Formula) {
      return ((Z3Formula) pT).getFormulaInfo();
    }
    throw new IllegalArgumentException(
        "Cannot get the formula info of type " + pT.getClass().getSimpleName() + " in the Solver!");
  }

  private static final ImmutableMap<Tactic, String> Z3_TACTICS =
      ImmutableMap.<Tactic, String>builder()
          .put(Tactic.NNF, "nnf")
          .put(Tactic.CNF, "cnf")
          .put(Tactic.TSEITIN_CNF, "tseitin-cnf")
          .put(Tactic.QE, "qe")
          .put(Tactic.QE_LIGHT, "qe-light")
          .build();

  @Override
  protected Long applyTacticImpl(Long input, Tactic tactic) {
    String z3TacticName = Z3_TACTICS.get(tactic);
    if (z3TacticName != null) {
      return Z3NativeApiHelpers.applyTactic(getFormulaCreator().getEnv(), input, z3TacticName);
    } else {
      return super.applyTacticImpl(input, tactic);
    }
  }

  @Override
  public Appender dumpFormula(final Long expr) {
    assert getFormulaCreator().getFormulaType(expr) == FormulaType.BooleanType
        : "Only BooleanFormulas may be dumped";

    return new Appenders.AbstractAppender() {

      @Override
      public void appendTo(Appendable out) throws IOException {
        String txt =
            Z3NativeApi.benchmark_to_smtlib_string(
                getEnvironment(), "dumped-formula", "", "unknown", "", 0, new long[] {}, expr);

        for (String line : Splitter.on('\n').split(txt)) {

          if (line.startsWith("(set-info") || line.startsWith(";") || line.startsWith("(check")) {
            // ignore
          } else if (line.startsWith("(assert") || line.startsWith("(dec")) {
            out.append('\n');
            out.append(line);
          } else {
            // Z3 spans formulas over multiple lines, append to previous line
            out.append(' ');
            out.append(line.trim());
          }
        }
      }
    };
  }

  @Override
  protected Long simplify(Long pF) {
    return Z3NativeApi.simplify(getFormulaCreator().getEnv(), pF);
  }

  @Override
  public <R> R visit(FormulaVisitor<R> rFormulaVisitor, Formula f) {
    return getUnsafeFormulaManager().visit(rFormulaVisitor, f);
  }
}
