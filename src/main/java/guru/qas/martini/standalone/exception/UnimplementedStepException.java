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

package guru.qas.martini.standalone.exception;

import gherkin.ast.Step;
import guru.qas.martini.Martini;

public class UnimplementedStepException extends Exception {

	protected static final String TEMPLATE = "unimplemented step: %s line %s: @%s %s";

	public UnimplementedStepException(Martini martini, Step step) {
		super(getMessage(martini, step));

	}

	public static String getMessage(Martini martini, Step step) {
		String description = martini.getRecipe().getSource().getDescription();
		int line = step.getLocation().getLine();
		String keyword = step.getKeyword().trim();
		String text = step.getText().trim();
		return String.format(TEMPLATE, description, line, keyword, text);
	}
}
