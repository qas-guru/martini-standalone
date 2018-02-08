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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;

import gherkin.ast.Examples;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Step;
import gherkin.ast.TableCell;
import gherkin.ast.TableRow;
import guru.qas.martini.Martini;
import guru.qas.martini.event.Status;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.result.DefaultMartiniResult;
import guru.qas.martini.result.DefaultStepResult;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.runtime.event.EventManager;
import guru.qas.martini.standalone.exception.UnimplementedStepException;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.tag.Categories;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class MartiniTask implements Callable<MartiniResult> {

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniTask.class);
	private static final Pattern OUTLINE_PATTERN = Pattern.compile("^<(.*)>$");

	private final SuiteIdentifier suiteIdentifier;
	private final Martini martini;
	private final EventManager eventManager;
	private final Categories categories;
	private final ConversionService conversionService;
	private final BeanFactory beanFactory;

	public MartiniTask(
		BeanFactory beanFactory,
		EventManager eventManager,
		ConversionService conversionService,
		Categories categories,
		SuiteIdentifier suiteIdentifier,
		Martini martini
	) {
		this.beanFactory = checkNotNull(beanFactory, "null BeanFactory");
		this.eventManager = checkNotNull(eventManager, "null EventManager");
		this.conversionService = checkNotNull(conversionService, "null ConversionService");
		this.categories = checkNotNull(categories, "null Categories");
		this.suiteIdentifier = checkNotNull(suiteIdentifier, "null SuiteIdentifier");
		this.martini = checkNotNull(martini, "null Martini");
	}

	@Override
	public MartiniResult call() throws Exception {
		LOGGER.info("executing scenario {}", martini.getId());
		Thread thread = Thread.currentThread();
		Set<String> categorizations = categories.getCategorizations(martini);

		DefaultMartiniResult result = DefaultMartiniResult.builder()
			.setThreadGroupName(thread.getThreadGroup().getName())
			.setThreadName(thread.getName())
			.setCategorizations(categorizations)
			.setMartini(martini)
			.setMartiniSuiteIdentifier(suiteIdentifier)
			.build();

		eventManager.publishBeforeScenario(this, result);

		try {
			Map<Step, StepImplementation> stepIndex = martini.getStepIndex();
			result.setStartTimestamp(System.currentTimeMillis());

			DefaultStepResult lastResult = null;
			for (Map.Entry<Step, StepImplementation> mapEntry : stepIndex.entrySet()) {

				Step step = mapEntry.getKey();
				eventManager.publishBeforeStep(this, result);

				StepImplementation implementation = mapEntry.getValue();
				if (null == lastResult || Status.PASSED == lastResult.getStatus()) {
					lastResult = execute(step, implementation);
				}
				else {
					lastResult = new DefaultStepResult(step, implementation);
					lastResult.setStatus(Status.SKIPPED);
				}
				result.add(lastResult);
				eventManager.publishAfterStep(this, result);
			}
		}
		finally {
			result.setEndTimestamp(System.currentTimeMillis());
			eventManager.publishAfterScenario(this, result);
		}

		return result;
	}

	protected DefaultStepResult execute(Step step, StepImplementation implementation)
		throws InvocationTargetException, IllegalAccessException {
		LOGGER.info("executing @{} {}", step.getKeyword().trim(), step.getText().trim());

		DefaultStepResult result = new DefaultStepResult(step, implementation);
		result.setStartTimestamp(System.currentTimeMillis());
		try {
			assertImplemented(step, implementation);
			Object[] arguments = getArguments(step, implementation);

			Object bean = getBean(implementation);
			Object o = execute(implementation, bean, arguments);
			if (HttpEntity.class.isInstance(o)) {
				result.add(HttpEntity.class.cast(o));
			}
			result.setStatus(Status.PASSED);
		}
		catch (UnimplementedStepException e) {
			result.setException(e);
			result.setStatus(Status.SKIPPED);
		}
		catch (Exception e) {
			result.setException(e);
			result.setStatus(Status.FAILED);
		}
		finally {
			result.setEndTimestamp(System.currentTimeMillis());
		}
		return result;
	}

	protected void assertImplemented(Step step, StepImplementation implementation) throws UnimplementedStepException {
		Method method = implementation.getMethod();
		if (null == method) {
			throw new UnimplementedStepException(martini, step);
		}
	}

	protected Object[] getArguments(Step step, StepImplementation implementation) {
		Method method = implementation.getMethod();
		Parameter[] parameters = method.getParameters();
		Object[] arguments = new Object[parameters.length];

		Map<String, String> exampleValues = null;

		Recipe recipe = martini.getRecipe();
		ScenarioDefinition definition = recipe.getScenarioDefinition();
		if (ScenarioOutline.class.isInstance(definition)) {
			exampleValues = new HashMap<>();
			ScenarioOutline outline = ScenarioOutline.class.cast(definition);

			int exampleLine = recipe.getLocation().getLine();

			List<Examples> examples = outline.getExamples();
			TableRow header = null;
			TableRow match = null;
			for (Iterator<Examples> i = examples.iterator(); null == match && i.hasNext(); ) {
				Examples nextExamples = i.next();
				List<TableRow> rows = nextExamples.getTableBody();
				for (Iterator<TableRow> j = rows.iterator(); null == match && j.hasNext(); ) {
					TableRow row = j.next();
					if (row.getLocation().getLine() == exampleLine) {
						match = row;
						header = nextExamples.getTableHeader();
					}
				}
			}

			checkState(null != header, "unable to locate matching Examples table");
			List<TableCell> headerCells = header.getCells();
			List<TableCell> rowCells = match.getCells();
			checkState(headerCells.size() == rowCells.size(), "Examples header to row size mismatch");
			for (int i = 0; i < headerCells.size(); i++) {
				String headerValue = headerCells.get(i).getValue();
				String rowValue = rowCells.get(i).getValue();
				exampleValues.put(headerValue, rowValue);
			}
		}

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

				Object converted;
				if (null == exampleValues) {
					converted = conversionService.convert(parameterAsString, parameterType);
				}
				else {
					Matcher tableMatcher = OUTLINE_PATTERN.matcher(parameterAsString);
					checkState(tableMatcher.find(), "Example table keys must be in the format <key>");
					String key = tableMatcher.group(1);
					String tableValue = exampleValues.get(key);
					converted = conversionService.convert(tableValue, parameterType);
				}

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
