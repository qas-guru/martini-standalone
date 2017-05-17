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

package guru.qas.martini.standalone.harness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ForkJoinPoolFactoryBean;

import static com.google.common.base.Preconditions.checkArgument;

@Configuration
public class ForkJoinPoolConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(ForkJoinPoolConfiguration.class);

	@Bean(name = "martiniStandaloneParallelism")
	public int getParallelism(@Value("${fork.join.pool.parallelism?:null}") Integer override) {
		checkArgument(null == override || override > 0,
			"invalid fork.join.pool.parallelism %s; must be greater than zero", override);

		Integer parallelism = override;
		if (null == parallelism) {
			parallelism = Runtime.getRuntime().availableProcessors();
		}
		LOGGER.info("using ForkJoinPool parallelism %s", parallelism);
		return parallelism;
	}

	@Bean(name = "martiniStandaloneAwaitTerminationSeconds")
	public int getAWaitTerminationSeconds(@Value("${fork.join.pool.await.termination.seconds?:null}") Integer override) {
		checkArgument(null == override || override > 0,
			"invalid fork.join.pool.await.termination.seconds value %s; must be greater than zero", override);
		Integer awaitTerminationSeconds = override;
		if (null == awaitTerminationSeconds) {
			awaitTerminationSeconds = 10;
		}
		LOGGER.info("using ForkJoinPool awaitTerminationSeconds %s", awaitTerminationSeconds);
		return awaitTerminationSeconds;
	}

	@Bean(name = "martiniStandaloneUncaughtExceptionHandler")
	public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler(
		@Value("${fork.join.pool.uncaught.exception.handler.implementation?:null}") Class<? extends Thread.UncaughtExceptionHandler> override,
		AutowireCapableBeanFactory beanFactory
	) {
		Class<? extends Thread.UncaughtExceptionHandler> implementation = null == override ?
			DefaultUncaughtExceptionHandler.class : override;
		return beanFactory.createBean(implementation);
	}

	@Bean
	public ForkJoinPoolFactoryBean forkJoinPoolFactoryBean(
		@Qualifier("martiniStandaloneParallelism") int parallelism,
		@Qualifier("martiniStandaloneAwaitTerminationSeconds") int awaitTerminationSeconds,
		@Qualifier("martiniStandaloneUncaughtExceptionHandler") Thread.UncaughtExceptionHandler handler
	) {
		ForkJoinPoolFactoryBean factory = new ForkJoinPoolFactoryBean();
		factory.setParallelism(parallelism);
		factory.setAsyncMode(true);
		factory.setAwaitTerminationSeconds(awaitTerminationSeconds);
		factory.setCommonPool(false);
		factory.setUncaughtExceptionHandler(handler);
		return factory;
	}
}
