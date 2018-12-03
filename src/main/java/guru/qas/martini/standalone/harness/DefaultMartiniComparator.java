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

import java.util.Comparator;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import guru.qas.martini.Martini;
import guru.qas.martini.gate.MartiniGate;

@SuppressWarnings("WeakerAccess")
public class DefaultMartiniComparator implements Comparator<Martini> {

	private Ordering<Martini> ordering;

	public DefaultMartiniComparator() {
		Comparator<Martini> permitCountComparator = Ordering.natural().onResultOf(new PermitCountFunction());
		Comparator<Martini> gateCountComparator = Ordering.natural().onResultOf(new GateCountFunction());
		ordering = Ordering.from(permitCountComparator).compound(gateCountComparator);
	}

	@Override
	public int compare(Martini left, Martini right) {
		return ordering.compare(left, right);
	}

	protected class PermitCountFunction implements Function<Martini, Integer> {

		@Override
		public Integer apply(Martini martini) {
			return null == martini ? null : martini.getGates().stream()
				.map(MartiniGate::getPermits)
				.min(Ordering.natural())
				.orElse(Integer.MAX_VALUE);
		}
	}

	protected class GateCountFunction implements Function<Martini, Integer> {

		@Override
		public Integer apply(Martini martini) {
			return null == martini ? null : Sets.newHashSet(martini.getGates()).size() * -1;
		}
	}
}
