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

package guru.qas.martini.standalone.harness;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import javax.annotation.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Ordering;

import guru.qas.martini.Martini;
import guru.qas.martini.gate.MartiniGate;

@SuppressWarnings("WeakerAccess")
@Configurable
public class GatedMartiniComparator implements Comparator<Martini>, InitializingBean {

	protected Ordering<By> ordering;

	protected GatedMartiniComparator() {
	}

	@Override
	public void afterPropertiesSet() {
		initializeOrdering();
	}

	protected void initializeOrdering() {
		Comparator<By> highestPriority = Ordering.natural().nullsLast().onResultOf(By::highestPriority);
		Ordering<By> byGateCount = Ordering.natural().nullsLast().onResultOf(By::gateCount);

		ordering = Ordering
			.from(highestPriority)
			.compound(byGateCount)
			.onResultOf(by -> by);
	}

	@Override
	public int compare(Martini left, Martini right) {
		By lefty = null == left ? null : By.builder().build(left);
		By righty = null == right ? null : By.builder().build(right);
		return ordering.compare(lefty, righty);
	}

	@SuppressWarnings("WeakerAccess")
	protected static final class By {

		private final LinkedHashMultimap<String, Integer> index;

		protected By(LinkedHashMultimap<String, Integer> index) {
			this.index = index;
		}

		public int highestPriority() {
			return index.values().stream().min(Ordering.natural()).orElse(Integer.MAX_VALUE);
		}

		public int gateCount() {
			return Integer.MAX_VALUE - index.keySet().size();
		}

		public static Builder builder() {
			return new Builder();
		}

		protected static class Builder {

			protected Builder() {
			}

			protected By build(@Nullable Martini martini) {
				Collection<MartiniGate> gates = null == martini ? null : martini.getGates();
				LinkedHashMultimap<String, Integer> index = null == gates ? getIndex(Collections.emptySet()) : getIndex(gates);
				return new By(index);
			}

			protected LinkedHashMultimap<String, Integer> getIndex(Collection<MartiniGate> gates) {
				LinkedHashMultimap<String, Integer> index = LinkedHashMultimap.create();
				gates.forEach(gate -> {
					String name = gate.getName();
					int priority = gate.getPriority();
					index.put(name, priority);
				});
				return index;
			}
		}
	}
}
