/*
Copyright 2018-2019 Penny Rohr Curich

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

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import exception.MartiniException;

import static guru.qas.martini.standalone.jcommander.ClassConverterMessages.*;

@SuppressWarnings("WeakerAccess")
public class ClassConverter implements IStringConverter<Class> {

	@Override
	public Class convert(String s) throws ParameterException {
		String trimmed = null == s ? "" : s.trim();
		try {
			assertArgumentProvided(trimmed);
			return Class.forName(s);
		}
		catch (ClassNotFoundException e) {
			MartiniException cause = new MartiniException(e, INVALID_IMPLEMENTATION, s);
			throw new ParameterException(cause);
		}
	}

	protected void assertArgumentProvided(String s) {
		if (s.isEmpty() || s.startsWith("-")) {
			MartiniException cause = new MartiniException(MISSING_IMPLEMENTATION, s);
			throw new ParameterException(cause);
		}
	}
}
