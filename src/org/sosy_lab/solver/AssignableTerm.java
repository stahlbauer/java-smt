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
package org.sosy_lab.solver;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

public interface AssignableTerm {
  TermType getType();

  String getName();

  class Variable implements AssignableTerm {
    private final String name;
    private final TermType type;

    public Variable(final String name, final TermType type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public TermType getType() {
      return type;
    }

    @Override
    public String toString() {
      return name + " : " + type;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, type);
    }

    @Override
    public boolean equals(@Nullable final Object other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      if (!getClass().equals(other.getClass())) {
        return false;
      }
      final Variable otherConstant = (Variable) other;
      return name.equals(otherConstant.name) && type.equals(otherConstant.type);
    }
  }

  /**
   * A function call can have a concrete return value.
   */
  class Function implements AssignableTerm {

    private final String mName;
    private final TermType mReturnType;
    private final List<Object> mArguments;
    private final int mHashCode;

    public Function(String pName, TermType pReturnType, Object[] pArguments) {
      mName = pName;
      mReturnType = pReturnType;
      mArguments = ImmutableList.copyOf(pArguments);
      mHashCode = Objects.hashCode(pName, pReturnType, Arrays.hashCode(pArguments));
    }

    @Override
    public String getName() {
      return mName;
    }

    @Override
    public TermType getType() {
      return mReturnType;
    }

    public int getArity() {
      return mArguments.size();
    }

    public Object getArgument(int lArgumentIndex) {
      return mArguments.get(lArgumentIndex);
    }

    @Override
    public String toString() {
      return mName + "(" + Joiner.on(',').join(mArguments) + ") : " + mReturnType;
    }

    @Override
    public int hashCode() {
      return mHashCode;
    }

    @Override
    public boolean equals(@Nullable Object pOther) {
      if (this == pOther) {
        return true;
      }

      if (pOther == null) {
        return false;
      }

      if (!getClass().equals(pOther.getClass())) {
        return false;
      }

      Function lFunction = (Function) pOther;

      return (lFunction.mName.equals(mName)
          && lFunction.mReturnType.equals(mReturnType)
          && lFunction.mArguments.equals(mArguments));
    }
  }
}
