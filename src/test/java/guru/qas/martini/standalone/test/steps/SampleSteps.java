/*
Copyright 2017 Penny Rohr Curich

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

package guru.qas.martini.standalone.test.steps;

import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.qas.martini.annotation.And;
import guru.qas.martini.annotation.Gated;
import guru.qas.martini.annotation.Given;
import guru.qas.martini.annotation.Steps;
import guru.qas.martini.annotation.Then;
import guru.qas.martini.annotation.When;

@Steps
public class SampleSteps {

	private static final Logger LOGGER = LoggerFactory.getLogger(SampleSteps.class);

	private final SecureRandom random;

	SampleSteps() {
		random = new SecureRandom();
	}

	@Given("^a precondition$")
	public void aPrecondition() {
		sleep();
	}

	@When("^something happens$")
	public void somethingHappens() {
		sleep();
	}

	@Gated(name = "sampleGate")
	@And("^a gate is encountered$")
	public void setGate() {
		sleep();
	}


	@Then("^outcome is evaluated$")
	public void outcomeIsEvaluated() {
		sleep();
	}

	private void sleep() {
		int millis = random.nextInt(3000);
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			LOGGER.error("encountered exception while sleeping", e);
		}
	}
}
