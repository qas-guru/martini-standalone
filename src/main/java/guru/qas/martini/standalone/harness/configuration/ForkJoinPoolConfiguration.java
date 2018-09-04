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

package guru.qas.martini.standalone.harness.configuration;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import guru.qas.martini.standalone.jcommander.Args;

@SuppressWarnings("WeakerAccess")
@Configuration
@Lazy
public class ForkJoinPoolConfiguration implements DisposableBean {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ForkJoinPoolConfiguration.class);

	public static final String BEAN_NAME = "martiniForkJoinPool";

	protected final AutowireCapableBeanFactory beanFactory;
	protected final Args args;

	protected final Thread.UncaughtExceptionHandler exceptionHandler;
	protected ForkJoinPool forkJoinPool;

	@Autowired
	ForkJoinPoolConfiguration(
		AutowireCapableBeanFactory beanFactory,
		Args args,
		@Qualifier(UncaughtExceptionHandlerConfiguration.BEAN_NAME) Thread.UncaughtExceptionHandler exceptionHandler
	) {
		this.beanFactory = beanFactory;
		this.args = args;
		this.exceptionHandler = exceptionHandler;
	}

	@Bean(name = BEAN_NAME)
	ForkJoinPool getForkJoinPool() {
		LOGGER.info("creating ForkJoinPool with parallelization {}", args.parallelism);
		ForkJoinPool.ForkJoinWorkerThreadFactory factory = ForkJoinPool.defaultForkJoinWorkerThreadFactory;
		forkJoinPool = new ForkJoinPool(args.parallelism, factory, exceptionHandler, true);
		return forkJoinPool;
	}

	@Override
	public void destroy() throws InterruptedException {
		if (null != forkJoinPool && !forkJoinPool.isShutdown()) {
			forkJoinPool.shutdown();
			if (!forkJoinPool.awaitTermination(args.awaitTerminationSeconds, TimeUnit.SECONDS)) {
				forkJoinPool.shutdownNow();
			}
		}
	}
}