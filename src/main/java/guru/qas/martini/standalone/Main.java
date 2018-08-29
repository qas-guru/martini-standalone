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
import java.util.concurrent.ExecutorService;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import guru.qas.martini.standalone.harness.Engine;
import guru.qas.martini.standalone.jcommander.Args;
import guru.qas.martini.standalone.jcommander.ArgsPropertySource;
import guru.qas.martini.standalone.jcommander.WritableResourceConverterFactory;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class Main {

	private final Args args;

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

	public void executeSuite(ConfigurableApplicationContext context) throws ExecutionException, InterruptedException {
		Engine engine = context.getBean(Engine.class);
		ExecutorService service = getExecutorService(context);
		engine.executeSuite(args.spelFilter, service, args.timeoutInMinutes);
	}

	public ConfigurableApplicationContext getApplicationContext() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(args.configLocations, false);
		updateEnvironment(context);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	private void updateEnvironment(ConfigurableApplicationContext context) {
		ConfigurableEnvironment environment = context.getEnvironment();
		MutablePropertySources sources = environment.getPropertySources();
		sources.addLast(new ArgsPropertySource(args));
	}

	protected ExecutorService getExecutorService(ConfigurableApplicationContext context) {
		Thread.UncaughtExceptionHandler handler = context.getBean(
			"martiniUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class);

		ExecutorService executorService;
		if (1 == args.parallelism) {
			executorService = new InThreadExecutorService(handler, args.awaitTerminationSeconds);
			context.getBeanFactory().registerSingleton(executorService.getClass().getName(), executorService);
		}
		else {
			ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
			factory.setParallelism(args.parallelism);
			factory.setAwaitTerminationSeconds(args.awaitTerminationSeconds);
			factory.setCommonPool(false);
			factory.setUncaughtExceptionHandler(handler);
			factory.afterPropertiesSet();
			executorService = factory.getObject();
		}
		return executorService;
	}

	public static void main(String[] argv) throws Exception {
		Args args = new Args();
		/*
			@Parameter(
		names = "-jsonOverwrite",
		description = "overwrites existing JSON output",
		arity = 1
	)
	public boolean jsonOverwrite = true;

	@Parameter(
		names = {"-jsonOutput", "martini.standalone.json.output.resource"},
		description = "URI destination for JSON suite reporting, e.g. file:///tmp/martini.json")
	public WritableResource jsonOutputResource;

	IStringConverterFactory var1
		 */
		try {
			JCommander jCommander = JCommander.newBuilder()
				.addConverterInstanceFactory(new WritableResourceConverterFactory(args))
				.addObject(args)
				.build();
			jCommander.parse(argv);
			main(args, jCommander);
		}
		catch (ParameterException e) {
			e.printStackTrace();
			e.usage();
		}
	}

	private static void main(Args args, JCommander jCommander) throws Exception {
		if (args.help) {
			jCommander.usage();
		}
		else {
			Main application = new Main(args);
			application.executeSuite();
		}
	}
}
