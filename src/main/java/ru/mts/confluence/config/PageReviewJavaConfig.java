package ru.mts.confluence.config;

import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.plugins.osgi.javaconfig.configs.beans.ModuleFactoryBean;
import com.atlassian.plugins.osgi.javaconfig.configs.beans.PluginAccessorBean;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.mts.confluence.service.PageExportService;
import ru.mts.confluence.settings.SpaceSettingsService;

import static com.atlassian.plugins.osgi.javaconfig.OsgiServices.importOsgiService;

@Configuration
@Import({
        ModuleFactoryBean.class,
        PluginAccessorBean.class
})
public class PageReviewJavaConfig {

    @Bean
    public PluginSettingsFactory pluginSettingsFactory() {
        return importOsgiService(PluginSettingsFactory.class);
    }

    @Bean
    public I18nResolver i18nResolver() {
        return importOsgiService(I18nResolver.class);
    }

    @Bean
    public PageManager pageManager() {
        return importOsgiService(PageManager.class);
    }

    @Bean
    public SpaceManager spaceManager() {
        return importOsgiService(SpaceManager.class);
    }

    @Bean
    public AttachmentManager attachmentManager() {
        return importOsgiService(AttachmentManager.class);
    }

    @Bean
    public PermissionManager permissionManager() {
        return importOsgiService(PermissionManager.class);
    }

    @Bean
    public SettingsManager settingsManager() {
        return importOsgiService(SettingsManager.class);
    }

    @Bean
    public SpaceSettingsService spaceSettingsService(PluginSettingsFactory pluginSettingsFactory) {
        return new SpaceSettingsService(pluginSettingsFactory);
    }

    @Bean
    public PageExportService pageExportService(PageManager pageManager, AttachmentManager attachmentManager,
                                               PermissionManager permissionManager, SettingsManager settingsManager,
                                               SpaceSettingsService spaceSettingsService) {
        return new PageExportService(pageManager, attachmentManager, permissionManager, settingsManager,
                spaceSettingsService);
    }
}
