package it.smartcommunitylab.aac.files.service;

import java.io.InputStream;
import java.util.List;

import it.smartcommunitylab.aac.files.persistence.FileInfo;
import it.smartcommunitylab.aac.files.store.FileStoreService;

public class JDBCFileStoreService implements FileStoreService {

	@Override
	public void save(String filename, String contentType, InputStream file) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InputStream load(String filename) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(String filename) {
		// TODO Auto-generated method stub
		return false;
	}

	public FileInfo readFileInfo(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<FileInfo> readAllFileInfo() {
		// TODO Auto-generated method stub
		return null;
	}

}
