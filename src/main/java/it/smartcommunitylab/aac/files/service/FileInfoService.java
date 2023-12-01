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
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.files.persistence.FileInfo;
import it.smartcommunitylab.aac.files.persistence.FileInfoRepository;

@Service
@Transactional
public class FileInfoService {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private FileInfoRepository fileInfoRepository;

	public FileInfoService(FileInfoRepository fileInfoRepository) {
		Assert.notNull(fileInfoRepository, "fileinfo repository is mandatory");
		this.fileInfoRepository = fileInfoRepository;
		logger.debug("fileinfo service initialized");
	}

	public FileInfo createFileInfo(String realm) {
		return new FileInfo(generateUuid(), realm);
	}

	public FileInfo addFileInfo(String id, String realm, String fileName, String mimeType, long size)
			throws AlreadyRegisteredException {
		logger.debug("Add file for realm {}", String.valueOf(realm));
		FileInfo exist = fileInfoRepository.findOne(id);
		if (exist != null) {
			throw new AlreadyRegisteredException();
		}
		// create file.
		if (id == null || id.isBlank() || id.isEmpty()) {
			id = generateUuid();
		}
		FileInfo f = new FileInfo(id, realm);
		f.setName(fileName);
		f.setMimeType(mimeType);
		f.setSize(size);
		f = fileInfoRepository.save(f);
		return f;
	}

	@Transactional(readOnly = true)
	public FileInfo getFile(String id) throws NoSuchUserException {
		logger.debug("Read file id {}", String.valueOf(id));
		FileInfo f = fileInfoRepository.findOne(id);
		if (f == null) {
			throw new NoSuchUserException("no file with id " + id);
		}
		return f;
	}

	@Transactional(readOnly = true)
	public long countFiles(@NotNull String realm) {
		return fileInfoRepository.countByRealm(realm);
	}

	@Transactional(readOnly = true)
	public List<FileInfo> listFiles(@NotNull String realm) {
		return fileInfoRepository.findByRealm(realm);
	}

	public List<FileInfo> readAllFileInfo() {
		return fileInfoRepository.findAll();
	}

	public FileInfo readFileInfo(String realm, String id) throws FileNotFoundException {
		FileInfo file = fileInfoRepository.findByRealmAndId(realm, id);
		if (file != null) {
			return file;
		}
		throw new FileNotFoundException("file not found");
	}

	private String generateUuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	public FileInfo updateFileInfo(String id, String realm, String fileName, String mimeType, Long size)
			throws NoSuchResourceException, FileNotFoundException {
		logger.debug("Update file id {}", String.valueOf(id));
		FileInfo f = readFileInfo(realm, id);
		if (f == null) {
			throw new NoSuchResourceException("no file with id " + id);
		}
		f.setMimeType(mimeType);
		f.setName(fileName);
		f.setSize(size);
		f = fileInfoRepository.save(f);
		return f;
	}

	public boolean deleteFileInfo(String realm, String id) {
		logger.debug("Delete file id {} for realm {}", String.valueOf(id), String.valueOf(realm));
		FileInfo blob = fileInfoRepository.findByRealmAndId(realm, id);
		if (blob != null) {
			fileInfoRepository.delete(blob);
			return true;
		}
		return false;
	}

}
