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

package guru.qas.martini.standalone.harness;

import ch.qos.cal10n.BaseName;
import ch.qos.cal10n.Locale;
import ch.qos.cal10n.LocaleData;

@BaseName("guru.qas.martini.standalone.harness.defaultMartiniStandaloneEngineMessages")
@LocaleData({@Locale("en")})
public enum DefaultMartiniStandaloneEngineMessages {
	NO_MARTINIS_FOUND,
	NO_MARTINIS_FOUND_FOR_FILTER,
	UNIMPLEMENTED_STEPS,
	EXECUTION_INTERRUPTED,
	EXECUTION_TIMED_OUT,
}
