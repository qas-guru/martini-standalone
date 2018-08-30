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

package guru.qas.martini.standalone.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.core.io.FileSystemResource;

import static com.google.common.base.Preconditions.checkNotNull;

public final class OptionedFileSystemResource extends FileSystemResource {

	private final OpenOption[] options;

	public OptionedFileSystemResource(@Nonnull File file, @Nullable OpenOption... options) {
		super(checkNotNull(file, "null File"));
		this.options = null == options ? null : Arrays.copyOf(options, options.length);
	}

	@Nonnull
	@Override
	public OutputStream getOutputStream() throws IOException {
		File file = super.getFile();
		com.google.common.io.Files.createParentDirs(file);
		Path path = file.toPath();
		return Files.newOutputStream(path, options);
	}
}
