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

package guru.qas.martini.standalone;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.beust.jcommander.JCommander;

@SuppressWarnings("WeakerAccess")
public class Main {

	public void doSomething(String[] argv) {
		try (ConfigurableApplicationContext context = getApplicationContext(argv)) {
			// TODO: start up the spring application context, INCLUDING an event bus
		}
	}

	protected ConfigurableApplicationContext getApplicationContext(String[] argv) {
		Args args = new Args();
		JCommander.newBuilder().addObject(args).build().parse(argv);
		String[] configLocations = args.getConfigLocations();
		return new ClassPathXmlApplicationContext(configLocations);
	}

	public static void main(String[] args) {
		Main application = new Main();
		application.doSomething(args);
	}
}
