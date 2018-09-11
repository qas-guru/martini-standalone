/*
Copyright 2017-2018 Penny Rohr Curich

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package guru.qas.martini.standalone.jcommander;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.beust.jcommander.Parameter;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import guru.qas.martini.Martini;
import guru.qas.martini.event.DefaultSuiteIdentifier;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.spring.standalone.GatedMartiniComparator;
import guru.qas.martini.standalone.harness.DefaultMartiniStandaloneEngine;
import guru.qas.martini.standalone.harness.DefaultTaskFactory;
import guru.qas.martini.standalone.harness.DefaultUncaughtExceptionHandler;
import guru.qas.martini.standalone.harness.MartiniStandaloneEngine;
import guru.qas.martini.standalone.harness.Options;
import guru.qas.martini.standalone.harness.TaskFactory;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings({"ConstantConditions", "FieldCanBeLocal"})
public class CommandLineOptions implements Options {

	@Parameter(names = {"-h", "--h", "-help", "--help"}, help = true)
	protected boolean help;

	@Parameter(
		names = "-configLocations",
		variableArity = true,
		description = "list of Spring configuration files")
	protected List<String> configLocations = Lists.newArrayList("classpath*:**/applicationContext.xml");

	@Parameter(
		names = "-jsonOverwrite",
		description = "overwrites existing JSON output"
	)
	protected boolean jsonOverwrite = true;

	@Parameter(
		names = "-jsonOutputFile",
		description = "JSON output file location for suite reporting, e.g. /tmp/martini.json")
	protected File jsonOutputFile;

	@Parameter(
		names = "-spelFilter",
		variableArity = true,
		description = "Spring SPel expression indicating which scenarios should be executed")
	protected List<String> spelFilter;

	@Parameter(
		names = "-parallelism",
		description = "Fork Join Pool parallelism (defaulted to available processors); must be greater than zero",
		validateValueWith = GreaterThanZeroValidator.class
	)
	protected int parallelism = Runtime.getRuntime().availableProcessors();

	@Parameter(
		names = "-unimplementedStepsFatal",
		description = "true to prevent execution when unimplemented steps are detected"
	)
	protected boolean unimplementedStepsFatal = false;

	@Parameter(
		names = "-awaitTerminationS",
		description = "number of seconds Fork Join Pool will wait before forcing termination; must be greater than zero",
		validateValueWith = GreaterThanZeroValidator.class
	)
	protected Long awaitTerminationSeconds = (long) 5 * 60;

	@Parameter(
		names = "-timeoutInMinutes",
		description = "period of time after which suite should exit; must be greater than zero",
		validateValueWith = GreaterThanZeroValidator.class
	)
	protected Long timeoutInMinutes = (long) (60 * 12);

	@Parameter(
		names = "-jobPoolPollIntervalMs",
		description = "number of milliseconds between queued job check of Fork Join Pool",
		validateValueWith = GreaterThanZeroValidator.class
	)
	protected long jobPoolPollIntervalMs = (long) 250;

	@Parameter(
		names = "-martiniGateMonitorPollTimeoutMs",
		description = "number of milliseconds to wait for the gate monitor",
		validateValueWith = GreaterThanZeroValidator.class
	)
	protected long martiniGateMonitorPollTimeout = (long) 500;

	@Parameter(
		names = "-gatedMartiniComparatorImplementation",
		arity = 1,
		converter = ClassConverter.class,
		description = "gated Martini comparator implementation"
	)
	Class<? extends Comparator<Martini>> gatedComparatorImplementation = GatedMartiniComparator.class;

	@Parameter(
		names = "-engineImplementation",
		arity = 1,
		converter = ClassConverter.class,
		description = "MartiniStandaloneEngine implementation"
	)
	Class<? extends MartiniStandaloneEngine> engineImplementation = DefaultMartiniStandaloneEngine.class;

	@Parameter(
		names = "-suiteIdentifierImplementation",
		arity = 1,
		converter = ClassConverter.class,
		description = "SuiteIdentifier implementation"
	)
	Class<? extends SuiteIdentifier> suiteIdentifierImplementation = DefaultSuiteIdentifier.class;

	@Parameter(
		names = "-taskFactoryImplementation",
		arity = 1,
		converter = ClassConverter.class,
		description = "TaskFactory implementation"
	)
	Class<? extends TaskFactory> taskFactoryImplementation = DefaultTaskFactory.class;

	@Parameter(
		names = "-uncaughtExceptionHandlerImplementation",
		arity = 1,
		converter = ClassConverter.class,
		description = "Thread.UncaughtExceptionHandler implementation"
	)
	Class<? extends Thread.UncaughtExceptionHandler> uncaughtExceptionHandlerImplementation =
		DefaultUncaughtExceptionHandler.class;

	public boolean isHelp() {
		return help;
	}

	@Nonnull
	@Override
	public String[] getSpringConfigurationLocations() {
		List<String> locations = configLocations.stream()
			.map(location -> null == location ? "" : location.trim())
			.filter(location -> !location.isEmpty())
			.collect(Collectors.toList());
		checkArgument(!locations.isEmpty(), "-configLocations");

		return new LinkedHashSet<>(locations).toArray(new String[0]);
	}

	@Override
	public boolean isUnimplementedStepsFatal() {
		return unimplementedStepsFatal;
	}

	@Override
	public Optional<Long> getTimeoutInMinutes() {
		return Optional.ofNullable(timeoutInMinutes);
	}

	@Override
	public Optional<String> getSpelFilter() {
		String joined = null == spelFilter ? "" : Joiner.on(' ').skipNulls().join(spelFilter).trim();
		return Optional.ofNullable(joined.isEmpty() ? null : joined);
	}

	@Override
	public long getJobPoolPollIntervalMs() {
		return jobPoolPollIntervalMs;
	}

	@Override
	public Optional<File> getJsonOutputFile() {
		return Optional.ofNullable(jsonOutputFile);
	}

	@Override
	public int getParallelism() {
		return parallelism;
	}

	@Override
	public Optional<Long> getAwaitTerminationSeconds() {
		return Optional.ofNullable(awaitTerminationSeconds);
	}

	@Override
	public boolean isJsonOutputFileOverwrite() {
		return jsonOverwrite;
	}

	@Nonnull
	@Override
	public Class<? extends Comparator<Martini>> getGatedMartiniComparatorImplementation() {
		return gatedComparatorImplementation;
	}

	@Nonnull
	@Override
	public Class<? extends MartiniStandaloneEngine> getEngineImplementation() {
		return engineImplementation;
	}

	@Nonnull
	@Override
	public Class<? extends SuiteIdentifier> getSuiteIdentifierImplementation() {
		return suiteIdentifierImplementation;
	}

	@Nonnull
	@Override
	public Class<? extends TaskFactory> getTaskFactoryImplementation() {
		return taskFactoryImplementation;
	}

	@Nonnull
	@Override
	public Class<? extends Thread.UncaughtExceptionHandler> getUncaughtExceptionHandlerImplementation() {
		return uncaughtExceptionHandlerImplementation;
	}

	@Override
	public long getMartiniGatePollTimeoutMs() {
		return martiniGateMonitorPollTimeout;
	}
}