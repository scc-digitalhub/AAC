package it.smartcommunitylab.aac.files.service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

import it.smartcommunitylab.aac.files.persistence.FileDB;

public interface FilesStorageService {
	public void init();

	public void save(String filename, String contentType, InputStream file);

	public InputStream load(String filename);
	
	public FileDB readMetaData(String id);

	public boolean delete(String filename);

	public void deleteAll();

	public Stream<Path> loadAll();
}
