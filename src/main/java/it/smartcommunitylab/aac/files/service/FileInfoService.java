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
import it.smartcommunitylab.aac.dto.FileInfoDTO;
import it.smartcommunitylab.aac.files.persistence.FileInfoEntity;
import it.smartcommunitylab.aac.files.persistence.FileInfoRepository;
import java.util.ArrayList;

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

	public FileInfoEntity createFileInfo(String realm) {
		FileInfoEntity f = new FileInfoEntity();
		f.setId(generateUuid());
		f.setRealm(realm);
		return f;
	}

	public FileInfoEntity addFileInfo(String id, String realm, String fileName, String mimeType, long size)
			throws AlreadyRegisteredException {
		logger.debug("Add file for realm {}", String.valueOf(realm));
		FileInfoEntity exist = fileInfoRepository.findOne(id);
		if (exist != null) {
			throw new AlreadyRegisteredException();
		}
		// create file.
		if (id == null || id.isBlank() || id.isEmpty()) {
			id = generateUuid();
		}
		FileInfoEntity f = new FileInfoEntity();
		f.setId(id);
		f.setRealm(realm);
		f.setName(fileName);
		f.setMimeType(mimeType);
		f.setSize(size);
		f = fileInfoRepository.save(f);
		return f;
	}

	@Transactional(readOnly = true)
	public FileInfoEntity getFile(String id) throws NoSuchUserException {
		logger.debug("Read file id {}", String.valueOf(id));
		FileInfoEntity f = fileInfoRepository.findOne(id);
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
	public List<FileInfoDTO> listFiles(@NotNull String realm) {
		List<FileInfoDTO> listfilesDTO = new ArrayList<>();
		for (FileInfoEntity fileInfoEntity: fileInfoRepository.findByRealm(realm)) {
			listfilesDTO.add(getFileInfoDTO(fileInfoEntity));
		}		
		return listfilesDTO;
	}

	public List<FileInfoEntity> readAllFileInfo() {
		return fileInfoRepository.findAll();
	}

	public FileInfoDTO readFileInfo(String realm, String id) throws FileNotFoundException {
		FileInfoEntity file = fileInfoRepository.findByRealmAndId(realm, id);
		if (file != null) {
			return getFileInfoDTO(file);
		}
		throw new FileNotFoundException("file not found");
	}

	private String generateUuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	public FileInfoEntity updateFileInfo(String id, String realm, String fileName, String mimeType, Long size)
			throws NoSuchResourceException, FileNotFoundException {
		logger.debug("Update file id {}", String.valueOf(id));
		FileInfoEntity f = fileInfoRepository.findByRealmAndId(realm, id);;
		if (f == null) {
			throw new NoSuchResourceException("no file with id " + id);
		}
		f.setMimeType(mimeType);
		f.setName(fileName);
		f.setSize(size);
		f = fileInfoRepository.save(f);
		return f;
	}

	public void deleteFileInfo(String realm, String id) {
		logger.debug("Delete file id {} for realm {}", String.valueOf(id), String.valueOf(realm));
		FileInfoEntity blob = fileInfoRepository.findByRealmAndId(realm, id);
		if (blob != null) {
			fileInfoRepository.delete(blob);
		}
	}
	
	public FileInfoDTO getFileInfoDTO(FileInfoEntity fileInfoEntity) {
		FileInfoDTO fileInfoDTO = new FileInfoDTO();
		fileInfoDTO.setId(fileInfoEntity.getId());
		fileInfoDTO.setMimeType(fileInfoEntity.getMimeType());
		fileInfoDTO.setModifiedDate(fileInfoEntity.getModifiedDate());
		fileInfoDTO.setCreateDate(fileInfoEntity.getCreateDate());
		fileInfoDTO.setName(fileInfoEntity.getName());
		fileInfoDTO.setRealm(fileInfoEntity.getRealm());
		fileInfoDTO.setSize(fileInfoEntity.getSize());
		return fileInfoDTO;
		
	}
	
	public FileInfoEntity getFileInfoEntity(FileInfoDTO fileInfoDTO) {
		return fileInfoRepository.findByRealmAndId(fileInfoDTO.getRealm(), fileInfoDTO.getId());		
	}
	
}
