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

package guru.qas.martini.standalone;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.WritableResource;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;

import guru.qas.martini.standalone.harness.DefaultUncaughtExceptionHandler;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("ConstantConditions")

class Args {
	@Parameter(
		names = "-jsonOutputResource",
		description = "Spring WriteableResource destination for JSON suite reporting")
	private String jsonOutputResource;

	@Parameter(
		names = "-configLocations",
		description = "Comma-separated list of Spring configuration files")
	private List<String> configLocations = Lists.newArrayList("classpath*:**/applicationContext.xml");

	@Parameter(
		names = "-spelFilter",
		description = "Spring SPel expression indicating which scenarios should be executed")
	private String spelFilter;

	@Parameter(
		names = "-parallelism",
		description = "Fork Join Pool parallelism")
	private Integer parallelism;

	@Parameter(
		names = "-awaitTerminationSeconds",
		description = "number of seconds Fork Join Pool will wait before forcing termination")
	private Integer awaitTerminationSeconds = 30;

	@Parameter(
		names = "-uncaughtExceptionHandlerImplementation",
		description = "fully qualified name of Fork Join Pool's Thread.UncaughtExceptionHandler")
	private String uncaughtExceptionHandlerImplementation;

	@Parameter(
		names = "-timeoutInMinutes",
		description = "period of time after which suite should exit")
	private Integer timeoutInMinutes = 60;

	String[] getConfigLocations() {
		return configLocations.toArray(new String[configLocations.size()]);
	}

	String getSpelFilter() {
		return null == spelFilter ? null : spelFilter.trim();
	}

	int getParallelism() {
		checkArgument(null == parallelism || parallelism > 0,
			"invalid parallelism %s; must be greater than zero", parallelism);
		Integer value = parallelism;
		if (null == value) {
			value = Runtime.getRuntime().availableProcessors();
		}
		return value;
	}

	int getAwaitTerminationSeconds() {
		checkArgument(awaitTerminationSeconds > 0,
			"invalid awaitTerminationSeconds %s; must be greater than zero", awaitTerminationSeconds);

		return awaitTerminationSeconds;
	}

	Class<? extends Thread.UncaughtExceptionHandler> getUncaughtExceptionHandlerImplementation() throws ClassNotFoundException {
		Class c = null == uncaughtExceptionHandlerImplementation ?
			null : Class.forName(uncaughtExceptionHandlerImplementation);
		checkArgument(null == c || Thread.UncaughtExceptionHandler.class.isAssignableFrom(c),
			"Class is not an implementation of UncaughtExceptionHandler: %s", uncaughtExceptionHandlerImplementation);
		return null == c ? DefaultUncaughtExceptionHandler.class : c;
	}

	Integer getTimeoutInMinutes() {
		checkArgument(null == timeoutInMinutes || timeoutInMinutes > 0,
			"invalid timeoutInMinutes %s; must be greater than zero", timeoutInMinutes);
		return timeoutInMinutes;
	}

	WritableResource getJsonOutputResource() {
		WritableResource resource = null;
		if (null != jsonOutputResource && !jsonOutputResource.trim().isEmpty()) {
			try {
				URI uri = new URI(jsonOutputResource.trim());
				return getJsonOutputResource(uri);
			}
			catch (Exception e) {
				throw new RuntimeException("unable to write to JSON resource: " + jsonOutputResource, e);
			}
		}
		return resource;
	}

	private WritableResource getJsonOutputResource(URI uri) throws IOException {
		WritableResource resource = new PathResource(uri);
		File file = resource.getFile();
		if (file.exists()) {
			checkState(!file.isDirectory(), "jsonOutputResource is a directory: %s", resource);
			checkState(file.canWrite(), "unable to write to jsonOutputResource %s", resource);
		}
		else {
			checkState(file.createNewFile(), "unable to create jsonOutputResource file %s", resource);
		}
		return resource;
	}
}
