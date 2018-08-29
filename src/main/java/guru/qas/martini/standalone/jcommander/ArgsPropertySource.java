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

import java.lang.reflect.Field;

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.springframework.core.env.PropertySource;

import com.beust.jcommander.Parameter;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class ArgsPropertySource extends PropertySource<Args> {

	public ArgsPropertySource(@Nonnull Args source) {
		super("jCommanderArguments", checkNotNull(source, "null Args"));
	}

	@Override
	public Object getProperty(@Nonnull String property) {

		Class<? extends Args> implementation = source.getClass();
		Field[] fields = implementation.getDeclaredFields();

		Field match = Arrays.stream(fields).filter(field -> {
			Parameter annotation = field.getAnnotation(Parameter.class);
			String[] names = null == annotation ? new String[0] : annotation.names();
			return Arrays.asList(names).contains(property);
		}).findFirst().orElse(null);

		return null == match ? null : getProperty(match);
	}

	protected Object getProperty(Field field) {
		try {
			return field.get(source);
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException("unable to extract property from Args Field " + field, e);
		}
	}
}