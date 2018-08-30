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

package guru.qas.martini.standalone.harness.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.common.util.concurrent.ForwardingExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import guru.qas.martini.standalone.jcommander.Args;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
@Configurable
public class InThreadExecutorService extends ForwardingExecutorService implements InitializingBean, DisposableBean {

	protected Thread.UncaughtExceptionHandler exceptionHandler;
	protected Args args;
	protected ExecutorService delegate;

	@Autowired
	protected void setExceptionHandler(Thread.UncaughtExceptionHandler h) {
		this.exceptionHandler = h;
	}

	@Autowired
	protected void setArgs(Args args) {
		this.args = args;
	}

	@Override
	public void afterPropertiesSet() {
		delegate = MoreExecutors.newDirectExecutorService();
	}

	@Override
	protected ExecutorService delegate() {
		return delegate;
	}

	@Override
	public void execute(@Nonnull Runnable command) {
		checkNotNull(command, "null Runnable");
		try {
			super.execute(command);
		}
		catch (Exception e) {
			exceptionHandler.uncaughtException(Thread.currentThread(), e);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (null != delegate && !delegate.isShutdown()) {
			if (args.awaitTerminationSeconds > 0) {
				delegate.shutdown();
				delegate.awaitTermination(args.awaitTerminationSeconds, TimeUnit.SECONDS);
			}
			else {
				delegate.shutdownNow();
			}
		}
	}
}
