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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;

import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.SimpleTimeLimiter;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.i18n.MessageSources;

import guru.qas.martini.runtime.event.EventManager;
import guru.qas.martini.standalone.harness.configuration.ForkJoinPoolConfiguration;
import guru.qas.martini.standalone.harness.configuration.MartiniComparatorConfiguration;
import guru.qas.martini.standalone.jcommander.Args;

import static java.util.concurrent.TimeUnit.MINUTES;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultMartiniStandaloneEngine implements MartiniStandaloneEngine, ApplicationContextAware {

	protected final Args args;
	protected final Mixologist mixologist;
	protected final SuiteIdentifier suiteIdentifier;
	protected final Comparator<Martini> comparator;
	protected final TaskFactory taskFactory;
	protected final EventManager eventManager;
	protected final ForkJoinPool forkJoinPool;
	protected final long pollInterval;
	protected final Logger logger;

	protected ApplicationContext applicationContext;

	@Autowired
	DefaultMartiniStandaloneEngine(
		Args args,
		Mixologist mixologist,
		SuiteIdentifier suiteIdentifier,
		@Qualifier(MartiniComparatorConfiguration.BEAN_NAME) Comparator<Martini> comparator,
		TaskFactory taskFactory,
		EventManager eventManager,
		@Qualifier(ForkJoinPoolConfiguration.BEAN_NAME) ForkJoinPool forkJoinPool,
		@Value("${martini.fork.join.pool.poll.interval:250}") long pollInterval
	) {
		this.args = args;
		this.mixologist = mixologist;
		this.suiteIdentifier = suiteIdentifier;
		this.comparator = comparator;
		this.taskFactory = taskFactory;
		this.eventManager = eventManager;
		this.forkJoinPool = forkJoinPool;
		this.pollInterval = pollInterval;
		this.logger = LoggerFactory.getLogger(this.getClass());
	}

	@Override
	public void setApplicationContext(@Nonnull ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void executeSuite() {
		eventManager.publishBeforeSuite(this, suiteIdentifier);
		Collection<Martini> martinis = getMartinis();

		try {
			Runnable runnable = getRunnable(martinis);
			if (args.timeoutInMinutes > 0) {
				executeWithTimeLimit(runnable, args.timeoutInMinutes);
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
			SimpleTimeLimiter limiter = SimpleTimeLimiter.create(ForkJoinPool.commonPool());
			limiter.runWithTimeout(runnable, timeoutInMinutes, MINUTES);
		}
		catch (InterruptedException e) {
			throw getMartiniException(e);
		}
		catch (TimeoutException e) {
			throw getMartiniException(e);
		}
	}

	protected Collection<Martini> getMartinis() {
		String trimmed = null == args.spelFilter ? "" : args.spelFilter.trim();
		Collection<Martini> martinis = trimmed.isEmpty() ?
			mixologist.getMartinis() : mixologist.getMartinis(trimmed);

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
		Monitor monitor = new Monitor();

		return () -> {
			while (!martinis.isEmpty()) {
				if (forkJoinPool.hasQueuedSubmissions()) {
					sleep();
				}
				else {
					Runnable task = taskFactory.getTask(monitor, martinis);
					forkJoinPool.submit(task);
				}
			}
			forkJoinPool.awaitQuiescence(args.timeoutInMinutes, TimeUnit.MINUTES);
		};
	}

	protected void sleep() {
		if (pollInterval > 0) {
			try {
				Thread.sleep(pollInterval);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
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
