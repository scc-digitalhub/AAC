package it.smartcommunitylab.aac.files.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import it.smartcommunitylab.aac.files.persistence.FileDB;
import it.smartcommunitylab.aac.files.persistence.FileDBRepository;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {

	private final Path root = Paths.get("uploads");
	@Autowired
	private FileDBRepository fileDBRepository;

	@Override
	@PostConstruct
	public void init() {
		try {
			Files.createDirectories(root);
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize folder for upload!");
		}
	}

	@Override
	public void save(String fileName, String contentType, InputStream str) { // filename, inputstream
		try {
			FileDB fileDB = new FileDB(fileName, contentType);
			fileDB.setId(generateUuid(fileName));
			fileDB = fileDBRepository.save(fileDB);
			Files.copy(str, this.root.resolve(fileDB.getId()));
			str.close();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public InputStream load(String id) {
		try {
			FileDB fileDB = fileDBRepository.findOne(id);
			Path file = root.resolve(fileDB.getId());
			Resource resource = new UrlResource(file.toUri());

			if (resource.exists() || resource.isReadable()) {
				return resource.getInputStream();
			} else {
				throw new RuntimeException("Could not read the file!");
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error: " + e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean delete(String id) {
		try {
			FileDB fileDelete = fileDBRepository.findOne(id);
			Path file = root.resolve(fileDelete.getId());
			fileDBRepository.delete(fileDelete);
			return Files.deleteIfExists(file);
		} catch (IOException e) {
			throw new RuntimeException("Error: " + e.getMessage());
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(root.toFile());
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
		} catch (IOException e) {
			throw new RuntimeException("Could not load the files!");
		}
	}

	public String generateUuid(String fileName) {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		int hash = fileName.hashCode();
		return String.valueOf(hash) + uuid;
	}

	@Override
	public FileDB readMetaData(String id) {
		return fileDBRepository.findOne(id);
	}

}