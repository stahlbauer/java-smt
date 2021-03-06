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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * An uninterpreted function.
 * This not a subclass of {@link Formula} because it does not represent a term
 * (only an <em>application</em> of an uninterpreted function is a formula term,
 * not the function itself).
 * @param <T> The type of the return value
 */
public abstract class UfDeclaration<T extends Formula> {

  private final FormulaType<T> returnType;
  private final List<FormulaType<?>> argumentTypes;

  protected UfDeclaration(FormulaType<T> pReturnType, List<FormulaType<?>> pArgumentTypes) {
    returnType = checkNotNull(pReturnType);
    argumentTypes = ImmutableList.copyOf(pArgumentTypes);
  }

  /**
   * The list of arguments.
   */
  public List<FormulaType<?>> getArgumentTypes() {
    return argumentTypes;
  }

  /**
   * The returntype.
   */
  public FormulaType<T> getReturnType() {
    return returnType;
  }

  @Override
  public String toString() {
    return "(" + returnType.toString() + ") func(" + Joiner.on(',').join(argumentTypes) + ")";
  }
}
