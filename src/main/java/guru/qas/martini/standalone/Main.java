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

import java.util.concurrent.ExecutionException;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import guru.qas.martini.standalone.harness.MartiniStandaloneEngine;
import guru.qas.martini.standalone.harness.Options;
import guru.qas.martini.standalone.jcommander.CommandLineOptions;
import guru.qas.martini.standalone.jcommander.OptionsPropertySource;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class Main {

	protected final Options options;

	public Main(Options options) {
		this.options = checkNotNull(options, "null Options");
	}

	public void executeSuite() throws InterruptedException, ExecutionException {
		try (ConfigurableApplicationContext context = getApplicationContext()) {
			context.start();
			executeSuite(context);
			context.stop();
		}
	}

	public ConfigurableApplicationContext getApplicationContext() {
		String[] locations = options.getSpringConfigurationLocations();
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(locations, false);
		addOptions(context);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	protected void addOptions(ConfigurableApplicationContext context) {
		ConfigurableEnvironment environment = context.getEnvironment();
		MutablePropertySources sources = environment.getPropertySources();
		sources.addLast(new OptionsPropertySource(options));
	}

	public void executeSuite(ConfigurableApplicationContext context) throws ExecutionException, InterruptedException {
		MartiniStandaloneEngine engine = context.getBean(MartiniStandaloneEngine.class);
		engine.executeSuite();
	}

	public static void main(String[] argv) throws Exception {
		try {
			CommandLineOptions commandLineOptions = new CommandLineOptions();
			JCommander jCommander = JCommander.newBuilder()
				.acceptUnknownOptions(false)
				.programName(Main.class.getName())
				.addObject(commandLineOptions)
				.build();

			jCommander.parse(argv);
			if (commandLineOptions.isHelp()) {
				jCommander.usage();
			}
			else {
				main(commandLineOptions);
			}
		}
		catch (ParameterException e) {
			e.printStackTrace();
			e.usage();
		}
	}

	protected static void main(Options options) throws ExecutionException, InterruptedException {
		Main application = new Main(options);
		application.executeSuite();
	}
}
