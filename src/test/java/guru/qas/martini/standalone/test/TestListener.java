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

package guru.qas.martini.standalone.test;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import guru.qas.martini.Martini;
import guru.qas.martini.event.AfterScenarioEvent;
import guru.qas.martini.result.MartiniResult;

@SuppressWarnings("WeakerAccess")
@Component
public class TestListener {

	protected final Multimap<String, String> executionIndex;
	protected final Logger logger;

	public Multimap<String, String> getExecutionIndex() {
		return ImmutableMultimap.copyOf(executionIndex);
	}

	public TestListener() {
		executionIndex = ArrayListMultimap.create();
		logger = LoggerFactory.getLogger(getClass());
	}

	@EventListener
	public void handle(AfterScenarioEvent event) {
		try {
			MartiniResult result = event.getPayload();
			String threadName = result.getThreadName();
			Martini martini = result.getMartini();
			String id = martini.getId();
			synchronized (executionIndex) {
				executionIndex.put(threadName, id);
			}
		}
		catch (Exception e) {
			logger.warn("unable to handle AfterScenarioEvent", e);
		}
	}
}
