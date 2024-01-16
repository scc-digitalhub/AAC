/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.files.store;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public class LocalFileStore implements FileStore {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private String basePath;
	
	private Path root;

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public void setPath(String realm) {
		this.root = Paths.get(getBasePath() + System.getProperty("file.separator") + realm);
		logger.debug("Local file store initialized,  path set to " + this.root);
	}

	@Override
	public void save(String id, String realm, InputStream str, long size) {
		try {
			setPath(realm);
			Files.createDirectories(root);
			Files.copy(str, root.resolve(id));
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	@Override
	public InputStream load(String id, String realm) {
		try {
			setPath(realm);
			Path file = root.resolve(id);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource.getInputStream();
			} else {
				throw new FileNotFoundException("Could not read the file!");
			}
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Error: " + e.getMessage());
		} catch (IOException e) {
			logger.error("Could not read the file!");
		}
		return null;
	}

	@Override
	public void delete(String id, String realm) {
		try {
			setPath(realm);
			Path file = root.resolve(id);
			Files.deleteIfExists(file);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error: " + e.getMessage());
		}
	}

}