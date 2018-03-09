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
import java.util.concurrent.ForkJoinPool;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import guru.qas.martini.standalone.harness.Engine;

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
		String filter = args.getSpelFilter();
		ForkJoinPool forkJoinPool = getForkJoinPool(context);
		Integer timeoutInMinutes = args.getTimeoutInMinutes();
		engine.executeSuite(filter, forkJoinPool, timeoutInMinutes);
	}

	public ConfigurableApplicationContext getApplicationContext() {
		String[] configLocations = args.getConfigLocations();
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(configLocations, false);
		updateEnvironment(context);
		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	private void updateEnvironment(ConfigurableApplicationContext context) {
		ConfigurableEnvironment environment = context.getEnvironment();
		MutablePropertySources sources = environment.getPropertySources();
		sources.addLast(new WritableJsonResourceProperties(args));
	}

	protected ForkJoinPool getForkJoinPool(ConfigurableApplicationContext context) {
		Thread.UncaughtExceptionHandler handler = context.getBean("martiniUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class);

		ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
		factory.setParallelism(args.getParallelism());
		factory.setAsyncMode(true);
		factory.setAwaitTerminationSeconds(args.getAwaitTerminationSeconds());
		factory.setCommonPool(false);
		factory.setUncaughtExceptionHandler(handler);
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	public static void main(String[] argv) throws Exception {
		Args args = new Args();
		try {
			JCommander jCommander = JCommander.newBuilder().addObject(args).build();
			jCommander.parse(argv);
			main(args, jCommander);
		}
		catch (ParameterException e) {
			e.printStackTrace();
			e.usage();
		}
	}

	private static void main(Args args, JCommander jCommander) throws Exception {
		if (args.isHelp()) {
			jCommander.usage();
		}
		else {
			Main application = new Main(args);
			application.executeSuite();
		}
	}
}
