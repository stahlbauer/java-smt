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

import java.util.List;

/**
 * Manager for dealing with UFs.
 */
public interface FunctionFormulaManager {

  /**
   * Declare an uninterpreted function.
   */
  <T extends Formula> UfDeclaration<T> declareUninterpretedFunction(
      String name, FormulaType<T> returnType, List<FormulaType<?>> args);

  /**
   * Declare an uninterpreted function.
   */
  <T extends Formula> UfDeclaration<T> declareUninterpretedFunction(
      String name, FormulaType<T> returnType, FormulaType<?>... args);

  /**
   * Create an uninterpreted function call.
   *
   * @param funcType Declaration of the function to call.
   * @param args Arguments of the function.
   * @return Instantiated function call.
   */
  <T extends Formula> T callUninterpretedFunction(
      UfDeclaration<T> funcType, List<? extends Formula> args);

  /**
   * Declares and calls an uninterpreted function.
   */
  <T extends Formula> T declareAndCallUninterpretedFunction(
      String name, FormulaType<T> pReturnType, List<Formula> pArgs);
}
