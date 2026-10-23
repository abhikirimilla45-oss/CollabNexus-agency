package com.collabnexus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:file:./target/test-data/collabnexus-test",
        "spring.jpa.hibernate.ddl-auto=update"
})
@ActiveProfiles("test")
class FileDatabaseConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void fileDatabaseUrlFromLocalProfileIsValid() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(2)).isTrue();
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        }
    }
}
