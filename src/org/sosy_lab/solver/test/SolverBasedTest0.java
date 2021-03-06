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
package org.sosy_lab.solver.test;

import static com.google.common.truth.Truth.assert_;
import static com.google.common.truth.TruthJUnit.assume;
import static org.sosy_lab.solver.test.ProverEnvironmentSubject.proverEnvironment;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.junit.After;
import org.junit.Before;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.solver.SolverContextFactory;
import org.sosy_lab.solver.SolverContextFactory.Solvers;
import org.sosy_lab.solver.api.ArrayFormulaManager;
import org.sosy_lab.solver.api.BasicProverEnvironment;
import org.sosy_lab.solver.api.BitvectorFormulaManager;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.FormulaManager;
import org.sosy_lab.solver.api.FunctionFormulaManager;
import org.sosy_lab.solver.api.IntegerFormulaManager;
import org.sosy_lab.solver.api.QuantifiedFormulaManager;
import org.sosy_lab.solver.api.RationalFormulaManager;
import org.sosy_lab.solver.api.SolverContext;

import javax.annotation.Nullable;

/**
 * Abstract base class with helpful utilities for writing tests
 * that use an SMT solver.
 * It instantiates and closes the SMT solver before and after each test,
 * and provides fields with direct access to the most relevant instances.
 *
 * <p>To run the tests using all available solvers, add the following code to your class:
 * <pre>
 * <code>
 *  {@literal @}Parameters(name="{0}")
 *  public static List{@literal <Object[]>} getAllSolvers() {
 *    return allSolversAsParameters();
 *  }
 *
 *  {@literal @}Parameter(0)
 *  public Solvers solver;
 *
 *  {@literal @}Override
 *  protected Solvers solverToUse() {
 *    return solver;
 *  }
 * </code>
 * </pre>
 *
 * {@link #assertThatFormula(BooleanFormula)} can be used to easily write assertions
 * about formulas using Truth.
 *
 * <p>Test that rely on a theory that not all solvers support
 * should call one of the {@code require} methods at the beginning.
 */
@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "test code")
public abstract class SolverBasedTest0 {

  protected Configuration config;
  protected final LogManager logger = TestLogManager.getInstance();

  protected SolverContextFactory factory;
  protected SolverContext context;
  protected FormulaManager mgr;
  protected BooleanFormulaManager bmgr;
  protected FunctionFormulaManager fmgr;
  protected IntegerFormulaManager imgr;
  protected @Nullable RationalFormulaManager rmgr;
  protected @Nullable BitvectorFormulaManager bvmgr;
  protected @Nullable QuantifiedFormulaManager qmgr;
  protected @Nullable ArrayFormulaManager amgr;

  /**
   * Return the solver to use in this test.
   * The default is SMTInterpol because it's the only solver guaranteed on all platforms.
   * Overwrite to specify a different solver.
   */
  protected Solvers solverToUse() {
    return Solvers.SMTINTERPOL;
  }

  protected ConfigurationBuilder createTestConfigBuilder() {
    return Configuration.builder().setOption("solver.solver", solverToUse().toString());
  }

  @Before
  public final void initSolver() throws Exception {
    config = createTestConfigBuilder().build();

    factory = new SolverContextFactory(config, logger, ShutdownManager.create().getNotifier());
    context = factory.getSolverContext();
    mgr = context.getFormulaManager();

    fmgr = mgr.getFunctionFormulaManager();
    bmgr = mgr.getBooleanFormulaManager();
    imgr = mgr.getIntegerFormulaManager();
    try {
      rmgr = mgr.getRationalFormulaManager();
    } catch (UnsupportedOperationException e) {
      rmgr = null;
    }
    try {
      bvmgr = mgr.getBitvectorFormulaManager();
    } catch (UnsupportedOperationException e) {
      bvmgr = null;
    }
    try {
      qmgr = mgr.getQuantifiedFormulaManager();
    } catch (UnsupportedOperationException e) {
      qmgr = null;
    }
    try {
      amgr = mgr.getArrayFormulaManager();
    } catch (UnsupportedOperationException e) {
      amgr = null;
    }
  }

  @After
  public final void closeSolver() throws Exception {
    if (mgr instanceof AutoCloseable) {
      ((AutoCloseable) mgr).close();
    }
  }

  /**
   * Skip test if the solver does not support rationals.
   */
  protected final void requireRationals() {
    assume()
        .withFailureMessage("Solver " + solverToUse() + " does not support the theory of rationals")
        .that(rmgr)
        .isNotNull();
  }

  /**
   * Skip test if the solver does not support bitvectors.
   */
  protected final void requireBitvectors() {
    assume()
        .withFailureMessage(
            "Solver " + solverToUse() + " does not support the theory of bitvectors")
        .that(bvmgr)
        .isNotNull();
  }
  /**
   * Skip test if the solver does not support quantifiers.
   */
  protected final void requireQuantifiers() {
    assume()
        .withFailureMessage("Solver " + solverToUse() + " does not support quantifiers")
        .that(qmgr)
        .isNotNull();
  }

  /**
   * Skip test if the solver does not support arrays.
   */
  protected final void requireArrays() {
    assume()
        .withFailureMessage("Solver " + solverToUse() + " does not support the theory of arrays")
        .that(amgr)
        .isNotNull();
  }

  /**
   * Skip test if the solver does not support optimization.
   */
  protected final void requireOptimization() {
    try {
      context.newOptEnvironment().close();
    } catch (UnsupportedOperationException e) {
      assume()
          .withFailureMessage("Solver " + solverToUse() + " does not support optimization")
          .that(e)
          .isNull();
    }
  }

  /**
   * Use this for checking assertions about BooleanFormulas with Truth:
   * <code>assertThatFormula(formula).is...()</code>.
   */
  protected final BooleanFormulaSubject assertThatFormula(BooleanFormula formula) {
    return assert_().about(BooleanFormulaSubject.forSolver(context)).that(formula);
  }

  /**
   * Use this for checking assertions about ProverEnvironments with Truth:
   * <code>assertThatEnvironment(stack).is...()</code>.
   */
  protected final ProverEnvironmentSubject assertThatEnvironment(BasicProverEnvironment<?> prover) {
    return assert_().about(proverEnvironment()).that(prover);
  }
}
