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

package guru.qas.martini.standalone.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import guru.qas.martini.Martini;
import guru.qas.martini.event.AfterScenarioEvent;
import guru.qas.martini.result.MartiniResult;

import static com.google.common.base.Preconditions.checkState;

@Component
public class TestListener {

	private final Multimap<String, String> executionIndex;

	TestListener() {
		executionIndex = ArrayListMultimap.create();
	}

	@EventListener
	public void handle(AfterScenarioEvent event) {
		MartiniResult result = event.getPayload();
		String threadName = result.getThreadName();
		Martini martini = result.getMartini();
		String id = martini.getId();
		executionIndex.put(threadName, id);
	}

	public void assertMultithreaded() {
		Collection<String> values = executionIndex.values();
		int valueCount = values.size();

		HashSet<String> scenarioIds = Sets.newHashSet(values);
		int scenarioCount = scenarioIds.size();
		checkState(scenarioCount == valueCount, "a scenario was executed more than once");

		Set<String> threadNames = executionIndex.keySet();
		checkState(threadNames.size() > 1, "only one thread executed all tests");
	}
}
