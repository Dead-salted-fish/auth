package com.lld.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * 读取classpath下所有yml与properties文件
 * */

@Configuration
public class MultiConfigLoad {
    private static final Logger LOG = LoggerFactory.getLogger(MultiConfigLoad.class);

    public MultiConfigLoad(ConfigurableEnvironment environment) throws IOException {

        loadAllConfigFiles(environment);
    }

    private static void loadAllConfigFiles(ConfigurableEnvironment environment) throws IOException {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] propertiesResources;
        Resource[] ymlResources;

        try {
            propertiesResources = resolver.getResources("classpath:*.properties");
            ymlResources = resolver.getResources("classpath:*.yml");
        } catch (IOException e) {
            LOG.error("Failed to load resources", e);
            return; // Consider whether to throw a custom exception or continue in some cases
        }

        MutablePropertySources propertySources = environment.getPropertySources();


        List<PropertySource<?>> allPropertySources = new ArrayList<>();

        // 读取 properties files
        loadResources(propertiesResources, allPropertySources, "properties");

        // 读取 YAML files
        loadResources(ymlResources, allPropertySources, "yml");

        for (PropertySource<?> propertySource : allPropertySources) {
            propertySources.addLast(propertySource);
        }


    }
    private static void loadResources(Resource[] resources, List<PropertySource<?>> allPropertySources, String fileType) {
        PropertiesPropertySourceLoader propertiesLoader = new PropertiesPropertySourceLoader();
        YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

        for (Resource resource : resources) {
            try {
                if ("properties".equals(fileType)) {
                    List<PropertySource<?>> propertiesSources = propertiesLoader.load(resource.getFilename(), resource);
                    allPropertySources.addAll(propertiesSources);
                    LOG.info("Loaded configuration from: " + resource.getFilename());
                } else if ("yml".equals(fileType)) {
                    List<PropertySource<?>> yamlSources = yamlLoader.load(resource.getFilename(), resource);
                    allPropertySources.addAll(yamlSources);
                    LOG.info("Loaded configuration from: " + resource.getFilename());
                }
            } catch (IOException e) {
                LOG.error("Failed to load " + fileType + " resource: " + resource.getFilename(), e);
            }
        }
    }

}
