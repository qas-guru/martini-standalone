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

package guru.qas.martini.standalone.harness;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;
import guru.qas.martini.runtime.event.EventManager;
import guru.qas.martini.standalone.jcommander.Args;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultEngine implements Engine, ApplicationContextAware {

	protected final Mixologist mixologist;
	protected final EventManager eventManager;
	protected final int pollIntervalMs;

	protected ApplicationContext applicationContext;

	@Autowired
	DefaultEngine(
		Mixologist mixologist,
		EventManager eventManager,
		@Value("${martini.engine.poll.interval.ms:2000}") int pollIntervalMs
	) {
		this.mixologist = mixologist;
		this.eventManager = eventManager;
		this.pollIntervalMs = pollIntervalMs;
		checkArgument(pollIntervalMs > 0, "poll interval must be greater than zero ms");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void executeSuite(String filter, ExecutorService executorService, Integer timeoutInMinutes) {
		checkState(null != executorService, "null ExecutorService");

		Collection<Martini> martinis = null == filter || filter.isEmpty() ?
			mixologist.getMartinis() : mixologist.getMartinis(filter);
		checkState(!martinis.isEmpty(),
			null == filter || filter.isEmpty() ? "no Martinis found" : "no Martini found matching spel filter %s", filter);

		Environment environment = applicationContext.getEnvironment();
		Integer parallelism = environment.getRequiredProperty(Args.PROPERTY_PARALLELISM, int.class);

		throw new UnsupportedOperationException();
//		DefaultMartiniIndex.Builder builder = DefaultMartiniIndex.builder();
//		martinis.forEach(builder::add);
//
//		DefaultMartiniIndex index = builder.build();
//		executeSuite(executorService, timeoutInMinutes, index);
	}

//	// TODO: multiple users for timeout, get rid of the use in configuration.
//	private void executeSuite(ExecutorService service, Integer timeoutInMinutes) {
//		TaskFunction function = TaskFunction.builder().build(context);
//		SuiteIdentifier suiteIdentifier = function.getSuiteIdentifier();
//		eventManager.publishBeforeSuite(this, suiteIdentifier);
//
//		try {
//			Duration duration = Duration.ofMinutes(timeoutInMinutes);
//			long cutoff = duration.get(ChronoUnit.MILLIS);
//
//			// TODO: what's our max concurrency?
//			// TODO: maintain the same size of tasks!
//
//			while (System.currentTimeMillis() < cutoff && !index.isEmpty()) {
//				Collection<Martini> martinis = index.removeExecutables(/* TODO: HOW MANY? */);
//				List<Callable<MartiniResult>> tasks = martinis.stream().map(function).collect(Collectors.toList());
//				List<Future<MartiniResult>> futures = service.invokeAll(tasks);
//			}
//
//			long remaining = cutoff - System.currentTimeMillis();
//			if (0 < remaining) {
//				service.shutdown();
//				if (!service.awaitTermination(remaining, TimeUnit.MILLISECONDS)) {
//					LOGGER.warn("not all scenarios completed execution before termination");
//				}
//			}
//			else {
//				LOGGER.warn("over allotted runtime of {} minutes; shutting down now", duration.get(MINUTES));
//				service.shutdownNow();
//			}
//		}
//		catch (InterruptedException e) {
//			LOGGER.warn("interrupted while executing suite", e);
//		}
//		finally {
//			eventManager.publishAfterSuite(this, suiteIdentifier);
//		}
//	}
}
