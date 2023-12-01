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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.files.persistence.FileInfoRepository;
import it.smartcommunitylab.aac.files.persistence.FileInfo;

@Service
public class FileInfoService {
	@Autowired
	private FileInfoRepository fileInfoRepository;
	
	/**
	 * 
	 * 5. Generate UUID 
 * 		Its a business of FileInfoService. **
 * 6. CRUD Semantic
 *      create file info pass here
 *      business of fileinfo service, here need actual logic realm again as parameter, check if it exist
 *      if a call is made for create for object which already there it must throw error, write logic before saving
 *      save, update
 *      before saving check if it exist throw exception.
 *      if exist update 
 *      all the logic to manage FileInfo must be inside this service.   
 *      delete takes in put just realm and id if it not null delete else cannot be found. all the checks
 *      define in repository ListByRealm ** check UserEntityService for check.JPA
 *      
 */
	public List<FileInfo> readAllFileInfo() {
		return fileInfoRepository.findAll();
	}
	
	public List<FileInfo> listByRealm(String realm) {
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

	public FileInfo addFileInfo(String realm, String id, FileInfo fileInfo) {
		if (id != null && !id.isBlank() && !id.isEmpty()) {
			fileInfo.setId(id);
		} else {
			fileInfo.setId(generateUuid());	
		}		
		fileInfo.setRealm(realm);
		return fileInfoRepository.save(fileInfo);
	}
	
	public FileInfo updateFileInfo(FileInfo fileInfo) {
		// if exist.
		
		return fileInfoRepository.save(fileInfo);
	}

	public boolean deleteFileInfo(String realm, String id) {
		FileInfo blob = fileInfoRepository.findByRealmAndId(realm, id);
		if (blob != null) {
			fileInfoRepository.delete(blob);
			return true;
		}
		return false;				
	}

}
