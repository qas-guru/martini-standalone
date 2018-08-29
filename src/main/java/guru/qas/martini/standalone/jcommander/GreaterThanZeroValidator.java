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

import org.springframework.context.MessageSource;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

import guru.qas.martini.MartiniException;
import guru.qas.martini.i18n.MessageSources;

public class GreaterThanZeroValidator implements IValueValidator<Integer> {

	@Override
	public void validate(String s, Integer integer) throws ParameterException {
		if (0 >= integer) {
			MessageSource messageSource = MessageSources.getMessageSource(getClass());
			MartiniException cause = new MartiniException.Builder()
				.setKey("invalid.parameter")
				.setArguments(s, integer)
				.setMessageSource(messageSource)
				.build();
			throw new ParameterException(cause);
		}
	}
}
