package ru.mts.confluence.config;

import static com.atlassian.plugins.osgi.javaconfig.OsgiServices.importOsgiService;

import com.atlassian.plugins.osgi.javaconfig.configs.beans.ModuleFactoryBean;
import com.atlassian.plugins.osgi.javaconfig.configs.beans.PluginAccessorBean;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.mts.confluence.settings.SpaceSettingsService;

@Configuration
@Import({ModuleFactoryBean.class, PluginAccessorBean.class})
public class PageReviewJavaConfig {

  @Bean
  public PluginSettingsFactory pluginSettingsFactory() {
    return importOsgiService(PluginSettingsFactory.class);
  }

  @Bean
  public SpaceSettingsService spaceSettingsService(PluginSettingsFactory pluginSettingsFactory) {
    return new SpaceSettingsService(pluginSettingsFactory);
  }
}
