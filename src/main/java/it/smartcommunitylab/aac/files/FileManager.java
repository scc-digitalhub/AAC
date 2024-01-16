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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.files.persistence.FileInfo;

@Service
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')" + " or hasAuthority(#realm+':" + Config.R_ADMIN + "')"
		+ " or hasAuthority(#realm+':" + Config.R_DEVELOPER + "')")
public class FileManager {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private FileService fileService;

	public FileInfo saveFile(String realm, String originalFilename, String contentType, long size, InputStream inputStream)
			throws AlreadyRegisteredException, IOException {
		return (fileService.createFile(realm, originalFilename, contentType, size, inputStream));
	}

	public List<FileInfo> readFilesByRealm(String realm) {
		return fileService.getFilesByRealm(realm);
	}

	public InputStream readFileBlob(String realm, String id) {
		return fileService.getFileStream(realm, id);
	}

	public FileInfo readFile(String realm, String id) throws FileNotFoundException {
		return fileService.getFile(realm, id);
	}

	public void deleteFile(String realm, String id) throws FileNotFoundException {
		fileService.deleteFile(realm, id);
	}

}
