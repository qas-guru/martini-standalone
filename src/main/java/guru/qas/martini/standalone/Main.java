/*
Copyright 2017 Penny Rohr Curich

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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;

import com.beust.jcommander.JCommander;

import guru.qas.martini.standalone.harness.Engine;

@SuppressWarnings("WeakerAccess")
public class Main {

	private final Args args;

	protected Main(Args args) {
		this.args = args;
	}

	public void doSomething() throws ClassNotFoundException, InterruptedException, ExecutionException {

		try (ConfigurableApplicationContext context = getApplicationContext()) {
			ForkJoinPool forkJoinPool = getForkJoinPool(context);
			Engine engine = context.getBean(Engine.class);
			String filter = args.getSpelFilter();
			Integer timeoutInMinutes = args.getTimeoutInMinutes();
			engine.doSomething(filter, forkJoinPool, timeoutInMinutes);
		}
	}

	protected ConfigurableApplicationContext getApplicationContext() {
		String[] configLocations = args.getConfigLocations();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configLocations);
		context.registerShutdownHook();
		return context;
	}

	protected ForkJoinPool getForkJoinPool(ConfigurableApplicationContext context) throws ClassNotFoundException {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		Thread.UncaughtExceptionHandler handler = beanFactory.createBean(args.getUncaughtExceptionHandlerImplementation());

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
		JCommander.newBuilder().addObject(args).build().parse(argv);
		Main application = new Main(args);
		application.doSomething();
	}
}
