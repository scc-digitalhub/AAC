package it.smartcommunitylab.aac.files.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

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
import it.smartcommunitylab.aac.files.model.FileInfo;
import it.smartcommunitylab.aac.files.persistence.FileDB;
import it.smartcommunitylab.aac.files.service.FilesStorageService;

@Controller
@RequestMapping("/files")
public class FilesController {

	@Autowired
	FilesStorageService storageService;

	@PostMapping(path = "/upload")
	public ResponseEntity<ResponseMessage> uploadFile(
			@RequestPart(name = "file", required = true) @Valid MultipartFile file) {
		String message = "";
		try {
			storageService.save(file.getOriginalFilename(), file.getContentType(), file.getInputStream());
			message = "Uploaded the file successfully: " + file.getOriginalFilename();
			return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
		} catch (Exception e) {
			message = "Could not upload the file: " + file.getOriginalFilename() + ". Error: " + e.getMessage();
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
		}
	}

	@GetMapping("/list")
	public ResponseEntity<List<FileInfo>> getListFiles() {
		List<FileInfo> fileInfos = storageService.loadAll().map(path -> {
			String filename = path.getFileName().toString();
			return new FileInfo(filename, null);
		}).collect(Collectors.toList());

		return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
	}

	@GetMapping("/{id:.+}")
	public void getFile(@PathVariable String id, HttpServletResponse res) throws IOException {
		FileDB fileDB = storageService.readMetaData(id);
		InputStream is = storageService.load(id);
		res.setContentType(fileDB.getType());
		res.setHeader("Content-Disposition", "attachment;filename=" + fileDB.getName());
		ServletOutputStream out = res.getOutputStream();
		out.write(is.readAllBytes());
		out.flush();
		out.close();
	}

	@DeleteMapping("/{id:.+}")
	public ResponseEntity<ResponseMessage> deleteFile(@PathVariable String id) {
		String message = "";
		try {
			boolean deleted = storageService.delete(id);
			if (deleted) {
				message = "Deleted successfully: " + id;
				return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
			}
			message = "The file does not exist!";
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseMessage(message));
		} catch (Exception e) {
			message = "Could not delete the file: " + id + ". Error: " + e.getMessage();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseMessage(message));
		}
	}
}