package org.sosy_lab.solver.princess;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.solver.TermType;
import org.sosy_lab.solver.api.FormulaManager;
import org.sosy_lab.solver.api.InterpolatingProverEnvironment;
import org.sosy_lab.solver.api.OptEnvironment;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.api.SolverContext;
import org.sosy_lab.solver.basicimpl.AbstractSolverContext;

import javax.annotation.Nullable;

public final class PrincessSolverContext extends AbstractSolverContext {

  @Options(prefix = "solver.princess")
  static class PrincessOptions {
    @Option(
      secure = true,
      description =
          "The number of atoms a term has to have before"
              + " it gets abbreviated if there are more identical terms."
    )
    private int minAtomsForAbbreviation = 100;

    PrincessOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    public int getMinAtomsForAbbreviation() {
      return minAtomsForAbbreviation;
    }
  }

  private final ShutdownNotifier shutdownNotifier;
  private final PrincessFormulaManager manager;
  private final PrincessFormulaCreator creator;

  private PrincessSolverContext(
      Configuration config,
      LogManager logger,
      ShutdownNotifier shutdownNotifier,
      PrincessFormulaManager manager,
      PrincessFormulaCreator creator)
      throws InvalidConfigurationException {
    super(config, logger, manager);
    this.shutdownNotifier = shutdownNotifier;
    this.manager = manager;
    this.creator = creator;
  }

  public static SolverContext create(
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      @Nullable PathCounterTemplate pLogfileTemplate)
      throws InvalidConfigurationException {
    PrincessOptions options = new PrincessOptions(config);
    PrincessEnvironment env = new PrincessEnvironment(pLogfileTemplate, pShutdownNotifier, options);
    PrincessFormulaCreator creator =
        new PrincessFormulaCreator(env, TermType.Boolean, TermType.Integer);

    // Create managers
    PrincessUnsafeFormulaManager unsafeManager = new PrincessUnsafeFormulaManager(creator);
    PrincessFunctionFormulaManager functionTheory = new PrincessFunctionFormulaManager(creator);
    PrincessBooleanFormulaManager booleanTheory =
        new PrincessBooleanFormulaManager(creator, unsafeManager);
    PrincessIntegerFormulaManager integerTheory = new PrincessIntegerFormulaManager(creator);
    PrincessArrayFormulaManager arrayTheory = new PrincessArrayFormulaManager(creator);
    PrincessQuantifiedFormulaManager quantifierTheory =
        new PrincessQuantifiedFormulaManager(creator);
    PrincessFormulaManager manager =
        new PrincessFormulaManager(
            creator,
            unsafeManager,
            functionTheory,
            booleanTheory,
            integerTheory,
            arrayTheory,
            quantifierTheory);
    return new PrincessSolverContext(config, logger, pShutdownNotifier, manager, creator);
  }

  @Override
  public FormulaManager getFormulaManager() {
    return manager;
  }

  @Override
  public ProverEnvironment newProverEnvironment0(
      ShutdownNotifier pNotifier,
      boolean pGenerateModels, boolean pGenerateUnsatCore) {

    return new PrincessTheoremProver(manager, pNotifier);
  }

  @Override
  public InterpolatingProverEnvironment<?> newProverEnvironmentWithInterpolation0() {
    return new PrincessInterpolatingProver(manager);
  }

  @Override
  public OptEnvironment newOptEnvironment() {
    throw new UnsupportedOperationException("Princess does not support optimization");
  }

  @Override
  public String getVersion() {
    return creator.getEnv().getVersion();
  }

  @Override
  public void close() {}
}
