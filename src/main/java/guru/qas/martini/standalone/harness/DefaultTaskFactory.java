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

package guru.qas.martini.standalone.harness;

import java.util.ArrayList;
import java.util.Collection;

import java.util.Iterator;
import java.util.Optional;

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.collect.Lists;

import guru.qas.martini.Martini;
import guru.qas.martini.gate.MartiniGate;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.runtime.event.EventManager;
import guru.qas.martini.runtime.harness.MartiniCallable;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultTaskFactory implements TaskFactory, ApplicationContextAware {

	protected final EventManager eventManager;
	protected final Logger logger;

	protected ApplicationContext applicationContext;

	@Autowired
	DefaultTaskFactory(EventManager eventManager) {
		this.eventManager = eventManager;
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Override
	public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Runnable getTask(Iterator<Optional<Martini>> i) {
		checkNotNull(i, "null Iterator");
		return () -> {
			Martini next = i.hasNext() ? i.next().orElse(null) : null;
			if (null != next) {
				execute(next);
			}
		};
	}

	protected void execute(Martini martini) {
		try {
			Callable<MartiniResult> callable = new MartiniCallable(martini);
			AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
			beanFactory.autowireBean(callable);
			beanFactory.initializeBean(callable, callable.getClass().getName());
			callable.call();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			releasePermits(martini);
		}
	}

	protected void releasePermits(Martini martini) {
		Collection<MartiniGate> gates = martini.getGates();
		ArrayList<MartiniGate> gateList = Lists.newArrayList(gates);
		Lists.reverse(gateList).forEach(MartiniGate::leave);
	}
}
