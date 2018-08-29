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

package guru.qas.martini.standalone.jcommander;

import java.io.File;
import java.net.URI;
import java.nio.file.OpenOption;

import org.springframework.context.MessageSource;
import org.springframework.core.io.WritableResource;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterInstanceFactory;
import com.beust.jcommander.Parameter;

import guru.qas.martini.MartiniException;
import guru.qas.martini.i18n.MessageSources;

import static java.nio.file.StandardOpenOption.*;

@SuppressWarnings("WeakerAccess")
public class WritableResourceConverterFactory implements IStringConverterInstanceFactory, IStringConverter<WritableResource> {

	protected final Args args;

	public WritableResourceConverterFactory(Args args) {
		this.args = args;
	}

	@Override
	public IStringConverter<?> getConverterInstance(Parameter parameter, Class<?> aClass, String s) {
		return WritableResource.class.equals(aClass) ? this : null;
	}

	@Override
	public WritableResource convert(String location) {
		String trimmed = null == location ? "" : location.trim();
		return trimmed.isEmpty() ? null : getResource(trimmed, args.jsonOverwrite); // TODO: check ordering
	}

	protected WritableResource getResource(String location, boolean overwrite) {
		try {
			URI uri = new URI(location);
			File file = new File(uri);
			OpenOption[] options = new OpenOption[]{overwrite ? CREATE : CREATE_NEW, TRUNCATE_EXISTING};
			return new OptionedFileSystemResource(file, options);
		}
		catch (Exception e) {
			MessageSource messageSource = MessageSources.getMessageSource(getClass());
			throw new MartiniException.Builder()
				.setCause(e)
				.setKey("exception.creating.resource")
				.setArguments(location)
				.setMessageSource(messageSource)
				.build();
		}
	}
}
