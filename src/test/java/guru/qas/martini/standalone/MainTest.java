package guru.qas.martini.standalone;/*
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(MainTest.class);

//	public void testHarness() throws Exception {
//		int availableProcessors = Runtime.getRuntime().availableProcessors();
//		LOGGER.info("running with {} CPUs", availableProcessors);
//
//		checkState(1 < availableProcessors,
//			"unable to reasonably execute multithreaded test on single-CPU system");
//
//		Args args = new Args();
//		JCommander.newBuilder().addObject(args).build().parse();
//		Main application = new Main(args);
//
//		ConfigurableApplicationContext context = application.getApplicationContext();
//		TestListener listener = context.getBean(TestListener.class);
//		application.executeSuite(context);
//
//		listener.assertMultithreaded();
//	}

	public static void main(String[] args) throws Exception {
		Main.main(args);
	}
}
