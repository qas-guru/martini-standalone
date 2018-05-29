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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;

import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.runtime.event.EventManager;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.standalone.harness.JsonSuiteMarshaller.LOGGER;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultEngine implements Engine {

	private final ApplicationContext context;
	private final Mixologist mixologist;
	private final EventManager eventManager;

	@Autowired
	DefaultEngine(ApplicationContext context, Mixologist mixologist, EventManager eventManager) {
		this.context = context;
		this.mixologist = mixologist;
		this.eventManager = eventManager;
	}

	@Override
	public void executeSuite(String filter, ExecutorService executorService, Integer timeoutInMinutes) {
		checkState(null != executorService, "null ExecutorService");

		Collection<Martini> martinis = null == filter || filter.isEmpty() ?
			mixologist.getMartinis() : mixologist.getMartinis(filter);
		checkState(!martinis.isEmpty(),
			null == filter || filter.isEmpty() ? "no Martinis found" : "no Martini found matching spel filter %s", filter);
		executeSuite(executorService, timeoutInMinutes, martinis);
	}

	private void executeSuite(ExecutorService service, Integer timeoutInMinutes, Collection<Martini> martinis) {
		TaskFunction function = TaskFunction.builder().build(context);
		SuiteIdentifier suiteIdentifier = function.getSuiteIdentifier();
		eventManager.publishBeforeSuite(this, suiteIdentifier);
		try {
			List<Callable<MartiniResult>> tasks = martinis.stream().map(function).collect(Collectors.toList());
			service.invokeAll(tasks, timeoutInMinutes, TimeUnit.MINUTES);
		}
		catch (InterruptedException e) {
			LOGGER.warn("interrupted while executing suite", e);
		}
		finally {
			eventManager.publishAfterSuite(this, suiteIdentifier);
		}
	}
}
