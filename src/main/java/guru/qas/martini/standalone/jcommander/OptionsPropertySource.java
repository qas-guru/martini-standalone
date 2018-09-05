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

import javax.annotation.Nonnull;

import org.springframework.core.env.PropertySource;

import guru.qas.martini.standalone.harness.Options;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class OptionsPropertySource extends PropertySource<Options> {

	public static final String PROPERTY = "martini.engine.options";

	public OptionsPropertySource(@Nonnull Options source) {
		super(PROPERTY, checkNotNull(source, "null Args"));
	}

	@Override
	public Object getProperty(@Nonnull String s) {
		checkNotNull(s, "null String");
		return PROPERTY.equals(s) ? source : null;
	}
}