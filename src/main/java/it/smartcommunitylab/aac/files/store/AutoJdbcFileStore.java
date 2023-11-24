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

package it.smartcommunitylab.aac.files.store;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.files.persistence.FileInfo;
import it.smartcommunitylab.aac.files.service.FileInfoService;

public class AutoJdbcFileStore implements FileStore {
	private static final String DEFAULT_SELECT_STATEMENT = "select id, fileinfo_id, data from files where  fileinfo_id = ?";
	private static final String DEFAULT_INSERT_STATEMENT = "insert into files (id, fileinfo_id, data) values (?, ?, ?, ?)";
	private static final String DEFAULT_DELETE_STATEMENT = "delete from files where fileinfo_id = ?";

	private String selectFileSql = DEFAULT_SELECT_STATEMENT;
	private String insertFileSql = DEFAULT_INSERT_STATEMENT;
	private String deleteFileSql = DEFAULT_DELETE_STATEMENT;

	@Autowired
	private FileInfoService fileInfoService;
	private final JdbcTemplate jdbcTemplate;
	private final RowMapper<Pair<String, Optional<Serializable>>> rowMapper = new FileRowMapper();

	public AutoJdbcFileStore(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource required");
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	private static class FileRowMapper implements RowMapper<Pair<String, Optional<Serializable>>> {
		@Override
		public Pair<String, Optional<Serializable>> mapRow(ResultSet rs, int rowNum) throws SQLException {
			String key = rs.getString("fileinfo_id");
			Serializable value = SerializationUtils.deserialize(rs.getBytes("data"));
			return Pair.of(key, Optional.ofNullable(value));
		}
	}

	public void addFile(String id, String fileInfoId, Serializable data) {
		jdbcTemplate.update(insertFileSql,
				new Object[] { id, fileInfoId, new SqlLobValue(SerializationUtils.serialize(data)) },
				new int[] { Types.VARCHAR, Types.VARCHAR, Types.BLOB });
	}

	public void deleteFile(String fileInfoId) {
		jdbcTemplate.update(deleteFileSql, fileInfoId);
	}

	public Map<String, Serializable> findFile(String fileInfoId) {
		List<Pair<String, Optional<Serializable>>> list = jdbcTemplate.query(selectFileSql, rowMapper, fileInfoId);

		return list.stream().filter(p -> p.getSecond().isPresent())
				.collect(Collectors.toMap(p -> p.getFirst(), p -> p.getSecond().get()));
	}

	@Override
	public void save(String filename, String contentType, InputStream file) {
		// TODO Auto-generated method stub

	}

	@Override
	public InputStream load(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean delete(String id) {
		FileInfo fileInfoObj = fileInfoService.readFileInfo(id);
		fileInfoService.delete(fileInfoObj);
		deleteFile(id);
		return true;
	}

}
