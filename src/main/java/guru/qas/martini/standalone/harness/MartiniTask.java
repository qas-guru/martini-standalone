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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.event.AfterScenarioEvent;
import guru.qas.martini.event.BeforeScenarioEvent;
import guru.qas.martini.event.MartiniEventPublisher;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.DefaultMartiniResult;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.tag.Categories;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class MartiniTask implements Callable<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniTask.class);
	protected static final String TEMPLATE = "unimplemented step: %s line %s: @%s %s";

	private final SuiteIdentifier suiteIdentifier;
	private final Martini martini;
	private final MartiniEventPublisher publisher;
	private final Categories categories;
	private final ConversionService conversionService;
	private final BeanFactory beanFactory;

	public MartiniTask(
		BeanFactory beanFactory,
		MartiniEventPublisher publisher,
		ConversionService conversionService,
		Categories categories,
		SuiteIdentifier suiteIdentifier,
		Martini martini
	) {
		this.beanFactory = checkNotNull(beanFactory, "null BeanFactory");
		this.publisher = checkNotNull(publisher, "null MartiniEventPublisher");
		this.conversionService = checkNotNull(conversionService, "null ConversionService");
		this.categories = checkNotNull(categories, "null Categories");
		this.suiteIdentifier = checkNotNull(suiteIdentifier, "null SuiteIdentifier");
		this.martini = checkNotNull(martini, "null Martini");
	}

	@Override
	public String call() throws Exception {
		LOGGER.info("executing scenario {}", martini.getId());
		Thread thread = Thread.currentThread();
		Set<String> categorizations = categories.getCategorizations(martini);

		MartiniResult result = DefaultMartiniResult.builder()
			.setThreadGroupName(thread.getThreadGroup().getName())
			.setThreadName(thread.getName())
			.setCategorizations(categorizations)
			.setMartini(martini)
			.setMartiniSuiteIdentifier(suiteIdentifier)
			.build();

		publisher.publish(new BeforeScenarioEvent(this, result));

		try {
			Map<Step, StepImplementation> stepIndex = martini.getStepIndex();
			for (Map.Entry<Step, StepImplementation> mapEntry : stepIndex.entrySet()) {
				Step step = mapEntry.getKey();
				StepImplementation implementation = mapEntry.getValue();
				execute(step, implementation);
			}
		}
		finally {
			publisher.publish(new AfterScenarioEvent(this, result));
		}

		String threadName = Thread.currentThread().getName();
		String id = martini.getId();
		return String.format("Thread %s Martini %s", threadName, id);
	}

	@SuppressWarnings("UnusedReturnValue")
	protected Object execute(Step step, StepImplementation implementation)
		throws InvocationTargetException, IllegalAccessException {

		LOGGER.info("executing @{} {}", step.getKeyword().trim(), step.getText().trim());
		assertImplemented(step, implementation);

		Object[] arguments = getArguments(step, implementation);
		Object bean = getBean(implementation);
		return execute(implementation, bean, arguments);
	}

	protected void assertImplemented(Step step, StepImplementation implementation) {
		Method method = implementation.getMethod();
		if (null == method) {
			String description = martini.getRecipe().getSource().getDescription();
			int line = step.getLocation().getLine();
			String keyword = step.getKeyword().trim();
			String text = step.getText().trim();
			String message = String.format(TEMPLATE, description, line, keyword, text);
			throw new IllegalStateException(message);
		}
	}

	protected Object[] getArguments(Step step, StepImplementation implementation) {
		Method method = implementation.getMethod();
		Parameter[] parameters = method.getParameters();
		Object[] arguments = new Object[parameters.length];

		if (parameters.length > 0) {
			String text = step.getText();
			Pattern pattern = implementation.getPattern();
			Matcher matcher = pattern.matcher(text);
			checkState(matcher.find(),
				"unable to locate substitution parameters for pattern %s with input %s", pattern.pattern(), text);

			int groupCount = matcher.groupCount();
			for (int i = 0; i < groupCount; i++) {
				String parameterAsString = matcher.group(i + 1);
				Parameter parameter = parameters[i];
				Class<?> parameterType = parameter.getType();
				Object converted = conversionService.convert(parameterAsString, parameterType);
				arguments[i] = converted;
			}
		}
		return arguments;
	}

	protected Object getBean(StepImplementation implementation) {
		Method method = implementation.getMethod();
		Class<?> declaringClass = method.getDeclaringClass();
		return beanFactory.getBean(declaringClass);
	}

	protected Object execute(
		StepImplementation implementation,
		Object bean,
		Object[] arguments
	) throws InvocationTargetException, IllegalAccessException {
		Method method = implementation.getMethod();
		return method.invoke(bean, arguments);
	}
}
