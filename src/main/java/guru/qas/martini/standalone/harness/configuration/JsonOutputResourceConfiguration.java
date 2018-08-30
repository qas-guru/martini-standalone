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

import java.nio.file.OpenOption;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.WritableResource;

import guru.qas.martini.standalone.jcommander.Args;
import guru.qas.martini.standalone.io.OptionedFileSystemResource;

import static java.nio.file.StandardOpenOption.*;

@Configuration
@Lazy
class JsonOutputResourceConfiguration {

	@Bean(name = "jsonOutputResource")
	WritableResource getJsonOutputResource(Args args) {
		OpenOption[] options = new OpenOption[]{args.jsonOverwrite ? CREATE : CREATE_NEW, TRUNCATE_EXISTING};
		return new OptionedFileSystemResource(args.jsonOutputFile, options);
	}
}
