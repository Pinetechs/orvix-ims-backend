package com.pinetechs.orvix.ims.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

@Component
public class Config {

    private static final Logger LOGGER = Logger.getLogger(Config.class.getName());
    private static final String CONFIG_FILE_NAME = "../conf/OrvixConf.xml";

    private final Map<String, String> properties = new HashMap<>();

    @PostConstruct
    public void start() {
        checkConfig();
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(Property property) {
        String propertyValue = properties.get(property.getName());
        if (propertyValue == null) {
            propertyValue = property.getDefaultValue();
        }

        Class<?> returnType = property.getReturnType();
        if (returnType.equals(String.class)) {
            return (T) propertyValue;
        }
        if (returnType.equals(Boolean.class)) {
            return (T) Boolean.valueOf(Boolean.parseBoolean(propertyValue));
        }
        if (returnType.equals(Integer.class)) {
            return (T) Integer.valueOf(Integer.parseInt(propertyValue));
        }
        if (returnType.equals(Long.class)) {
            return (T) Long.valueOf(Long.parseLong(propertyValue));
        }
        return null;
    }

    public String getString(Property property) {
        return getProperty(property);
    }

    public Boolean getBoolean(Property property) {
        return getProperty(property);
    }

    public Integer getInteger(Property property) {
        return getProperty(property);
    }

    public Long getLong(Property property) {
        return getProperty(property);
    }

    private void checkConfig() {
        List<Property> missingProperties = new ArrayList<>();
        File configFile = new File(CONFIG_FILE_NAME);

        if (!configFile.exists()) {
            createDefaultConfigFile(configFile);
        }

        LOGGER.info("********** Initialize Orvix IMS properties from XML **********");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (Property property : Property.values()) {
                    if (line.toLowerCase().contains(("key=\"" + property.getName().toLowerCase() + "\"").trim())) {
                        properties.put(property.getName(), getValueFromLine(line));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Unable to read Orvix config file: " + e.getMessage());
        }

        for (Property property : Property.values()) {
            if (!properties.containsKey(property.getName())) {
                missingProperties.add(property);
                properties.put(property.getName(), property.getDefaultValue());
            }
        }

        for (Property property : missingProperties) {
            writeConfig(property.getName(), property.getDefaultValue());
        }

        for (Property property : Property.values()) {
            LOGGER.info(property.getName() + " = " + maskSensitiveValue(property, properties.get(property.getName())));
        }

        LOGGER.info("*************************************************************");
    }

    private void writeConfig(String propertyName, String propertyValue) {
        try {
            File configFile = new File(CONFIG_FILE_NAME);
            if (!configFile.exists()) {
                createDefaultConfigFile(configFile);
            }

            String xmlContent = readFile(configFile);
            if (!xmlContent.contains("key=\"" + propertyName + "\"")) {
                xmlContent = xmlContent.replace("</properties>", "\t<entry key=\"" + propertyName + "\">" + escapeXml(propertyValue) + "</entry>\n</properties>");
                writeFile(configFile, xmlContent);
            }
        } catch (IOException e) {
            LOGGER.warning("Unable to update Orvix config file: " + e.getMessage());
        }
    }

    private String getValueFromLine(String line) {
        int start = line.indexOf('>') + 1;
        int end = line.lastIndexOf("</");
        if (start <= 0 || end <= start) {
            return "";
        }
        return unescapeXml(line.substring(start, end).trim());
    }

    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private void writeFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(content);
        }
    }

    private void createDefaultConfigFile(File configFile) {
        try {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                writer.write("<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n");
                writer.write("<properties>\n");
                writer.write("\t<comment>Orvix IMS Configuration File</comment>\n");
                writer.write("</properties>\n");
            }
        } catch (IOException e) {
            LOGGER.warning("Unable to create Orvix config file: " + e.getMessage());
        }
    }

    private String maskSensitiveValue(Property property, String value) {
        if (property == Property.DB_PASSWORD || property == Property.MAIL_PASSWORD || property == Property.JWT_SECRET) {
            return "********";
        }
        return value;
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String unescapeXml(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}
