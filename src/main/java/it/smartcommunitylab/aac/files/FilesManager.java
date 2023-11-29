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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.smartcommunitylab.aac.files.persistence.FileInfo;
import it.smartcommunitylab.aac.files.service.FileInfoService;
import it.smartcommunitylab.aac.files.store.FileStore;
import it.smartcommunitylab.aac.realms.service.RealmService;

@Service
//@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')" + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')")
public class FilesManager {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	FileStore storageService;

	@Autowired
	FileInfoService fileInfoService;

	@Autowired
	private RealmService realmService;

	public void saveFileInfo(FileInfo fileInfo) {
		fileInfoService.save(fileInfo);
	}

	public FileInfo getFileInfo(String id) {
		return fileInfoService.readFileInfo(id);
	}

	public List<FileInfo> readFileInfo() {
		return fileInfoService.readAllFileInfo();
	}

	public void deleteFileInfo(FileInfo fileInfoObj) {
		fileInfoService.delete(fileInfoObj);
	}

	public void saveFile(@Valid MultipartFile file) throws IOException {
		FileInputStream isr = (FileInputStream) file.getInputStream();
		FileInfo fileInfo = new FileInfo(file.getOriginalFilename(), file.getContentType());
		String fileInfoId = fileInfoService.generateUuid(file.getOriginalFilename());
		fileInfo.setId(fileInfoId);
		storageService.save(fileInfoId, isr);
		saveFileInfo(fileInfo);
		isr.close();
	}

	public InputStream getFile(String id) {
		return storageService.load(id);
	}

	public boolean deleteFile(String id) {
		return storageService.delete(id);
	}

}
