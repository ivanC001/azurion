package com.azurion.shared.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticFilesConfig implements WebMvcConfigurer {

    private final String publicFilesRootDir;

    public StaticFilesConfig(
            @Value("${azurion.storage.public-files.root-dir:${user.dir}/storage/public-files}") String publicFilesRootDir
    ) {
        this.publicFilesRootDir = publicFilesRootDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path rootPath = Paths.get(publicFilesRootDir).toAbsolutePath().normalize();
        String location = rootPath.toUri().toString();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
