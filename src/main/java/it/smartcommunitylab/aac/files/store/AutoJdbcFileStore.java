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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.util.Assert;

public class AutoJdbcFileStore implements FileStore {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private static final String DEFAULT_SELECT_STATEMENT = "select id, file_id, data, realm, size from files where  file_id = ?";
	private static final String DEFAULT_INSERT_STATEMENT = "insert into files (id, file_id, data, realm, size) values (?, ?, ?, ?, ?)";
	private static final String DEFAULT_DELETE_STATEMENT = "delete from files where file_id = ?";

	private String selectFileSql = DEFAULT_SELECT_STATEMENT;
	private String insertFileSql = DEFAULT_INSERT_STATEMENT;
	private String deleteFileSql = DEFAULT_DELETE_STATEMENT;

	private final JdbcTemplate jdbcTemplate;
	private final RowMapper<Pair<String, Optional<Serializable>>> rowMapper = new FileRowMapper();

	public AutoJdbcFileStore(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource required");
		logger.debug("Jdbc File Store initialized");
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	private static class FileRowMapper implements RowMapper<Pair<String, Optional<Serializable>>> {
		@Override
		public Pair<String, Optional<Serializable>> mapRow(ResultSet rs, int rowNum) throws SQLException {
			String key = rs.getString("file_id");
			Serializable value = rs.getBytes("data");
			return Pair.of(key, Optional.ofNullable(value));
		}
	}

	public void addFileDB(String id, String fileInfoId, Serializable data) {
		jdbcTemplate.update(insertFileSql,
				new Object[] { id, fileInfoId, new SqlLobValue((byte[]) data) },
				new int[] { Types.VARCHAR, Types.VARCHAR, Types.BLOB });
	}

	public void deleteFileDB(String fileInfoId) {
		jdbcTemplate.update(deleteFileSql, fileInfoId);
	}

	public Map<String, Serializable> findFileDB(String fileInfoId) {
		List<Pair<String, Optional<Serializable>>> list = jdbcTemplate.query(selectFileSql, rowMapper, fileInfoId);
		return list.stream().filter(p -> p.getSecond().isPresent())
				.collect(Collectors.toMap(p -> p.getFirst(), p -> p.getSecond().get()));
	}

	@Override
	public void save(String id, String realm, InputStream str, long size) {
		try {
			jdbcTemplate.update(insertFileSql,
					new Object[] { UUID.randomUUID(), id, new SqlLobValue(str.readAllBytes()), realm, size },
					new int[] { Types.VARCHAR, Types.VARCHAR, Types.BLOB, Types.VARCHAR, Types.BIGINT });
		} catch (Exception e) {
			throw new IllegalArgumentException(e);						
		}
	}

	@Override
	public InputStream load(String id, String realm) {
		Map<String, Serializable> fileKeyValue = findFileDB(id);
		if (fileKeyValue == null || fileKeyValue.isEmpty()) {
			return null;
		}
		return new ByteArrayInputStream((byte[]) fileKeyValue.get(id));
	}

	@Override
	public boolean delete(String id, String realm) {
		deleteFileDB(id);
		return true;
	}

}
