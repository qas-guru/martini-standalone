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

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.WritableResource;
import org.springframework.lang.Nullable;

import static com.google.common.base.Preconditions.checkState;

public class WritableJsonResourceProperties extends PropertySource<Args> {

	public static final String PROPERTY = "martini.standalone.json.output.resource";

	private static final AtomicBoolean RETRIEVED = new AtomicBoolean(false);
	private static final AtomicReference<WritableResource> REF = new AtomicReference<>();

	public WritableJsonResourceProperties(Args source) {
		super("Args", source);
	}

	@Nullable
	@Override
	public Object getProperty(@Nonnull String s) {
		return PROPERTY.equals(s) ? getResource() : null;
	}

	private WritableResource getResource() {
		synchronized (RETRIEVED) {
			if (!RETRIEVED.getAndSet(true)) {
				String location = source.getJsonOutputResource();
				REF.set(null == location || location.isEmpty() ? null : getResource(location));
			}
		}
		return REF.get();
	}

	private WritableResource getResource(String location) {
		try {
			URI uri = new URI(location);
			return getResource(uri);
		} catch (Exception e) {
			throw new RuntimeException("invalid output resource: " + location, e);
		}

	}

	private WritableResource getResource(URI uri) {
		PathResource resource = new PathResource(uri);
		checkState(resource.isWritable(), "resource %s not writable", resource);
		return resource;
	}
}
