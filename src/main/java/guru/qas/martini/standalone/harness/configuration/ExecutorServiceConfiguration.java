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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;

import guru.qas.martini.standalone.jcommander.Args;

@SuppressWarnings("WeakerAccess")
@Configuration
@Lazy
public class ExecutorServiceConfiguration implements DisposableBean {

	public static final String BEAN_NAME = "martiniExecutorService";

	protected final AutowireCapableBeanFactory beanFactory;
	protected final Args args;

	protected final Thread.UncaughtExceptionHandler exceptionHandler;
	protected ExecutorService executorService;

	@Autowired
	ExecutorServiceConfiguration(
		AutowireCapableBeanFactory beanFactory,
		Args args,
		@Qualifier("martiniUncaughtExceptionHandler") Thread.UncaughtExceptionHandler exceptionHandler
	) {
		this.beanFactory = beanFactory;
		this.args = args;
		this.exceptionHandler = exceptionHandler;
	}

	@Bean(name = BEAN_NAME)
	ExecutorService getExecutorService() {
		return 1 == args.parallelism ? getInThreadExecutorService() : getForkJoinExecutorService();
	}

	protected ExecutorService getInThreadExecutorService() {
		executorService = beanFactory.createBean(InThreadExecutorService.class);
		return executorService;
	}

	protected ExecutorService getForkJoinExecutorService() {
		ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
		factory.setParallelism(args.parallelism);
		factory.setAwaitTerminationSeconds(args.awaitTerminationSeconds);
		factory.setCommonPool(false);
		factory.setUncaughtExceptionHandler(exceptionHandler);
		factory.afterPropertiesSet();
		executorService = factory.getObject();
		return executorService;
	}

	@Override
	public void destroy() throws Exception {
		if (null != executorService && !DisposableBean.class.isInstance(executorService) && !executorService.isShutdown()) {
			if (args.awaitTerminationSeconds > 0) {
				executorService.shutdown();
				executorService.awaitTermination(args.awaitTerminationSeconds, TimeUnit.SECONDS);
			}
			else {
				executorService.shutdownNow();
			}
		}
	}
}
