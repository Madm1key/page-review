package ru.mts.confluence.web;

import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.plugin.descriptor.web.WebInterfaceContext;
import com.atlassian.confluence.plugin.descriptor.web.conditions.BaseConfluenceCondition;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.ConfluenceUser;
import ru.mts.confluence.settings.SpaceSettingsService;

public class PageReviewCondition extends BaseConfluenceCondition {
    private final SpaceSettingsService settingsService;
    private final PermissionManager permissionManager;

    public PageReviewCondition(SpaceSettingsService settingsService, PermissionManager permissionManager) {
        this.settingsService = settingsService;
        this.permissionManager = permissionManager;
    }

    @Override
    protected boolean shouldDisplay(WebInterfaceContext context) {
        AbstractPage page = context.getPage();
        ConfluenceUser user = context.getCurrentUser();
        if (!(page instanceof Page) || user == null || !page.isCurrent()) {
            return false;
        }
        return settingsService.get(page.getSpaceKey()).isEnabled()
                && permissionManager.hasPermission(user, Permission.VIEW, page);
    }
}
