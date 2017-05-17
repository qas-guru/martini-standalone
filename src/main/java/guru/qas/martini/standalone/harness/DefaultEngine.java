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

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;

import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;
import guru.qas.martini.result.MartiniResult;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultEngine implements Engine {
	private final ApplicationContext context;
	private final Mixologist mixologist;

	@Autowired
	DefaultEngine(ApplicationContext context, Mixologist mixologist) {
		this.context = context;
		this.mixologist = mixologist;
	}

	@Override
	public void executeSuite(
		String filter,
		ForkJoinPool pool,
		Integer timeoutInMinutes
	) throws InterruptedException, ExecutionException {
		checkState(null != pool, "null ForkJoinPool");

		Collection<Martini> martinis = getMartinis(filter);
		TaskFunction function = TaskFunction.builder().build(context);
		for (Martini martini : martinis) {
			Callable<MartiniResult> callable = function.apply(martini);
			pool.submit(ForkJoinTask.adapt(checkNotNull(callable)));
		}
		pool.awaitQuiescence(timeoutInMinutes, TimeUnit.MINUTES);
	}

	protected Collection<Martini> getMartinis(String filter) {
		Collection<Martini> martinis = null == filter || filter.isEmpty() ?
			mixologist.getMartinis() : mixologist.getMartinis(filter);
		checkState(!martinis.isEmpty(),
			null == filter || filter.isEmpty() ? "no Martinis found" : "no Martini found matching spel filter %s", filter);
		return martinis;
	}
}
