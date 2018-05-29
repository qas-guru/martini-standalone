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

package guru.qas.martini.standalone;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.DisposableBean;

import com.google.common.util.concurrent.ForwardingExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class InThreadExecutorService extends ForwardingExecutorService implements DisposableBean {

	protected static final InheritableThreadLocal<ExecutorService> THREAD_LOCAL = new InheritableThreadLocal<>();
	protected final Thread.UncaughtExceptionHandler handler;
	protected final int awaitTerminationSeconds;

	public InThreadExecutorService(Thread.UncaughtExceptionHandler handler, int awaitTerminationSeconds) {
		this.handler = checkNotNull(handler, "null Thread.UncaughtExceptionHandler");
		this.awaitTerminationSeconds = awaitTerminationSeconds;
	}

	@Override
	protected ExecutorService delegate() {
		ExecutorService executorService = THREAD_LOCAL.get();
		if (null == executorService) {
			executorService = MoreExecutors.newDirectExecutorService();
			THREAD_LOCAL.set(executorService);
		}
		return executorService;
	}

	@Override
	public void execute(@Nonnull Runnable command) {
		checkNotNull(command, "null Runnable");
		try {
			super.execute(command);
		}
		catch (Exception e) {
			handler.uncaughtException(Thread.currentThread(), e);
		}
	}

	@Override
	public void destroy() {
		ExecutorService executorService = THREAD_LOCAL.get();
		if (null != executorService && !executorService.isShutdown()) {
			executorService.shutdown();
			if (this.awaitTerminationSeconds > 0) {
				try {
					executorService.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS);
				}
				catch (InterruptedException ignored) {
				}
			}
		}
	}
}
