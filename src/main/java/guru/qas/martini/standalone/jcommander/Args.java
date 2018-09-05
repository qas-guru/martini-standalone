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
import java.util.List;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

@SuppressWarnings({"ConstantConditions", "FieldCanBeLocal"})
public class Args {

	@Parameter(names = {"-h", "--h", "-help", "--help"}, help = true)
	public boolean help;

	@Parameter(
		names = "-jsonOverwrite",
		description = "overwrites existing JSON output",
		arity = 1
	)
	public boolean jsonOverwrite = true;

	@Parameter(
		names = {"-jsonOutputFile"},
		description = "JSON output file location for suite reporting, e.g. /tmp/martini.json")
	public File jsonOutputFile;

	@Parameter(
		names = "-configLocations",
		variableArity = true,
		description = "list of Spring configuration files")
	public List<String> configLocations = Lists.newArrayList("classpath*:**/applicationContext.xml");

	@Parameter(
		names = "-spelFilter",
		description = "Spring SPel expression indicating which scenarios should be executed")
	public String spelFilter;

	@Parameter(
		names = {"-parallelism"},
		description = "Fork Join Pool parallelism (defaulted to available processors); must be greater than zero",
		validateValueWith = GreaterThanZeroValidator.class
	)
	public int parallelism = Runtime.getRuntime().availableProcessors();

	@Parameter(
		names = {"-awaitTerminationSeconds", "martini.engine.await.termination.seconds"},
		description = "number of seconds Fork Join Pool will wait before forcing termination; must be greater than zero",
		validateValueWith = GreaterThanZeroValidator.class
	)
	public int awaitTerminationSeconds = 5 * 60;

	@Parameter(
		names = {"-timeoutInMinutes", "martini.engine.timeout.minutes"},
		description = "period of time after which suite should exit; must be greater than zero",
		validateValueWith = GreaterThanZeroValidator.class
	)
	public int timeoutInMinutes = 60 * 12;
}
