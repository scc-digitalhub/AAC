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

	public List<FileInfo> readAllFileInfo() {
		return fileInfoRepository.findAll();
	}

	public FileInfo readFileInfo(String id) {
		return fileInfoRepository.findOne(id);
	}

	public String generateUuid(String fileName) {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		int hash = fileName.hashCode();
		return String.valueOf(Math.abs(hash)) + uuid;
	}

	public FileInfo save(FileInfo fileInfo) {
		return fileInfoRepository.save(fileInfo);
	}

	public void delete(FileInfo fileDelete) {
		fileInfoRepository.delete(fileDelete);		
	}

}
