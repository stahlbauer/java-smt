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
package org.sosy_lab.solver.cvc4;

import edu.nyu.acsys.CVC4.Expr;
import edu.nyu.acsys.CVC4.ExprManager;
import edu.nyu.acsys.CVC4.FunctionType;
import edu.nyu.acsys.CVC4.Kind;
import edu.nyu.acsys.CVC4.Type;
import edu.nyu.acsys.CVC4.vectorExpr;
import edu.nyu.acsys.CVC4.vectorType;

import org.sosy_lab.solver.basicimpl.AbstractFunctionFormulaManager;
import org.sosy_lab.solver.basicimpl.FormulaCreator;

import java.util.List;

public class CVC4FunctionFormulaManager
    extends AbstractFunctionFormulaManager<Expr, Expr, Type, CVC4Environment> {

  private final ExprManager exprManager;

  protected CVC4FunctionFormulaManager(FormulaCreator<Expr, Type, CVC4Environment> pCreator) {
    super(pCreator);
    exprManager = pCreator.getEnv().getExprManager();
  }

  @Override
  protected Expr declareUninterpretedFunctionImpl(
      String pName, Type pReturnType, List<Type> pArgTypes) {
    vectorType argTypes = new vectorType();
    for (Type t : pArgTypes) {
      argTypes.add(t);
    }
    FunctionType functionType = exprManager.mkFunctionType(argTypes, pReturnType);
    return exprManager.mkVar(pName, functionType);
  }

  @Override
  protected Expr createUninterpretedFunctionCallImpl(Expr pFunc, List<Expr> pArgs) {
    vectorExpr args = new vectorExpr();
    for (Expr t : pArgs) {
      args.add(t);
    }
    return exprManager.mkExpr(Kind.APPLY_UF, pFunc, args);
  }
}
