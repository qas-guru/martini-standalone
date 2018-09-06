/*
Copyright 2017-2018 Penny Rohr Curich

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

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import guru.qas.martini.standalone.harness.MartiniStandaloneEngine;
import guru.qas.martini.standalone.harness.Options;

@SuppressWarnings("WeakerAccess")
@Configuration
@Lazy
class MartiniStandaloneEngineConfiguration {

	@Bean
	protected MartiniStandaloneEngine getEngine(
		AutowireCapableBeanFactory beanFactory,
		Options options
	) {
		Class<? extends MartiniStandaloneEngine> implementation = options.getEngineImplementation();
		return beanFactory.createBean(implementation);
	}
}