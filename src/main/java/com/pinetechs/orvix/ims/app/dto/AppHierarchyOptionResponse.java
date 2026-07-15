package com.pinetechs.orvix.ims.app.dto;

public class AppHierarchyOptionResponse {
    private final Long id;
    private final String code;
    private final String name;

    public AppHierarchyOptionResponse(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
