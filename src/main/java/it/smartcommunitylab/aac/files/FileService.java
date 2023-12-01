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
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.files.persistence.FileInfo;
import it.smartcommunitylab.aac.files.service.FileInfoService;
import it.smartcommunitylab.aac.files.store.FileStore;

@Service
public class FileService {
	/**
	 * 1. realm field in every method call.(both for FileInfo and Store)
	 *    store needs to keep track of realm.
	 *    filestore handles io id is the fileinfo_id. Also mimeType and filesize(we want to check what we write and readback is the samething).
	 *    filestore will save file under path for every realm
	 *    long size. In JDBC we dont care but it will be duplicated but only saved in JDBC not in local file case.
	 *    fileInfo_id is file id. (file meta id)
	 *    CHANGE the JDBC queries.
	 *    filehash of the content. * (TODO)
	 * 2. ID of FileInfo
	 * 	   - hash is on actual bytes no on the name. Just keep it UUID.
	 * 3. Take realm as input from API /api/realm/file. (TODO)
	 * 4. Save
	 * 	  if we save fileINfo and file obj fails it fine. For delete , first delete blob and then fileInfo.
	 * logging in here, you have the check if you get the fields
	 * 5. you see the copy the kind of logic in other service. if you get the field getOneRealm Id through illegaargument if it does not belong MultiTenancy logic.
	 * 6. Transactional annotation is required.
	 * 7. EntityService, UserEnttity
	 * 8. Add logger.
	 * 9. Manager TODO that will wrap file service and just call the manager instead of service here.
	 * 10. you did the basic stuff now we will complement it.
	 */
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private FileStore fileStore;

	@Autowired
	private FileInfoService fileInfoService;

	public void saveFileInfo(String realm, FileInfo fileInfo) {
		fileInfoService.addFileInfo(realm, fileInfo.getId(), fileInfo);
	}

	public FileInfo getFileInfo(String realm, String id) throws FileNotFoundException {
		return fileInfoService.readFileInfo(realm, id);
	}

	public List<FileInfo> readFileInfo(String realm) {
		return fileInfoService.readAllFileInfo();
	}

	public void deleteFileInfo(String realm, FileInfo fileInfoObj) {
//		fileInfoService.delete(fileInfoObj);
	}

	/**
	 * 
	 * @param realm
	 * @param fileName
	 * @param mimeType
	 * @param size
	 * @param isr
	 * @throws IOException
	 */
	public void addFile(String realm, String fileName, String mimeType, long size, InputStream isr)
			throws IOException {
		FileInfo fileInfo = new FileInfo();
		fileInfo.setName(fileName);
		fileInfo.setMimeType(mimeType);
		fileInfo.setSize(size);
		fileInfo = fileInfoService.addFileInfo(realm, null, fileInfo);
		fileStore.save(fileInfo.getId(), realm, isr, size);
		isr.close();
	}

	public boolean deleteFile(String realm, String id) throws FileNotFoundException {
		FileInfo fileInfo = fileInfoService.readFileInfo(realm, id);
		if (fileInfo != null) {
			fileStore.delete(fileInfo.getId(), realm);
			return fileInfoService.deleteFileInfo(realm, id);
		}
		throw new FileNotFoundException("file not found");
		
	}
	
	public InputStream readFileBlob(String realm, String id) throws FileNotFoundException {
		return fileStore.load(id, realm);
	}

}
