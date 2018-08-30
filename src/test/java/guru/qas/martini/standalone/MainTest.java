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

package guru.qas.martini.standalone;

import org.springframework.context.ConfigurableApplicationContext;
import org.testng.annotations.Test;

import com.beust.jcommander.JCommander;

import guru.qas.martini.standalone.jcommander.Args;
import guru.qas.martini.standalone.test.TestListener;

public class MainTest {

	@Test
	public void testMultithreading() throws Exception {

		String[] argv = new String[]{"-parallelism", "10"};
		Args args = new Args();
		JCommander.newBuilder().addObject(args).build().parse(argv);
		Main application = new Main(args);

		ConfigurableApplicationContext context = application.getApplicationContext();
		TestListener listener = context.getBean(TestListener.class);
		application.executeSuite(context);

		listener.assertMultithreaded();
	}
}
