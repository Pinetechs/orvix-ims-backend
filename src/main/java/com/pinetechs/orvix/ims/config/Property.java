package com.pinetechs.orvix.ims.config;

public enum Property {

    COOKIE_DOMAIN("cookieDomain", "localhost", String.class),
    COOKIE_SECURE("cookieSecure", "false", Boolean.class),
    COOKIE_SAME_SITE("cookieSameSite", "Lax", String.class),
    COOKIE_MAX_AGE_SECONDS("cookieMaxAgeSeconds", "86400", Long.class),

    SERVER_PORT("ServerPort", "8080", Integer.class),

    DB_USERNAME("db_Username", "root", String.class),
    DB_PASSWORD("db_Password", "root", String.class),
    DB_URL("db_URL", "jdbc:mysql://75.119.138.236:3306/orvix_ims_db?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSqlLimit=2048", String.class),
    DB_DRIVER("db_Driver", "com.mysql.cj.jdbc.Driver", String.class),
    SHOW_SQL("showSql", "false", Boolean.class),

    JWT_SECRET("jwtSecret", "change-this-secret-before-production", String.class),
    JWT_TOKEN_VALIDITY_MS("jwtTokenValidityMs", "86400000", Long.class),

    FILE_UPLOAD_DIR("fileUploadDIR", "upload/", String.class),
    ERP_IMPORT_UPLOAD_DIR("erpImportUploadDir", "erp-imports/", String.class),
    REPORT_EXPORT_DIR("reportExportDir", "reports/", String.class),
    MAX_FILE_SIZE_MB("maxFileSizeMB", "50", Integer.class),
    PUBLIC_RESOURCE_PATH("publicResourcePath", "/public/**", String.class),
    FRONTEND_INDEX_FILE("frontendIndexFile", "index.html", String.class),

    MAIL_HOST("mail_Host", "mail.pinetechs.com", String.class),
    MAIL_PORT("mail_Port", "465", Integer.class),
    MAIL_USERNAME("mail_username", "", String.class),
    MAIL_PASSWORD("mail_password", "", String.class),
    MAIL_PROTOCOL("mail_protocol", "smtp", String.class),
    MAIL_SMTP_AUTH("mail_smtp_auth", "true", String.class),
    MAIL_SMTP_STARTTLS_ENABLE("mail_smtp_starttls_enable", "true", String.class),
    MAIL_SMTP_STARTTLS_REQUIRED("mail_smtp_starttls_required", "false", String.class),
    MAIL_SMTP_SSL_TRUST("mail_smtp_ssl_trust", "mail.pinetechs.com", String.class),
    MAIL_SMTP_SSL_ENABLE("mail_smtp_ssl_enable", "true", String.class),
    BACKGROUND_JOB_ENABLED("background_job_enabled", "true", Boolean.class),
    BACKGROUND_BATCH_SIZE("background_batch_size", "20", Integer.class),
    MAIL_DEBUG("mail_debug", "false", String.class);

    private final String name;
    private final String defaultValue;
    private final Class<?> returnType;

    Property(String name, String defaultValue, Class<?> returnType) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Class<?> getReturnType() {
        return returnType;
    }
}
