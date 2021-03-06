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

package guru.qas.martini.spring.standalone.configuration;

import java.io.File;
import java.nio.file.OpenOption;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.WritableResource;

import guru.qas.martini.standalone.harness.Options;
import guru.qas.martini.standalone.io.OptionedFileSystemResource;

import static java.nio.file.StandardOpenOption.*;

@Configuration
@Lazy
public class JsonOutputResourceConfiguration {

	public static final String BEAN_NAME = "jsonOutputResource";

	@Bean(name = BEAN_NAME)
	WritableResource getJsonOutputResource(Options options) {
		File file = options.getJsonOutputFile().orElseThrow(() -> new IllegalStateException("null File"));
		OpenOption[] openOptions = new OpenOption[]{
			options.isJsonOutputFileOverwrite() ? CREATE : CREATE_NEW, TRUNCATE_EXISTING};
		return new OptionedFileSystemResource(file, openOptions);
	}
}
