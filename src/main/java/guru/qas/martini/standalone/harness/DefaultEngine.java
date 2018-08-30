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

import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;

import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.SimpleTimeLimiter;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.result.DefaultMartiniResult;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.runtime.event.EventManager;
import guru.qas.martini.standalone.harness.configuration.ExecutorServiceConfiguration;
import guru.qas.martini.standalone.harness.configuration.MartiniComparatorConfiguration;
import guru.qas.martini.standalone.jcommander.Args;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultEngine implements Engine, ApplicationContextAware {

	protected final Args args;
	protected final Mixologist mixologist;
	protected final SuiteIdentifier suiteIdentifier;
	protected final Comparator<Martini> comparator;
	protected final EventManager eventManager;
	protected final ExecutorService executorService;

	protected ApplicationContext applicationContext;

	@Autowired
	DefaultEngine(
		Args args,
		Mixologist mixologist,
		SuiteIdentifier suiteIdentifier,
		@Qualifier(MartiniComparatorConfiguration.BEAN_NAME) Comparator<Martini> comparator,
		EventManager eventManager,
		@Qualifier(ExecutorServiceConfiguration.BEAN_NAME) ExecutorService executorService
	) {
		this.args = args;
		this.mixologist = mixologist;
		this.suiteIdentifier = suiteIdentifier;
		this.comparator = comparator;
		this.eventManager = eventManager;
		this.executorService = executorService;
	}

	@Override
	public void setApplicationContext(@Nonnull ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void executeSuite(@Nullable String filter, @Nullable Integer timeoutInMinutes) {
		eventManager.publishBeforeSuite(this, suiteIdentifier);
		Collection<Martini> martinis = getMartinis(filter);

		try {
			Runnable runnable = getRunnable(martinis);
			if (null != timeoutInMinutes && 0 < timeoutInMinutes) {
				executeWithTimeLimit(runnable, timeoutInMinutes);
			}
			else {
				runnable.run();
			}
		}
		finally {
			eventManager.publishAfterSuite(this, suiteIdentifier);
		}
	}

	protected void executeWithTimeLimit(Runnable runnable, int timeoutInMinutes) {
		try {
			SimpleTimeLimiter limiter = SimpleTimeLimiter.create(executorService);
			limiter.runWithTimeout(runnable, timeoutInMinutes, TimeUnit.MINUTES);
		}
		catch (InterruptedException e) {
			throw getMartiniException(e);
		}
		catch (TimeoutException e) {
			throw getMartiniException(e);
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

		return Ordering.from(comparator).sortedCopy(martinis);
	}

	protected Runnable getRunnable(Collection<Martini> martiniCollection) {
		ConcurrentLinkedDeque<Martini> martinis = new ConcurrentLinkedDeque<>(martiniCollection);
		ConcurrentLinkedDeque<Future> futures = new ConcurrentLinkedDeque<>();

		return () -> {
			try {
				while (!martinis.isEmpty()) {

					futures.stream()
						.filter(future -> future.isCancelled() || future.isDone())
						.forEach(futures::remove);

					if (futures.size() < args.parallelism) {
						Runnable task = () -> {

							Martini next;
							synchronized (martinis) {
								next = martinis.pollFirst(); // TODO: gating!
							}

							Thread thread = Thread.currentThread();
							String threadName = thread.getName();
							System.out.printf("Thread %s next: %s\n", threadName, next);

							if (null != next) {
								MartiniResult result = getMartiniResult(suiteIdentifier, next);
								eventManager.publishBeforeScenario(this, result);
								// TODO: actualy do something with Martini
								eventManager.publishAfterScenario(this, result);
							}
						};
						futures.add(executorService.submit(task));
					}

					if (futures.size() >= args.parallelism) {
						Thread.sleep(1000);
					}
				}
			}
			catch (InterruptedException e) {
				throw getMartiniException(e);
			}
			finally {
				futures.forEach(future -> future.cancel(true));
			}
		};
	}

	protected MartiniResult getMartiniResult(SuiteIdentifier identifier, Martini martini) {
		Thread thread = Thread.currentThread();
		String threadName = thread.getName();
		String groupName = thread.getThreadGroup().getName();

		return DefaultMartiniResult.builder()
			.setMartiniSuiteIdentifier(identifier)
			.setMartini(martini)
			.setThreadGroupName(groupName)
			.setThreadName(threadName)
			.build();
	}

	protected MartiniException getMartiniException(InterruptedException cause) {
		MessageSource messageSource = MessageSources.getMessageSource(this.getClass());
		return new MartiniException.Builder()
			.setCause(cause)
			.setMessageSource(messageSource)
			.setKey("execution.interrupted")
			.build();
	}

	protected MartiniException getMartiniException(TimeoutException cause) {
		MessageSource messageSource = MessageSources.getMessageSource(this.getClass());
		throw new MartiniException.Builder()
			.setCause(cause)
			.setMessageSource(messageSource)
			.setKey("execution.timed.out")
			.build();
	}
}
