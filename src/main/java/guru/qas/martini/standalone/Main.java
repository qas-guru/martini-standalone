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

import guru.qas.martini.standalone.harness.Engine;
import guru.qas.martini.standalone.jcommander.Args;
import guru.qas.martini.standalone.jcommander.ArgsPropertySource;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class Main {

	protected final Args args;

	public Main(Args args) {
		this.args = checkNotNull(args, "null Args");
	}

	public void executeSuite() throws InterruptedException, ExecutionException {
		try (ConfigurableApplicationContext context = getApplicationContext()) {
			context.start();
			executeSuite(context);
			context.stop();
		}
	}

	public ConfigurableApplicationContext getApplicationContext() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(args.configLocations, false);
		addJCommanderArgs(context);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	private void addJCommanderArgs(ConfigurableApplicationContext context) {
		ConfigurableEnvironment environment = context.getEnvironment();
		MutablePropertySources sources = environment.getPropertySources();
		sources.addLast(new ArgsPropertySource(args));
	}

	public void executeSuite(ConfigurableApplicationContext context) throws ExecutionException, InterruptedException {
		Engine engine = context.getBean(Engine.class);
		engine.executeSuite(args.spelFilter, args.timeoutInMinutes);
	}

	public static void main(String[] argv) throws Exception {
		try {
			Args args = new Args();
			JCommander jCommander = JCommander.newBuilder().addObject(args).build();
			jCommander.parse(argv);
			main(args, jCommander);
		}
		catch (ParameterException e) {
			e.printStackTrace();
			e.usage();
		}
	}

	protected static void main(Args args, JCommander jCommander) throws Exception {
		if (args.help) {
			jCommander.usage();
		}
		else {
			Main application = new Main(args);
			application.executeSuite();
		}
	}
}
