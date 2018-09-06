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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Configurable;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.Monitor;

import guru.qas.martini.Martini;
import guru.qas.martini.gate.MartiniGate;

import static com.google.common.base.Preconditions.checkArgument;

@SuppressWarnings("WeakerAccess")
@Configurable
public class MartiniIterator implements Iterator<Optional<Martini>> {

	protected final long pollTimeoutMs;
	protected final ConcurrentLinkedDeque<Martini> gated;
	protected final ConcurrentLinkedDeque<Martini> ungated;
	protected final Monitor monitor;

	protected MartiniIterator(
		long pollTimeoutMs, ConcurrentLinkedDeque<Martini> gated,
		ConcurrentLinkedDeque<Martini> ungated
	) {
		this.pollTimeoutMs = pollTimeoutMs;
		this.gated = gated;
		this.ungated = ungated;
		this.monitor = new Monitor();
	}

	@Override
	public boolean hasNext() {
		return !gated.isEmpty() || !ungated.isEmpty();
	}

	@Override
	public Optional<Martini> next() {
		Optional<Martini> next = getNextGated();
		return next.isPresent() ? next : getNextUngated();
	}

	protected Optional<Martini> getNextGated() {
		Martini next;
		try {
			monitor.enterInterruptibly(500, TimeUnit.MILLISECONDS);
			try {
				next = gated.stream()
					.filter(this::lock)
					.findFirst()
					.orElse(null);
				if (null != next) {
					gated.remove(next);
				}
			}
			finally {
				monitor.leave();
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException("interrupted while waiting on Monitor", e);
		}
		return Optional.ofNullable(next);
	}

	protected boolean lock(Martini martini) {
		Set<String> gateNames = new HashSet<>();
		return martini.getGates().stream()
			.filter(gate -> {
				String name = gate.getName();
				return gateNames.add(name);
			})
			.map(MartiniGate::enter)
			.filter(permitted -> !permitted)
			.findFirst()
			.orElse(true);
	}

	protected Optional<Martini> getNextUngated() {
		Martini martini = ungated.pollFirst();
		return Optional.ofNullable(martini);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void forEachRemaining(Consumer<? super Optional<Martini>> action) {
		throw new UnsupportedOperationException();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected long pollTimeoutMs;
		protected final List<Martini> martinis;
		protected Comparator<Martini> gatedComparator;
		protected Comparator<Martini> ungatedComparator;

		protected Builder() {
			pollTimeoutMs = 500;
			martinis = new ArrayList<>();
		}

		public Builder setPollTimeoutMs(long l) {
			this.pollTimeoutMs = l;
			return this;
		}

		public Builder setMartinis(Collection<Martini> martinis) {
			this.martinis.clear();
			if (null != martinis) {
				this.martinis.addAll(martinis);
			}
			return this;
		}

		public Builder setGated(Comparator<Martini> ordering) {
			this.gatedComparator = ordering;
			return this;
		}

		@SuppressWarnings("unused")
		public Builder setUngated(Comparator<Martini> ordering) {
			this.ungatedComparator = ordering;
			return this;
		}

		public MartiniIterator build() {
			checkArgument(pollTimeoutMs > 0,
				"illegal poll timeout %s; must be greater than zero milliseconds", pollTimeoutMs);

			int martiniCount = martinis.size();
			List<Martini> gated = Lists.newArrayListWithCapacity(martiniCount);
			List<Martini> ungated = Lists.newArrayListWithCapacity(martiniCount);

			martinis.stream()
				.filter(Objects::nonNull)
				.forEach(martini -> {
					Collection<MartiniGate> gates = martini.getGates();
					Collection<Martini> destination = gates.isEmpty() ? ungated : gated;
					destination.add(martini);
				});

			List<Martini> sortedGated = null == gatedComparator ?
				gated : Ordering.from(gatedComparator).sortedCopy(gated);

			List<Martini> sortedUngated = null == ungatedComparator ?
				ungated : Ordering.from(ungatedComparator).sortedCopy(ungated);

			return new MartiniIterator(
				pollTimeoutMs, new ConcurrentLinkedDeque<>(sortedGated),
				new ConcurrentLinkedDeque<>(sortedUngated));
		}
	}
}
