package com.utam.turnaround.domain;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "turnaround_alerts")
public class Alert {
    @Id
    private UUID id;

    @Column(name = "tenant_code", nullable = false)
    private String tenantCode;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private TurnaroundSession session;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String message;

    @Column(name = "timestamp")
    private ZonedDateTime timestamp;

    @Column(name = "is_active")
    private Boolean isActive;

    public Alert() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }

    public TurnaroundSession getSession() { return session; }
    public void setSession(TurnaroundSession session) { this.session = session; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
