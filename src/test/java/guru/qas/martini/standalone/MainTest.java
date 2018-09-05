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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.context.ConfigurableApplicationContext;
import org.testng.annotations.Test;

import com.beust.jcommander.JCommander;
import com.google.common.collect.Multimap;

import guru.qas.martini.standalone.harness.GatedMartiniComparator;
import guru.qas.martini.standalone.harness.Options;
import guru.qas.martini.standalone.jcommander.CommandLineOptions;
import guru.qas.martini.standalone.test.TestListener;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class MainTest {

	@Test
	public void testSpelFilter() {
		String[] argv = new String[]{"-spelFilter", "isWIP()", "&&", "!isProvisional()"};
		CommandLineOptions args = new CommandLineOptions();
		JCommander.newBuilder().addObject(args).build().parse(argv);
		Main application = new Main(args);

		ConfigurableApplicationContext context = application.getApplicationContext();
		Options options = context.getBean(Options.class);
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

	protected Multimap<String, String> executeWithParallelism(int parallelism) throws ExecutionException, InterruptedException {
		String[] argv = new String[]{
			"-parallelism", String.valueOf(parallelism),
			"-martiniComparatorImplementation", GatedMartiniComparator.class.getName(),
			"-configLocations", "classpath*:**/applicationContext.xml,classpath*:/bogus.xml"};
		CommandLineOptions args = new CommandLineOptions();
		JCommander.newBuilder().addObject(args).build().parse(argv);
		Main application = new Main(args);

		Multimap<String, String> executionIndex;

		try (ConfigurableApplicationContext context = application.getApplicationContext()) {
			TestListener listener = context.getBean(TestListener.class);
			application.executeSuite(context);
			executionIndex = listener.getExecutionIndex();
		}

		checkState(!executionIndex.isEmpty(), "no AfterScenarioEvent objects handled");
		return executionIndex;
	}

	protected void assertScenariosExecutedOnce(Multimap<String, String> executionIndex) {
		Collection<String> scenarios = executionIndex.values();
		int scenariosExecuted = scenarios.size();

		Set<String> uniq = new HashSet<>(scenarios);
		int uniqueScenarios = uniq.size();
		checkState(uniqueScenarios == scenariosExecuted, "scenarios executed more than once");
	}

	@Test
	public void testSingleThreaded() throws Exception {
		Multimap<String, String> executionIndex = executeWithParallelism(1);

		Set<String> threadNames = executionIndex.keySet();
		int threadCount = threadNames.size();
		checkState(1 == threadCount, "tests executed by more than one thread: %s", threadCount);

		assertScenariosExecutedOnce(executionIndex);
	}
}