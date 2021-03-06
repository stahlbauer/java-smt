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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.common.ChildFirstPatternClassLoader;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.solver.api.FormulaManager;
import org.sosy_lab.solver.api.SolverContext;
import org.sosy_lab.solver.mathsat5.Mathsat5SolverContext;
import org.sosy_lab.solver.princess.PrincessSolverContext;
import org.sosy_lab.solver.z3.Z3SolverContext;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Factory class for loading and instantiating SMT solvers:
 * instantiates and loads a {@link SolverContext} corresponding to the chosen
 * solver.
 *
 * <p>Main entry point.
 */
@Options(prefix = "solver", deprecatedPrefix = "cpa.predicate")
public class SolverContextFactory {

  @VisibleForTesting
  public enum Solvers {
    MATHSAT5,
    SMTINTERPOL,
    Z3,
    PRINCESS
  }

  @Option(secure = true, description = "Export solver queries in SmtLib format into a file.")
  private boolean logAllQueries = false;

  @Option(secure = true, description = "Export solver queries in SmtLib format into a file.")
  @FileOption(Type.OUTPUT_FILE)
  private @Nullable PathCounterTemplate logfile =
      PathCounterTemplate.ofFormatString("smtquery.%03d.smt2");

  @Option(secure = true, description = "Random seed for SMT solver.")
  private long randomSeed = 42;

  @Option(secure = true, description = "Which SMT solver to use.")
  private Solvers solver = Solvers.SMTINTERPOL;

  @Option(
    secure = true,
    description =
        "Which solver to use specifically for interpolation (default is to use the main one)."
  )
  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
  private @Nullable Solvers interpolationSolver = null;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final SolverContext context;
  private final SolverContext itpContext;

  public SolverContextFactory(
      Configuration config, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pShutdownNotifier);

    if (!logAllQueries) {
      logfile = null;
    }

    if (solver.equals(interpolationSolver)) {
      // If interpolationSolver is not null, we use SeparateInterpolatingProverEnvironment
      // which copies formula from and to the main solver using string serialization.
      // We don't need this if the solvers are the same anyway.
      interpolationSolver = null;
    }

    context = instantiateSolver(solver, config);

    // Instantiate another SMT solver for interpolation if requested.
    if (interpolationSolver != null) {
      itpContext = instantiateSolver(interpolationSolver, config);
    } else {
      itpContext = context;
    }
  }

  private SolverContext instantiateSolver(Solvers solverToCreate, Configuration config)
      throws InvalidConfigurationException {
    try {
      switch (solverToCreate) {
        case SMTINTERPOL:

          // Loading SmtInterpol is difficult as it requires its own class
          // loader.
          return loadSmtInterpol().create(config, logger, shutdownNotifier, logfile, randomSeed);

        case MATHSAT5:
          return Mathsat5SolverContext.create(
              logger, config, shutdownNotifier, logfile, randomSeed);

        case Z3:
          return Z3SolverContext.create(logger, config, shutdownNotifier, logfile, randomSeed);

        case PRINCESS:
          // TODO: pass randomSeed to Princess
          return PrincessSolverContext.create(config, logger, shutdownNotifier, logfile);

        default:
          throw new AssertionError("no solver selected");
      }

    } catch (UnsatisfiedLinkError e) {
      throw new InvalidConfigurationException(
          String.format(
              "The SMT solver %s is not available on this machine because of missing libraries "
                  + "(%s). "
                  + "You may experiment with SMTInterpol by setting solver.solver=SMTInterpol.",
              solverToCreate,
              e.getMessage()),
          e);
    }
  }

  /**
   * Shortcut for getting a {@link FormulaManager}.
   *
   * <p>See
   * {@link #SolverContextFactory(Configuration, LogManager, ShutdownNotifier)}
   * for documentation of accepted parameters.
   */
  public static SolverContext createFormulaManager(
      Configuration config, LogManager logger, ShutdownNotifier shutdownNotifier)
      throws InvalidConfigurationException {
    return new SolverContextFactory(config, logger, shutdownNotifier).getSolverContext();
  }

  public SolverContext getSolverContext() {
    return context;
  }

  public SolverContext getSolverContextForInterpolation() {
    return itpContext;
  }

  /**
   * Interface for completely encapsulating all accesses to a solver's package
   * to decouple the solver's package from the rest of the code.
   *
   * <p>This interface is only meant to be implemented by SMT solvers
   * and used by this class, not by other classes.
   */
  public interface InnerUtilFactory {
    SolverContext create(
        Configuration config,
        LogManager logger,
        ShutdownNotifier pShutdownNotifier,
        @Nullable PathCounterTemplate solverLogfile,
        long randomSeed)
        throws InvalidConfigurationException;
  }

  // ------------------------- SmtInterpol -------------------------
  // For SmtInterpol we need a separate class loader
  // because it needs it's own (modified) version of the Java CUP runtime
  // and we might already have the normal (unmodified) version of Java CUP
  // on the class path of the normal class loader.
  private static final Pattern SMTINTERPOL_CLASSES =
      Pattern.compile(
          "^("
              + "org\\.sosy_lab\\.solver\\.smtinterpol|"
              + "de\\.uni_freiburg\\.informatik\\.ultimate|"
              + "java_cup\\.runtime|"
              + "org\\.apache\\.log4j"
              + ")\\..*");
  private static final String SMTINTERPOL_FACTORY_CLASS =
      "org.sosy_lab.solver.smtinterpol.SmtInterpolSolverFactory";

  // We keep the class loader for SmtInterpol around
  // in case someone creates a second instance of FormulaManagerFactory
  private static WeakReference<ClassLoader> smtInterpolClassLoader = new WeakReference<>(null);
  private static final AtomicInteger smtInterpolLoadingCount = new AtomicInteger(0);

  private InnerUtilFactory loadSmtInterpol() {
    try {
      ClassLoader classLoader = getClassLoaderForSmtInterpol(logger);

      @SuppressWarnings("unchecked")
      Class<? extends InnerUtilFactory> factoryClass =
          (Class<? extends InnerUtilFactory>) classLoader.loadClass(SMTINTERPOL_FACTORY_CLASS);
      Constructor<? extends InnerUtilFactory> factoryConstructor = factoryClass.getConstructor();
      return factoryConstructor.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new Classes.UnexpectedCheckedException("Failed to load SmtInterpol", e);
    }
  }

  private static ClassLoader getClassLoaderForSmtInterpol(LogManager logger) {
    ClassLoader classLoader = smtInterpolClassLoader.get();
    if (classLoader != null) {
      return classLoader;
    }

    // Garbage collected on first entry.
    if (smtInterpolLoadingCount.incrementAndGet() > 1) {
      logger.log(Level.INFO, "Repeated loading of SmtInterpol");
    }

    classLoader = SolverContextFactory.class.getClassLoader();
    if (classLoader instanceof URLClassLoader) {

      // Filter out java-cup-runtime.jar from the class path,
      // so that the class loader for SmtInterpol loads the Java CUP classes
      // from SmtInterpol's JAR file.
      URL[] urls =
          from(Arrays.asList(((URLClassLoader) classLoader).getURLs()))
              .filter(
                  new Predicate<URL>() {
                    @Override
                    public boolean apply(@Nonnull URL pInput) {
                      return !pInput.getPath().contains("java-cup");
                    }
                  })
              .toArray(URL.class);

      classLoader = new ChildFirstPatternClassLoader(SMTINTERPOL_CLASSES, urls, classLoader);
    }
    smtInterpolClassLoader = new WeakReference<>(classLoader);
    return classLoader;
  }
}
