package com.utam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "tenants")
@Data
public class Tenant {
    @Id
    @Column(length = 4)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "varchar(50) default 'UTC'")
    private String timezone = "UTC";

    @Column(columnDefinition = "jsonb")
    private String config;

    @Column(name = "created_at")
    private Instant createdAt;
}
