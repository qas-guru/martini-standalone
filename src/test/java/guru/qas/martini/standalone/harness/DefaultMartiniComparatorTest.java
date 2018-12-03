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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import guru.qas.martini.Martini;
import guru.qas.martini.gate.MartiniGate;
import guru.qas.martini.standalone.TestMartini;
import guru.qas.martini.standalone.TestMartiniGate;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings("WeakerAccess")
public class DefaultMartiniComparatorTest {

	protected DefaultMartiniComparator comparator;

	@BeforeClass
	public void setUp() {
		comparator = new DefaultMartiniComparator();
	}

	@Test
	public void testUngatedEquivalent() {
		Martini one = new ComparableMartini(ImmutableSet.of());
		Martini two = new ComparableMartini(ImmutableSet.of());

		checkState(0 == comparator.compare(one, two), "comparator should always return -1 for equal gates");
		checkState(0 == comparator.compare(two, one), "comparator should always return -1 for equal gates");
	}

	@Test
	public void testUngatedListEquivalent() {
		Martini one = new ComparableMartini(ImmutableSet.of());
		Martini two = new ComparableMartini(ImmutableSet.of());
		Martini three = new ComparableMartini(ImmutableSet.of());

		ArrayList<Martini> sorted = Lists.newArrayList(three, one, two);
		sorted.sort(comparator);

		ImmutableList<Martini> expected = ImmutableList.of(three, one, two);
		checkState(sorted.equals(expected), "wrong sort order returned");
	}

	@Test
	public void testGatedSortsLower() {
		Martini one = new ComparableMartini(ImmutableSet.of());
		Martini two = new ComparableMartini(Collections.singleton(new ComparableMartiniGate(10)));

		checkState(0 < comparator.compare(one, two), "ungated should short higher");
		checkState(0 > comparator.compare(two, one), "gated should sort lower");
	}

	@Test
	public void testSortByLowestPermitCount() {
		Martini one = new ComparableMartini(Collections.singleton(new ComparableMartiniGate(6)));
		Martini two = new ComparableMartini(Collections.singleton(new ComparableMartiniGate(5)));
		Martini three = new ComparableMartini(Collections.singleton(new ComparableMartiniGate(3)));

		ArrayList<Martini> martinis = Lists.newArrayList(one, two, three);
		martinis.sort(comparator);

		ArrayList<Martini> expected = Lists.newArrayList(three, two, one);
		checkState(expected.equals(martinis), "wrong sort returned");
	}

	@Test
	public void testSortByHighestGateCount() {
		Martini one = new ComparableMartini(Collections.singleton(new ComparableMartiniGate(5)));
		Martini two = new ComparableMartini(Collections.singleton(new ComparableMartiniGate(5)));
		ImmutableList<MartiniGate> gates = ImmutableList.of(new ComparableMartiniGate(7), new ComparableMartiniGate(3));
		Martini three = new ComparableMartini(gates);

		ArrayList<Martini> martinis = Lists.newArrayList(one, two, three);
		martinis.sort(comparator);

		ArrayList<Martini> expected = Lists.newArrayList(three, one, two);
		checkState(expected.equals(martinis), "wrong sort returned");
	}

	@Test
	public void testVariedSort() {
		Martini one = new ComparableMartini(ImmutableList.of());
		Martini two = new ComparableMartini(
			ImmutableList.of(new ComparableMartiniGate(3), new ComparableMartiniGate(3)));
		Martini three = new ComparableMartini(Collections.singleton(new ComparableMartiniGate(3)));
		Martini four = new ComparableMartini(Collections.singleton(new ComparableMartiniGate(1)));

		ArrayList<Martini> martinis = Lists.newArrayList(one, two, three, four);
		martinis.sort(comparator);

		ArrayList<Martini> expected = Lists.newArrayList(four, two, three, one);
		checkState(expected.equals(martinis), "wrong sort returned");
	}

	@AfterClass
	public void tearDown() {
		comparator = null;
	}

	protected class ComparableMartiniGate implements TestMartiniGate {

		private final int permits;

		protected ComparableMartiniGate(int permits) {
			this.permits = permits;
		}

		@Override
		public int getPermits() {
			return permits;
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected class ComparableMartini implements TestMartini {

		private final Collection<MartiniGate> gates;

		protected ComparableMartini(Collection<MartiniGate> gates) {
			this.gates = gates;
		}

		@Override
		public Collection<MartiniGate> getGates() {
			return gates;
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}

		@Override
		public int hashCode() {
			return gates.hashCode();
		}
	}
}
