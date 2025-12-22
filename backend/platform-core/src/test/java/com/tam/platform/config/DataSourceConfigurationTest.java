package com.tam.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PlatformServicesAutoConfiguration.class)
@EnableAutoConfiguration
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
public class DataSourceConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    @Test
    void verifySingleSharedDataSourceConfiguration() {
        // Verify that a DataSource bean exists
        assertThat(dataSource).isNotNull();

        // Verify that it is NOT an AbstractRoutingDataSource
        // This confirms we are using a single shared datasource as per T032
        assertThat(dataSource).isNotInstanceOf(AbstractRoutingDataSource.class);

        // Verify that we don't have multiple datasources configured
        String[] dataSourceNames = context.getBeanNamesForType(DataSource.class);
        assertThat(dataSourceNames).hasSize(1);
    }
}
