package com.pinetechs.orvix.ims.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class ApplicationConfig {

    private final Config config;

    public ApplicationConfig(Config config) {
        this.config = config;
    }

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(config.getProperty(Property.DB_URL))
                .username(config.getProperty(Property.DB_USERNAME))
                .password(config.getProperty(Property.DB_PASSWORD))
                .driverClassName(config.getProperty(Property.DB_DRIVER))
                .build();
    }

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.setPort(config.getProperty(Property.SERVER_PORT));
        return factory;
    }

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(config.getProperty(Property.MAIL_HOST));
        mailSender.setPort(config.getProperty(Property.MAIL_PORT));
        mailSender.setUsername(config.getProperty(Property.MAIL_USERNAME));
        mailSender.setPassword(config.getProperty(Property.MAIL_PASSWORD));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", config.getProperty(Property.MAIL_PROTOCOL));
        props.put("mail.smtp.auth", config.getProperty(Property.MAIL_SMTP_AUTH));
        props.put("mail.smtp.starttls.enable", config.getProperty(Property.MAIL_SMTP_STARTTLS_ENABLE));
        props.put("mail.smtp.starttls.required", config.getProperty(Property.MAIL_SMTP_STARTTLS_REQUIRED));
        props.put("mail.debug", config.getProperty(Property.MAIL_DEBUG));
        props.put("mail.smtp.ssl.trust", config.getProperty(Property.MAIL_SMTP_SSL_TRUST));
        props.put("mail.smtp.ssl.enable", config.getProperty(Property.MAIL_SMTP_SSL_ENABLE));

        return mailSender;
    }
}
