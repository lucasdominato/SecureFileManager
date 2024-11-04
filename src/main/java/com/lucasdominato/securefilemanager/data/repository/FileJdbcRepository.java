package com.lucasdominato.securefilemanager.data.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@Repository
@Slf4j
public class FileJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public FileJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertFileContent(Long fileId, InputStream inputStream) {
        final String sql = "INSERT INTO file_content (file_id, content) VALUES (?, ?) ON CONFLICT (file_id) DO UPDATE SET content = EXCLUDED.content";

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setLong(1, fileId);
            statement.setBinaryStream(2, inputStream);
            return statement;
        });
    }

    public InputStream getFileContentStreamByFileId(Long fileId) {
        final String sql = "SELECT content FROM file_content WHERE file_id = ?";

        DataSource dataSource = Objects.requireNonNull(jdbcTemplate.getDataSource());

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setLong(1, fileId);
                preparedStatement.setFetchSize(1);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getBinaryStream("content");
                    } else {
                        throw new SQLException("File content not found for ID: " + fileId);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}