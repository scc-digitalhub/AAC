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
import it.smartcommunitylab.aac.files.persistence.FileInfo;
import it.smartcommunitylab.aac.files.service.FileInfoService;
import it.smartcommunitylab.aac.files.store.FileStore;

@Service
public class FileService {
	/**
	 *    filehash of the content. * (TODO)
	 *	 * 	   - hash is on actual bytes no on the name. Just keep it UUID.
	 * 7. EntityService, UserEnttity
	 * 9. Manager TODO that will wrap file service and just call the manager instead of service here.
	 */
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private FileStore fileStore;

	@Autowired
	private FileInfoService fileInfoService;

	/**
	 * Read single file inside realm.
	 * @param realm
	 * @param id
	 * @return
	 * @throws FileNotFoundException
	 */
	public FileInfo readFile(String realm, String id) throws FileNotFoundException {
		return fileInfoService.readFileInfo(realm, id);
	}

	/**
	 * ReadFiles for realm
	 * @param realm
	 * @return
	 */
	public List<FileInfo> readFilesByRealm(String realm) {
		return fileInfoService.listFiles(realm);
	}

	/**
	 * Save File.
	 * @param realm
	 * @param fileName
	 * @param mimeType
	 * @param size
	 * @param isr
	 * @throws IOException
	 * @throws AlreadyRegisteredException
	 */
	public void saveFile(String realm, String fileName, String mimeType, long size, InputStream isr)
			throws IOException, AlreadyRegisteredException {
		// generate a new file, always persisted
		FileInfo f = fileInfoService.createFileInfo(realm);
		String fileId = f.getId();
		try {
			FileInfo file = fileInfoService.addFileInfo(fileId, realm, fileName, mimeType, size);
			fileStore.save(file.getId(), realm, isr, size);
			isr.close();
		} catch (AlreadyRegisteredException e) {
			// something wrong, stop
			logger.error("error creating new userfor subject {}", String.valueOf(fileId));
			throw new AuthenticationServiceException("error processing request");
		}
	}

	/**
	 * Delete File.
	 * @param realm
	 * @param id
	 * @return
	 * @throws FileNotFoundException
	 */
	public boolean deleteFile(String realm, String id) throws FileNotFoundException {
		FileInfo fileInfo = fileInfoService.readFileInfo(realm, id);
		if (fileInfo != null) {
			logger.debug("Delete file blob for realm {} and id ", String.valueOf(realm), String.valueOf(id));
			fileStore.delete(fileInfo.getId(), realm);
			return fileInfoService.deleteFileInfo(realm, id);
		}
		throw new FileNotFoundException("file not found");
		
	}
	
	/**
	 * Read FileBlob.
	 * @param realm
	 * @param id
	 * @return
	 */
	public InputStream readFileBlob(String realm, String id) {
		logger.debug("Read file blob for realm {} and id ", String.valueOf(realm), String.valueOf(id));
		return fileStore.load(id, realm);
	}

}
