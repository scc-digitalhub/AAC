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

package it.smartcommunitylab.aac.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.dto.FileInfoDTO;
import it.smartcommunitylab.aac.files.persistence.FileInfoEntity;
import it.smartcommunitylab.aac.files.service.FileInfoService;
import it.smartcommunitylab.aac.files.store.FileStore;

@Service
public class FileService {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private FileStore fileStore;

	@Autowired
	private FileInfoService fileInfoService;

	/**
	 * Create File.
	 * 
	 * @param realm
	 * @param fileName
	 * @param mimeType
	 * @param size
	 * @param isr
	 * @return 
	 * @throws IOException
	 * @throws AlreadyRegisteredException
	 */
	public FileInfoDTO createFile(String realm, String fileName, String mimeType, long size, InputStream isr)
			throws IOException, AlreadyRegisteredException {
		// generate a new file, always persisted
		FileInfoEntity f = fileInfoService.createFileInfo(realm);
		String fileId = f.getId();
		try {
			FileInfoEntity file = fileInfoService.addFileInfo(fileId, realm, fileName, mimeType, size);
			fileStore.save(file.getId(), realm, isr, size);
			isr.close();
			return fileInfoService.getFileInfoDTO(file);
		} catch (AlreadyRegisteredException e) {
			// something wrong, stop
			logger.error("error creating new userfor subject {}", String.valueOf(fileId));
			throw e;
		}
	}

	/**
	 * Get single file inside realm.
	 * 
	 * @param realm
	 * @param id
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileInfoDTO getFile(String realm, String id) throws FileNotFoundException {
		return fileInfoService.readFileInfo(realm, id);
	}

	/**
	 * Get files for realm
	 * 
	 * @param realm
	 * @return
	 */
	public List<FileInfoDTO> getFilesByRealm(String realm) {
		return fileInfoService.listFiles(realm);
	}

	/**
	 * Get file stream (FileBlob).
	 * 
	 * @param realm
	 * @param id
	 * @return
	 */
	public InputStream getFileStream(String realm, String id) {
		logger.debug("Read file blob for realm {} and id {} ", String.valueOf(realm), String.valueOf(id));
		return fileStore.load(id, realm);
	}

	/**
	 * Delete File.
	 * 
	 * @param realm
	 * @param id
	 * @return
	 * @throws FileNotFoundException
	 */
	public void deleteFile(String realm, String id) throws FileNotFoundException {
		FileInfoDTO fileInfo = fileInfoService.readFileInfo(realm, id);
		if (fileInfo != null) {
			logger.debug("Delete file blob for realm {} and id {}", String.valueOf(realm), String.valueOf(id));
			fileStore.delete(fileInfo.getId(), realm);
			fileInfoService.deleteFileInfo(realm, id);
		} else {
			throw new FileNotFoundException("file not found");	
		}		
	}
		
}
