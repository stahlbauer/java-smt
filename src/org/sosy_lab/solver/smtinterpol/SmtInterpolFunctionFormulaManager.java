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
package org.sosy_lab.solver.smtinterpol;

import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;

import org.sosy_lab.solver.basicimpl.AbstractFunctionFormulaManager;

import java.util.List;

class SmtInterpolFunctionFormulaManager
    extends AbstractFunctionFormulaManager<Term, String, Sort, SmtInterpolEnvironment> {

  private final SmtInterpolUnsafeFormulaManager unsafeManager;

  SmtInterpolFunctionFormulaManager(
      SmtInterpolFormulaCreator creator, SmtInterpolUnsafeFormulaManager unsafeManager) {
    super(creator);
    this.unsafeManager = unsafeManager;
  }

  @Override
  public Term createUninterpretedFunctionCallImpl(String funcDecl, List<Term> pArgs) {
    Term[] args = SmtInterpolUtil.toTermArray(pArgs);
    return unsafeManager.createUIFCallImpl(funcDecl, args);
  }

  @Override
  protected String declareUninterpretedFunctionImpl(
      String pName, Sort returnType, List<Sort> pArgs) {
    Sort[] types = pArgs.toArray(new Sort[pArgs.size()]);
    getFormulaCreator().getEnv().declareFun(pName, types, returnType);

    return pName;
  }
}
