package com.lucasdominato.securefilemanager.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.BinaryJdbcType;

public class CustomPostgreSQLDialect extends PostgreSQLDialect {

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);

        typeContributions.getTypeConfiguration()
                .getJdbcTypeRegistry()
                .addDescriptor(SqlTypes.BLOB, BinaryJdbcType.INSTANCE);
    }
}