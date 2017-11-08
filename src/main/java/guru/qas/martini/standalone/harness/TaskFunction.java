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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;

import com.google.common.base.Function;
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

	SuiteIdentifier getSuiteIdentifier() {
		return suiteIdentifier;
	}

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
			DefaultSuiteIdentifier.Builder builder = DefaultSuiteIdentifier.builder();
			setSystemInformation(builder);
			setSpringInformation(context, builder);
			return builder.build();
		}

		private static void setSystemInformation(DefaultSuiteIdentifier.Builder builder) {
			setHostInformation(builder);
			setUserInformation(builder);
			setEnvironmentInformation(builder);
		}

		private static void setHostInformation(DefaultSuiteIdentifier.Builder builder) {
			try {
				InetAddress localHost = InetAddress.getLocalHost();
				String hostname = localHost.getHostName();
				builder.setHostname(hostname);
				String address = localHost.getHostAddress();
				builder.setHostAddress(address);
			}
			catch (UnknownHostException e) {
				builder.setHostAddress("unknown");
				builder.setHostAddress("unknown");
			}
		}

		private static void setUserInformation(DefaultSuiteIdentifier.Builder builder) {
			String username = System.getProperty("user.name");
			builder.setUsername(username);
		}

		private static void setEnvironmentInformation(DefaultSuiteIdentifier.Builder builder) {
			Map<String, String> environmentVariables = System.getenv();
			builder.setEnvironmentVariables(environmentVariables);
		}

		private static void setSpringInformation(ApplicationContext context, DefaultSuiteIdentifier.Builder builder) {
			setEnvironmentInformation(context, builder);
			setContextInformation(context, builder);
		}

		private static void setEnvironmentInformation(
			ApplicationContext context,
			DefaultSuiteIdentifier.Builder builder
		) {
			Environment environment = context.getEnvironment();
			String[] activeProfiles = environment.getActiveProfiles();
			ArrayList<String> profileList = Lists.newArrayList(activeProfiles);
			builder.setProfiles(profileList);
		}

		private static void setContextInformation(ApplicationContext context, DefaultSuiteIdentifier.Builder builder) {
			String name = context.getDisplayName();
			builder.setName(name);

			long timestamp = context.getStartupDate();
			builder.setStartupTimestamp(timestamp);
		}
	}
}
