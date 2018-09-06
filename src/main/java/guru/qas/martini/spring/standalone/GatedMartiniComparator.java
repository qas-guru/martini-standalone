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

package guru.qas.martini.spring.standalone;

import java.util.Comparator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Ordering;

import guru.qas.martini.Martini;
import guru.qas.martini.standalone.harness.GateIndex;

@SuppressWarnings("WeakerAccess")
@Component
public class GatedMartiniComparator implements Comparator<Martini>, InitializingBean {

	protected Ordering<GateIndex> ordering;

	protected GatedMartiniComparator() {
	}

	@Override
	public void afterPropertiesSet() {
		initializeOrdering();
	}

	protected void initializeOrdering() {
		Comparator<GateIndex> highestPriority = Ordering.natural().nullsLast().onResultOf(GateIndex::highestPriority);
		Ordering<GateIndex> byGateCount = Ordering.natural().nullsLast().onResultOf(GateIndex::gateCount);

		ordering = Ordering
			.from(highestPriority)
			.compound(byGateCount)
			.onResultOf(by -> by);
	}

	@Override
	public int compare(Martini left, Martini right) {
		GateIndex leftIndex = null == left ? null : GateIndex.builder().build(left);
		GateIndex rightIndex = null == right ? null : GateIndex.builder().build(right);
		return ordering.compare(leftIndex, rightIndex);
	}
}
