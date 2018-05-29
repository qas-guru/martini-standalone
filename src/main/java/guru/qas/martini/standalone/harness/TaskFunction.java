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

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Function;

import guru.qas.martini.Martini;
import guru.qas.martini.event.DefaultSuiteIdentifier;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.runtime.harness.MartiniCallable;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class TaskFunction implements Function<Martini, Callable<MartiniResult>> {

	protected final AutowireCapableBeanFactory beanFactory;
	protected final SuiteIdentifier suiteIdentifier;

	SuiteIdentifier getSuiteIdentifier() {
		return suiteIdentifier;
	}

	TaskFunction(
		AutowireCapableBeanFactory beanFactory,
		SuiteIdentifier suiteIdentifier
	) {
		this.beanFactory = beanFactory;
		this.suiteIdentifier = suiteIdentifier;
	}

	@Override
	public Callable<MartiniResult> apply(@Nonnull Martini martini) {
		checkNotNull(martini, "null Martini");
		Callable<MartiniResult> callable = new MartiniCallable(suiteIdentifier, martini);
		beanFactory.autowireBean(callable);
		return callable;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		public TaskFunction build(ApplicationContext context) {
			checkNotNull(context, "null ApplicationContext");
			SuiteIdentifier suiteIdentifier = DefaultSuiteIdentifier.builder().build(context);
			AutowireCapableBeanFactory beanFactory = context.getAutowireCapableBeanFactory();
			return new TaskFunction(beanFactory, suiteIdentifier);
		}
	}
}
