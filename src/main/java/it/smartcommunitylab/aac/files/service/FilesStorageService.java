package it.smartcommunitylab.aac.files.service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface FilesStorageService {
	public void init();

	public void save(String filename, InputStream file);

	public InputStream load(String filename);

	public boolean delete(String filename);

	public void deleteAll();

	public Stream<Path> loadAll();
}
