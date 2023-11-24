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

package it.smartcommunitylab.aac.files.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.files.persistence.FileInfo;
import it.smartcommunitylab.aac.files.persistence.FileDBRepository;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Path root = Paths.get("uploads");
	@Autowired
	private FileDBRepository fileDBRepository;

	@Override
	@PostConstruct
	public void init() {
		try {
			Files.createDirectories(root);
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not initialize folder for upload!");
		}
	}

	@Override
	public void save(String fileName, String contentType, InputStream str) {
		try {
			FileInfo fileDB = new FileInfo(fileName, contentType);
			fileDB.setId(generateUuid(fileName));
			fileDB = fileDBRepository.save(fileDB);
			Files.copy(str, this.root.resolve(fileDB.getId()));
			str.close();
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	@Override
	public InputStream load(String id) {
		try {
			FileInfo fileDB = fileDBRepository.findOne(id);
			Path file = root.resolve(fileDB.getId());
			Resource resource = new UrlResource(file.toUri());

			if (resource.exists() || resource.isReadable()) {
				return resource.getInputStream();
			} else {
				throw new FileNotFoundException("Could not read the file!");
			}
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Error: " + e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean delete(String id) {
		try {
			FileInfo fileDelete = fileDBRepository.findOne(id);
			Path file = root.resolve(fileDelete.getId());
			fileDBRepository.delete(fileDelete);
			return Files.deleteIfExists(file);
		} catch (IOException e) {
			throw new IllegalArgumentException("Error: " + e.getMessage());
		}
	}

	@Override
	public List<FileInfo> readAllFileInfo() {
		return fileDBRepository.findAll();
	}

	@Override
	public FileInfo readFileInfo(String id) {
		return fileDBRepository.findOne(id);
	}

	public String generateUuid(String fileName) {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		int hash = fileName.hashCode();
		return String.valueOf(Math.abs(hash)) + uuid;
	}

}