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
package org.sosy_lab.solver.princess;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;

import org.sosy_lab.solver.Model;
import org.sosy_lab.solver.SolverException;

abstract class PrincessAbstractProver {

  protected final PrincessStack stack;
  protected final PrincessFormulaManager mgr;
  protected boolean closed = false;

  protected PrincessAbstractProver(PrincessFormulaManager pMgr, boolean useForInterpolation) {
    this.mgr = pMgr;
    this.stack = checkNotNull(mgr.getEnvironment().getNewStack(useForInterpolation));
  }

  /** This function causes the SatSolver to check all the terms on the stack,
   * if their conjunction is SAT or UNSAT.
   */
  public boolean isUnsat() throws SolverException {
    Preconditions.checkState(!closed);
    return !stack.checkSat();
  }

  public abstract void pop();

  public abstract Model getModel() throws SolverException;

  public void close() {
    checkNotNull(stack);
    checkNotNull(mgr);
    stack.close();
    closed = true;
  }
}
