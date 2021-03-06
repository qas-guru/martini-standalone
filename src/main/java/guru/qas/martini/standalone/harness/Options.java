/*
Copyright 2018 Penny Rohr Curich

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

package guru.qas.martini.standalone.harness;

import java.io.File;
import java.util.Comparator;
import java.util.Optional;

import javax.annotation.Nonnull;

import guru.qas.martini.Martini;
import guru.qas.martini.event.SuiteIdentifier;

public interface Options {

	@Nonnull
	String[] getSpringConfigurationLocations();

	boolean isUnimplementedStepsFatal();

	Optional<Long> getTimeoutInMinutes();

	Optional<String> getSpelFilter();

	long getJobPoolPollIntervalMs();

	Optional<File> getJsonOutputFile();

	int getParallelism();

	Optional<Long> getAwaitTerminationSeconds();

	boolean isJsonOutputFileOverwrite();

	@Nonnull
	Class<? extends Comparator<Martini>> getMartiniComparatorImplementation();

	@Nonnull
	Class<? extends MartiniStandaloneEngine> getEngineImplementation();

	@Nonnull
	Class<? extends SuiteIdentifier> getSuiteIdentifierImplementation();

	@Nonnull
	Class<? extends TaskFactory> getTaskFactoryImplementation();

	@Nonnull
	Class<? extends Thread.UncaughtExceptionHandler> getUncaughtExceptionHandlerImplementation();

	long getMartiniGatePollTimeoutMs();
}
