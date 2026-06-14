package com.pinetechs.orvix.ims.company.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "companies",
        indexes = {
                @Index(name = "idx_companies_code", columnList = "code"),
                @Index(name = "idx_companies_name", columnList = "name"),
                @Index(name = "idx_companies_active", columnList = "is_active")
        }
)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Company code is required")
    @Column(
            name = "code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String code;

    @NotBlank(message = "Company name is required")
    @Column(
            name = "name",
            nullable = false,
            length = 200
    )
    private String name;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean active = true;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Company() {
    }

    public Company(Long id) {
        this.id = id;
    }

    public Company(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @PrePersist
    public void prePersist() {

        if (active == null) {
            active = true;
        }

        normalizeFields();
    }

    @PreUpdate
    public void preUpdate() {
        normalizeFields();
    }

    private void normalizeFields() {

        if (code != null) {
            code = code.trim().toUpperCase();
        }

        if (name != null) {
            name = name.trim();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {

        this.code = code == null
                ? null
                : code.trim().toUpperCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {

        this.name = name == null
                ? null
                : name.trim();
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActiveCompany() {
        return Boolean.TRUE.equals(active);
    }

    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", active=" + active +
                '}';
    }
}