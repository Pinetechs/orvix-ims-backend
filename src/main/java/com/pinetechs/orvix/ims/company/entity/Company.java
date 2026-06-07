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
                @Index(name = "idx_companies_active", columnList = "is_active")
        }
)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank
    @Column(name = "name_ar", nullable = false, length = 200)
    private String nameAr;

    @Column(name = "name_en", length = 200)
    private String nameEn;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Company() {}

    public Company(Long id) { this.id = id; }

    @PrePersist
    public void prePersist() {
        if (active == null) active = true;
        if (code != null) code = code.trim().toUpperCase();
    }

    @PreUpdate
    public void preUpdate() {
        if (code != null) code = code.trim().toUpperCase();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
