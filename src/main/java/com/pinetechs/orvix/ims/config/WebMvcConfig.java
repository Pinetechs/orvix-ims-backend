package com.pinetechs.orvix.ims.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Config config;

    public WebMvcConfig(Config config) {
        this.config = config;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicDir = getPublicDirectory();

        registry.addResourceHandler("/public/**")
                .addResourceLocations("file:" + publicDir + File.separator);

        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + publicDir + File.separator)
                .resourceChain(false);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect all non-API requests to React's index.html
        registry.addViewController("/{spring:\\w+}")
                .setViewName("forward:/index.html");

        registry.addViewController("/*/{spring:\\w+}")
                .setViewName("forward:/index.html");
    }



    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization", "Content-Disposition");
    }

    public static String getPublicDirectory() {
        File dir = new File("../public");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }
}
