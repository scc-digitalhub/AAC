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

package it.smartcommunitylab.aac.files.controller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import it.smartcommunitylab.aac.files.message.ResponseMessage;
import it.smartcommunitylab.aac.files.persistence.FileInfo;
import it.smartcommunitylab.aac.files.service.FileInfoService;
import it.smartcommunitylab.aac.files.store.FileStore;

@Controller
@RequestMapping("/files")
public class FilesController {

	@Autowired
	FileStore storageService;

	@Autowired
	FileInfoService fileInfoService;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@PostMapping(path = "/upload")
	public ResponseEntity<ResponseMessage> uploadFile(
			@RequestPart(name = "file", required = true) @Valid MultipartFile file) {
		String message = "";
		try {
			FileInputStream isr = (FileInputStream) file.getInputStream();
			FileInfo fileInfo = new FileInfo(file.getOriginalFilename(), file.getContentType());
			String fileInfoId = fileInfoService.generateUuid(file.getOriginalFilename());
			fileInfo.setId(fileInfoId);
			storageService.save(fileInfoId, isr);
			fileInfoService.save(fileInfo);			
			message = "File uploaded successfully: " + file.getOriginalFilename();
			logger.debug(message);
			isr.close();
			return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
		} catch (Exception e) {
			message = "Could not upload the file: " + file.getOriginalFilename() + ". Error: " + e.getMessage();
			logger.debug(message);
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
		}
	}

	@GetMapping("/list")
	public ResponseEntity<List<FileInfo>> getListFiles() {
		List<FileInfo> fileInfos = fileInfoService.readAllFileInfo();
		return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
	}

	@GetMapping("/{id:.+}")
	public void getFile(@PathVariable String id, HttpServletResponse res) throws IOException {
		InputStream is = storageService.load(id);
		if (is != null) {
			FileInfo fileInfo = fileInfoService.readFileInfo(id);
			res.setContentType(fileInfo.getType());
			res.setHeader("Content-Disposition", "attachment;filename=" + fileInfo.getName());
			ServletOutputStream out = res.getOutputStream();
			out.write(is.readAllBytes());
			out.flush();
			out.close();
			is.close();
		} else {
			throw new FileNotFoundException("Could not read the file!");
		}
	}

	@DeleteMapping("/{id:.+}")
	public ResponseEntity<ResponseMessage> deleteFile(@PathVariable String id) {
		String message = "";
		try {
			boolean deleted = storageService.delete(id);
			if (deleted) {
				FileInfo fileInfoObj = fileInfoService.readFileInfo(id);
				fileInfoService.delete(fileInfoObj);
				message = "Deleted successfully: " + id;
				logger.debug(message);
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
			}
			message = "The file does not exist!";
			logger.debug(message);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseMessage(message));
		} catch (Exception e) {
			message = "Could not delete the file: " + id + ". Error: " + e.getMessage();
			logger.debug(message);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseMessage(message));
		}
	}
}
