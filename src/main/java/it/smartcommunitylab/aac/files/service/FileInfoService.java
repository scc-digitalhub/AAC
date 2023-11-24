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
