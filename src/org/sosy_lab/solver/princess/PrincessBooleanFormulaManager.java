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

import static com.google.common.base.Verify.verify;
import static org.sosy_lab.solver.princess.PrincessUtil.castToFormula;
import static org.sosy_lab.solver.princess.PrincessUtil.castToTerm;

import ap.parser.IAtom;
import ap.parser.IBinFormula;
import ap.parser.IBinJunctor;
import ap.parser.IBoolLit;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IFormulaITE;
import ap.parser.IIntFormula;
import ap.parser.INot;
import ap.parser.IQuantified;
import ap.parser.ITermITE;
import ap.terfor.conjunctions.Quantifier.ALL$;

import com.google.common.collect.ImmutableList;

import org.sosy_lab.solver.TermType;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.QuantifiedFormulaManager;
import org.sosy_lab.solver.basicimpl.AbstractBooleanFormulaManager;
import org.sosy_lab.solver.visitors.BooleanFormulaVisitor;

import scala.Enumeration;

class PrincessBooleanFormulaManager
    extends AbstractBooleanFormulaManager<IExpression, TermType, PrincessEnvironment> {

  PrincessBooleanFormulaManager(
      PrincessFormulaCreator creator, PrincessUnsafeFormulaManager ufmgr) {
    super(creator, ufmgr);
  }

  @Override
  public IFormula makeVariableImpl(String varName) {
    return castToFormula(
        getFormulaCreator().makeVariable(getFormulaCreator().getBoolType(), varName));
  }

  @Override
  public IFormula makeBooleanImpl(boolean pValue) {
    return new IBoolLit(pValue);
  }

  @Override
  public IFormula equivalence(IExpression t1, IExpression t2) {
    return new IBinFormula(IBinJunctor.Eqv(), castToFormula(t1), castToFormula(t2));
  }

  @Override
  public boolean isTrue(IExpression t) {
    return PrincessUtil.isTrue(t);
  }

  @Override
  public boolean isFalse(IExpression t) {
    return PrincessUtil.isFalse(t);
  }

  @Override
  public IExpression ifThenElse(IExpression condition, IExpression t1, IExpression t2) {
    if (t1 instanceof IFormula) {
      return new IFormulaITE(castToFormula(condition), castToFormula(t1), castToFormula(t2));
    } else {
      return new ITermITE(castToFormula(condition), castToTerm(t1), castToTerm(t2));
    }
  }

  @Override
  public IFormula not(IExpression pBits) {
    if (isNot(pBits)) {
      return ((INot) pBits).subformula(); // "not not a" == "a"
    } else {
      return new INot(castToFormula(pBits));
    }
  }

  @Override
  public IFormula and(IExpression t1, IExpression t2) {
    if (t1.equals(t2)) {
      return castToFormula(t1);
    }
    if (PrincessUtil.isTrue(t1)) {
      return castToFormula(t2);
    }
    if (PrincessUtil.isTrue(t2)) {
      return castToFormula(t1);
    }
    return simplify(new IBinFormula(IBinJunctor.And(), castToFormula(t1), castToFormula(t2)));
  }

  @Override
  public IFormula or(IExpression t1, IExpression t2) {
    if (PrincessUtil.isFalse(t1)) {
      return castToFormula(t2);
    }
    if (PrincessUtil.isFalse(t2)) {
      return castToFormula(t1);
    }
    return simplify(new IBinFormula(IBinJunctor.Or(), castToFormula(t1), castToFormula(t2)));
  }

  /** simplification to avoid identical subgraphs: (a&b)&(a&c) --> a&(b&c), etc */
  private IFormula simplify(IFormula f) {
    if (f instanceof IBinFormula) {
      final IBinFormula bin = (IBinFormula) f;
      if (bin.f1() instanceof IBinFormula
          && bin.f2() instanceof IBinFormula
          && ((IBinFormula) bin.f1()).j().equals(((IBinFormula) bin.f2()).j())) {
        Enumeration.Value operator = ((IBinFormula) f).j();
        Enumeration.Value innerOperator = ((IBinFormula) bin.f1()).j();

        IFormula s11 = ((IBinFormula) bin.f1()).f1();
        IFormula s12 = ((IBinFormula) bin.f1()).f2();
        IFormula s21 = ((IBinFormula) bin.f2()).f1();
        IFormula s22 = ((IBinFormula) bin.f2()).f2();

        if (s11.equals(s21)) { // (ab)(ac) -> a(bc)
          return new IBinFormula(innerOperator, s11, new IBinFormula(operator, s12, s22));
        } else if (s11.equals(s22)) { // (ab)(ca) -> a(bc)
          return new IBinFormula(innerOperator, s11, new IBinFormula(operator, s12, s21));
        } else if (s12.equals(s21)) { // (ba)(ac) -> a(bc)
          return new IBinFormula(innerOperator, s12, new IBinFormula(operator, s11, s22));
        } else if (s12.equals(s22)) { // (ba)(ca) -> a(bc)
          return new IBinFormula(innerOperator, s12, new IBinFormula(operator, s11, s21));
        }
      }
    }

    // if we cannot simplify the formula, we create an abbreviation
    // return getFormulaCreator().getEnv().abbrev(f);
    return f;
  }

  @Override
  public IFormula xor(IExpression t1, IExpression t2) {
    return new INot(new IBinFormula(IBinJunctor.Eqv(), castToFormula(t1), castToFormula(t2)));
  }

  @Override
  public boolean isNot(IExpression pBits) {
    return pBits instanceof INot;
  }

  @Override
  public boolean isAnd(IExpression pBits) {
    return PrincessUtil.isAnd(pBits);
  }

  @Override
  public boolean isOr(IExpression pBits) {
    return PrincessUtil.isOr(pBits);
  }

  @Override
  public boolean isXor(IExpression pBits) {
    return PrincessUtil.isXor(pBits);
  }

  @Override
  protected boolean isEquivalence(IExpression pBits) {
    return PrincessUtil.isEquivalence(pBits);
  }

  @Override
  protected boolean isImplication(IExpression pFormula) {
    return PrincessUtil.isImplication(pFormula);
  }

  @Override
  protected boolean isIfThenElse(IExpression pBits) {
    return PrincessUtil.isIfThenElse(pBits);
  }

  private BooleanFormula getArg(IFormula f, int i) {
    return getFormulaCreator().encapsulateBoolean(f.apply(i));
  }

  @Override
  protected <R> R visit(BooleanFormulaVisitor<R> pVisitor, IExpression pF) {
    verify(
        pF instanceof IFormula,
        "Unexpected boolean formula of class %s",
        pF.getClass().getSimpleName());
    final IFormula f = (IFormula) pF;

    if (f instanceof IBoolLit) {
      if (((IBoolLit) f).value()) {
        return pVisitor.visitTrue();
      } else {
        return pVisitor.visitFalse();
      }

    } else if (f instanceof INot) {
      return pVisitor.visitNot(getArg(f, 0));

    } else if (f instanceof IBinFormula) {
      final IBinFormula bin = (IBinFormula) f;

      if (IBinJunctor.And() == bin.j()) {
        return pVisitor.visitAnd(ImmutableList.of(getArg(f, 0), getArg(f, 1)));

      } else if (IBinJunctor.Or() == bin.j()) {
        return pVisitor.visitOr(ImmutableList.of(getArg(f, 0), getArg(f, 1)));

      } else if (IBinJunctor.Eqv() == bin.j()) {
        return pVisitor.visitEquivalence(getArg(f, 0), getArg(f, 1));

      } else {
        throw new UnsupportedOperationException(
            "Unknown or unsupported boolean operator " + bin.j());
      }

    } else if (f instanceof IFormulaITE) {
      return pVisitor.visitIfThenElse(getArg(f, 0), getArg(f, 1), getArg(f, 2));

    } else if (f instanceof IQuantified) {
      IQuantified q = (IQuantified) f;
      QuantifiedFormulaManager.Quantifier t =
          (q.quan() == ALL$.MODULE$)
              ? QuantifiedFormulaManager.Quantifier.FORALL
              : QuantifiedFormulaManager.Quantifier.EXISTS;
      return pVisitor.visitQuantifier(
          t, getFormulaCreator().encapsulateBoolean(PrincessUtil.getQuantifierBody(q)));

    } else if (f instanceof IAtom || f instanceof IIntFormula) {
      return pVisitor.visitAtom(getFormulaCreator().encapsulateBoolean(f));
    }

    throw new UnsupportedOperationException(
        String.format(
            "Unknown or unsupported boolean operator of class %s: %s",
            f.getClass().getSimpleName(),
            f));
  }
}
