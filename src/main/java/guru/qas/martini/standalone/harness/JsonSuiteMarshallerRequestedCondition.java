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

package guru.qas.martini.standalone.harness;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import guru.qas.martini.standalone.jcommander.Args;
import guru.qas.martini.standalone.jcommander.ArgsPropertySource;

import static com.google.common.base.Preconditions.checkState;

public class JsonSuiteMarshallerRequestedCondition implements Condition {

	@Override
	public boolean matches(@Nonnull ConditionContext context, @Nonnull AnnotatedTypeMetadata metadata) {
		Environment environment = context.getEnvironment();
		Args args = environment.getProperty(ArgsPropertySource.PROPERTY, Args.class);
		checkState(null != args, "unable to retrieve Args");
		return null != args.jsonOutputFile;
	}
}
