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

import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import guru.qas.martini.Martini;
import guru.qas.martini.event.DefaultSuiteIdentifier;
import guru.qas.martini.event.MartiniEventPublisher;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.tag.Categories;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class TaskFunction implements Function<Martini, Callable<MartiniResult>> {

	protected final BeanFactory beanFactory;
	protected final MartiniEventPublisher publisher;
	protected final ConversionService conversionService;
	protected final Categories categories;
	protected final SuiteIdentifier suiteIdentifier;

	TaskFunction(
		BeanFactory beanFactory,
		MartiniEventPublisher publisher,
		ConversionService conversionService,
		Categories categories,
		SuiteIdentifier suiteIdentifier
	) {
		this.beanFactory = beanFactory;
		this.publisher = publisher;
		this.conversionService = conversionService;
		this.categories = categories;
		this.suiteIdentifier = suiteIdentifier;
	}

	@Nullable
	@Override
	public Callable<MartiniResult> apply(@Nullable Martini martini) {
		return null == martini ? null :
			new MartiniTask(
				beanFactory,
				publisher,
				conversionService,
				categories,
				suiteIdentifier,
				martini);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		public TaskFunction build(ApplicationContext context) {
			checkNotNull(context, "null ApplicationContext");
			SuiteIdentifier suiteIdentifier = getSuiteIdentifier(context);
			AutowireCapableBeanFactory beanFactory = context.getAutowireCapableBeanFactory();
			MartiniEventPublisher publisher = context.getBean(MartiniEventPublisher.class);
			ConversionService conversionService = context.getBean(ConversionService.class);
			Categories categories = context.getBean(Categories.class);
			return new TaskFunction(beanFactory, publisher, conversionService, categories, suiteIdentifier);
		}

		private SuiteIdentifier getSuiteIdentifier(ApplicationContext context) {
			Environment environment = context.getEnvironment();
			return DefaultSuiteIdentifier.builder()
				.setEnvironmentVariables(ImmutableMap.of()) // TODO:
				.setHostAddress("address")
				.setHostname("hostname")
				.setId(context.getId())
				.setProfiles(Lists.newArrayList(environment.getActiveProfiles()))
				.setName(context.getDisplayName())
				.setStartupTimestamp(context.getStartupDate())
				.setUsername(System.getProperty("user.name"))
				.build();
		}
	}
}
