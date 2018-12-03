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

package guru.qas.martini.standalone;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.gate.MartiniGate;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.tag.MartiniTag;

public interface TestMartini extends Martini {

	@Override
	default String getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	default Recipe getRecipe() {
		throw new UnsupportedOperationException();
	}

	@Override
	default Map<Step, StepImplementation> getStepIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	default Collection<MartiniGate> getGates() {
		throw new UnsupportedOperationException();
	}

	@Override
	default Collection<MartiniTag> getTags() {
		throw new UnsupportedOperationException();
	}

	@Override
	default String getFeatureName() {
		throw new UnsupportedOperationException();
	}

	@Override
	default String getScenarioName() {
		throw new UnsupportedOperationException();
	}

	@Override
	default int getScenarioLine() {
		throw new UnsupportedOperationException();
	}

	@Override
	default <T extends Annotation> List<T> getStepAnnotations(Class<T> implementation) {
		throw new UnsupportedOperationException();
	}

	@Override
	default boolean isAnyStepAnnotated(Class<? extends Annotation> implementation) {
		throw new UnsupportedOperationException();
	}
}
