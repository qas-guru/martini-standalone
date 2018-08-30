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

import java.util.Comparator;

import org.springframework.beans.factory.annotation.Configurable;

import guru.qas.martini.Martini;

@Configurable
public class GatedMartiniComparator implements Comparator<Martini> {

	@Override
	public int compare(Martini left, Martini right) {
		throw new UnsupportedOperationException();
	}
}
