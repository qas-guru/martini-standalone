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

import java.util.ArrayList;
import java.util.Collection;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SimpleTimeLimiter;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.event.DefaultSuiteIdentifier;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.result.DefaultMartiniResult;
import guru.qas.martini.runtime.event.EventManager;
import guru.qas.martini.standalone.jcommander.Args;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.standalone.harness.JsonSuiteMarshaller.LOGGER;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultEngine implements Engine, ApplicationContextAware {

	protected final Args args;
	protected final Mixologist mixologist;
	protected final EventManager eventManager;
	protected final int pollIntervalMs;

	protected ApplicationContext applicationContext;

	@Autowired
	DefaultEngine(
		Args args,
		Mixologist mixologist,
		EventManager eventManager,
		@Value("${martini.engine.poll.interval.ms:2000}") int pollIntervalMs
	) {
		this.args = args;
		this.mixologist = mixologist;
		this.eventManager = eventManager;
		this.pollIntervalMs = pollIntervalMs;
		checkArgument(pollIntervalMs > 0, "poll interval must be greater than zero ms");
	}

	@Override
	public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void executeSuite(String filter, ExecutorService executorService, Integer timeoutInMinutes) {
		checkState(null != executorService, "null ExecutorService");

		SuiteIdentifier suiteIdentifier = DefaultSuiteIdentifier.builder().build(applicationContext);
		eventManager.publishBeforeSuite(this, suiteIdentifier);

		Collection<Martini> martinis = getMartinis(filter);
		List<Martini> modifiable = new ArrayList<>(martinis);
		// TODO: create something that holds ordered martinis instead

		try {
			Runnable runnable = getRunnable(executorService, suiteIdentifier, modifiable);
			System.out.println(executorService);
			SimpleTimeLimiter limiter = SimpleTimeLimiter.create(executorService);
			limiter.runWithTimeout(runnable, timeoutInMinutes, TimeUnit.MINUTES);
			executorService.shutdown();
			// TODO: executorService.awaitTermination(timeout, unit); // remaining time from timeoutInMinutes
		}
		catch (InterruptedException e) {
			LOGGER.error("suite interrupted", e);
			executorService.shutdownNow(); // awaitTerminationSeconds = 5 *
		}
		catch (TimeoutException e) {
			LOGGER.error("suite timed out", e);
			executorService.shutdownNow(); // awaitTerminationSeconds = 5 *
		}
		finally {
			eventManager.publishAfterSuite(this, suiteIdentifier);
		}
	}

	protected Collection<Martini> getMartinis(String filter) {
		String trimmed = null == filter ? "" : filter.trim();
		Collection<Martini> martinis = trimmed.isEmpty() ?
			mixologist.getMartinis() : mixologist.getMartinis(filter);

		if (martinis.isEmpty()) {
			MessageSource source = MessageSources.getMessageSource(this.getClass());
			MartiniException.Builder builder = new MartiniException.Builder().setMessageSource(source);
			throw trimmed.isEmpty() ?
				builder.setKey("no.martinis.found").build() :
				builder.setKey("no.martinis.found.for.filter").setArguments(trimmed).build();
		}
		return martinis;
	}

	protected Runnable getRunnable(
		ExecutorService service,
		SuiteIdentifier suiteIdentifier,
		final List<Martini> martinis
	) {
		List<Future<?>> futures = Lists.newArrayList();

		return () -> {
			while (!martinis.isEmpty()) {
				int concurrency;
				synchronized (futures) {
					futures.removeIf(Future::isDone);
					concurrency = futures.size();

					if (concurrency < args.parallelism) {
						Runnable task = () -> {
							Martini next = martinis.isEmpty() ? null : martinis.remove(0);
							// TODO: check gating!!!
							Thread thread = Thread.currentThread();
							String threadName = thread.getName();
							System.out.printf("Thread %s next: %s\n", threadName, next);

							if (null != next) {
								String groupName = thread.getThreadGroup().getName();
								DefaultMartiniResult result = DefaultMartiniResult.builder()
									.setMartiniSuiteIdentifier(suiteIdentifier)
									.setMartini(next)
									.setThreadGroupName(groupName)
									.setThreadName(threadName)
									.build();
								eventManager.publishBeforeScenario(this, result);
								eventManager.publishAfterScenario(this, result);
							}
						};
						futures.add(service.submit(task));
					}
				}

				if (concurrency >= args.parallelism) {
					try {
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {
						throw new RuntimeException("execution interrupted", e);
					}
				}
			}
		};
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
