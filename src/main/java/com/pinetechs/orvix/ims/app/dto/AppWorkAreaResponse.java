package com.pinetechs.orvix.ims.app.dto;

public class AppWorkAreaResponse {

    private Long id;
    private String type;
    private String code;
    private String name;
    private Integer totalRecords;
    private Integer processedRecords;
    private Integer matchedRecords;
    private Integer progress;

    public AppWorkAreaResponse(Long id, String type, String code, String name,
                               Integer totalRecords, Integer processedRecords,
                               Integer matchedRecords, double progress) {
        this.id = id;
        this.type = type;
        this.code = code;
        this.name = name;
        this.totalRecords = valueOrZero(totalRecords);
        this.processedRecords = valueOrZero(processedRecords);
        this.matchedRecords = valueOrZero(matchedRecords);
        this.progress = (int) Math.round(progress);
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getTotalRecords() { return totalRecords; }
    public Integer getProcessedRecords() { return processedRecords; }
    public Integer getMatchedRecords() { return matchedRecords; }
    public Integer getProgress() { return progress; }
}
