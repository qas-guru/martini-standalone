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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Configurable;

import com.google.common.util.concurrent.Monitor;

import guru.qas.martini.Martini;
import guru.qas.martini.gate.MartiniGate;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
@Configurable
public class MartiniIterator implements Iterator<Optional<Martini>> {

	protected final long pollTimeoutMs;
	protected final List<Martini> martinis;
	protected final Monitor monitor;

	protected MartiniIterator(
		long pollTimeoutMs,
		List<Martini> martinis
	) {
		this.pollTimeoutMs = pollTimeoutMs;
		this.martinis = martinis;
		this.monitor = new Monitor();
	}

	@Override
	public boolean hasNext() {
		Optional<Boolean> evaluation = doInLock(() -> !martinis.isEmpty());
		return evaluation.isPresent() ? evaluation.get() : true;
	}

	protected <T> Optional<T> doInLock(Callable<T> callable) {
		try {
			T result = null;
			if (monitor.enterInterruptibly(pollTimeoutMs, TimeUnit.MILLISECONDS)) {
				result = callable.call();
			}
			return Optional.ofNullable(result);
		}
		catch (InterruptedException e) {
			throw new RuntimeException("interrupted while entering Monitor", e);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			monitor.leave();
		}
	}

	@Override
	public Optional<Martini> next() {
		return doInLock(() -> {
			Optional<Martini> optional = martinis.stream().filter(this::lock).findFirst();
			optional.ifPresent(martinis::remove);
			return optional.orElse(null);
		});
	}

	protected boolean lock(Martini martini) {
		Optional<MartiniGate> optional = martini.getGates().stream().filter(gate -> !gate.enter()).findFirst();
		optional.ifPresent(unentered -> martini.getGates().forEach(MartiniGate::leave));
		return !optional.isPresent();
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
		protected Comparator<Martini> comparator;

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

		@SuppressWarnings("UnusedReturnValue")
		public Builder setComparator(Comparator<Martini> comparator) {
			this.comparator = comparator;
			return this;
		}

		public MartiniIterator build() {
			checkState(null != comparator, "Comparator not set");
			checkArgument(pollTimeoutMs > 0,
				"illegal poll timeout %s; must be greater than zero milliseconds", pollTimeoutMs);

			martinis.sort(comparator);
			return new MartiniIterator(pollTimeoutMs, martinis);
		}
	}
}
