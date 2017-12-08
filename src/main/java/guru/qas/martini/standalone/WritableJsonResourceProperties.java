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
import java.net.URI;
import java.nio.file.OpenOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.springframework.core.env.PropertySource;
import org.springframework.core.io.WritableResource;
import org.springframework.lang.Nullable;


import static java.nio.file.StandardOpenOption.*;

@SuppressWarnings("WeakerAccess")
public class WritableJsonResourceProperties extends PropertySource<Args> {

	public static final String PROPERTY = "martini.standalone.json.output.resource";

	protected static final AtomicBoolean RETRIEVED = new AtomicBoolean(false);
	protected static final AtomicReference<WritableResource> REF = new AtomicReference<>();

	public WritableJsonResourceProperties(Args source) {
		super("Args", source);
	}

	@Nullable
	@Override
	public Object getProperty(@Nonnull String s) {
		return PROPERTY.equals(s) ? getResource() : null;
	}

	protected WritableResource getResource() {
		synchronized (RETRIEVED) {
			if (!RETRIEVED.getAndSet(true)) {
				String location = source.getJsonOutputResource();
				if (null != location && !location.isEmpty()) {
					boolean append = source.isJsonAppend();
					REF.set(getResource(location, append));
				}
			}
		}
		return REF.get();
	}

	protected WritableResource getResource(String location, boolean append) {
		try {
			URI uri = new URI(location);
			File file = new File(uri);
			OpenOption[] options = new OpenOption[]{CREATE, append ? APPEND : TRUNCATE_EXISTING};
			return new OptionedFileSystemResource(file, options);
		}
		catch (Exception e) {
			throw new RuntimeException("unable to create writable resource: " + location, e);
		}
	}
}
