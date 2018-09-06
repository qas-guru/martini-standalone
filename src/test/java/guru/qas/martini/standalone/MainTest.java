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

package guru.qas.martini.standalone;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.annotations.Test;

import com.beust.jcommander.JCommander;
import com.google.common.collect.Multimap;

import guru.qas.martini.standalone.harness.GatedMartiniComparator;
import guru.qas.martini.standalone.harness.Options;
import guru.qas.martini.standalone.jcommander.CommandLineOptions;
import guru.qas.martini.standalone.test.spring.TestListener;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class MainTest {

	protected final Logger logger;

	protected MainTest() {
		logger = LoggerFactory.getLogger(getClass());
	}

	@Test
	public void testJsonOutput() throws IOException {
		File tmpFile = File.createTempFile("test", "json");
		try {
			String path = tmpFile.getAbsolutePath();
			String[] argv = new String[]{"-jsonOutputFile", path};
			Main application = getApplication(argv);
			Options options = getOptions(application);
			File jsonOutputOption = options.getJsonOutputFile().orElse(null);
			checkNotNull(jsonOutputOption, "options has null file");
			checkState(tmpFile.equals(jsonOutputOption),
				"options has wrong file; expected {} but got {}", tmpFile, jsonOutputOption);
		}
		finally {
			if (!tmpFile.delete()) {
				logger.warn("unable to delete temporary test file {}", tmpFile);
			}
		}
	}

	@Test
	public void testSpelFilter() {
		String[] argv = new String[]{"-spelFilter", "isWIP()", "&&", "!isProvisional()"};
		Main application = getApplication(argv);

		Options options = getOptions(application);
		String spelFilter = options.getSpelFilter()
			.orElseThrow(() -> new IllegalStateException("no spelFilter recognized"));

		String joined = "isWIP() && !isProvisional()";
		checkArgument(joined.equals(spelFilter), "wrong spelFilter; expected %s but found %s", joined, spelFilter);
	}

	@Test
	public void testMultiThreaded() throws Exception {
		Multimap<String, String> executionIndex = executeWithParallelism(10);

		Set<String> threadNames = executionIndex.keySet();
		checkState(1 < threadNames.size(), "all tests run by a single thread");

		assertScenariosExecutedOnce(executionIndex);
	}

	@Test
	public void testSingleThreaded() throws Exception {
		Multimap<String, String> executionIndex = executeWithParallelism(1);

		Set<String> threadNames = executionIndex.keySet();
		int threadCount = threadNames.size();
		checkState(1 == threadCount, "tests executed by more than one thread: %s", threadCount);

		assertScenariosExecutedOnce(executionIndex);
	}

	protected Multimap<String, String> executeWithParallelism(int parallelism) throws ExecutionException, InterruptedException {
		String[] argv = new String[]{
			"-parallelism", String.valueOf(parallelism),
			"-martiniComparatorImplementation", GatedMartiniComparator.class.getName(),
			"-configLocations", "classpath*:**/applicationContext.xml,classpath*:/bogus.xml"};
		Main application = getApplication(argv);

		Multimap<String, String> executionIndex;

		try (ConfigurableApplicationContext context = application.getApplicationContext()) {
			TestListener listener = context.getBean(TestListener.class);
			application.executeSuite(context);
			executionIndex = listener.getExecutionIndex();
		}

		checkState(!executionIndex.isEmpty(), "no AfterScenarioEvent objects handled");
		return executionIndex;
	}

	protected Main getApplication(String[] argv) {
		CommandLineOptions args = new CommandLineOptions();
		JCommander.newBuilder().addObject(args).build().parse(argv);
		return new Main(args);
	}

	protected Options getOptions(Main application) {
		ConfigurableApplicationContext context = application.getApplicationContext();
		return context.getBean(Options.class);
	}

	protected void assertScenariosExecutedOnce(Multimap<String, String> executionIndex) {
		Collection<String> scenarios = executionIndex.values();
		int scenariosExecuted = scenarios.size();

		Set<String> uniq = new HashSet<>(scenarios);
		int uniqueScenarios = uniq.size();
		checkState(uniqueScenarios == scenariosExecuted, "scenarios executed more than once");
	}
}