package com.pinetechs.orvix.ims.common.lookup.dto;

public class LookupResponse {

    private String value;
    private String label;

    public LookupResponse() {
    }

    public LookupResponse(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static LookupResponse of(String value, String label) {
        return new LookupResponse(value, label);
    }

    public static LookupResponse of(Long value, String label) {
        return new LookupResponse(String.valueOf(value), label);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}