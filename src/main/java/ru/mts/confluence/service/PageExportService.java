package ru.mts.confluence.service;

import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.ConfluenceUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mts.confluence.convert.ConversionResult;
import ru.mts.confluence.convert.StorageToMarkdownConverter;
import ru.mts.confluence.gitlab.GitLabClient;
import ru.mts.confluence.gitlab.GitLabException;
import ru.mts.confluence.gitlab.GitLabRepo;
import ru.mts.confluence.settings.SpaceSettings;
import ru.mts.confluence.settings.SpaceSettingsService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PageExportService {
    private static final Logger log = LoggerFactory.getLogger(PageExportService.class);
    private static final long MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PageManager pageManager;
    private final AttachmentManager attachmentManager;
    private final PermissionManager permissionManager;
    private final SettingsManager settingsManager;
    private final SpaceSettingsService settingsService;

    public PageExportService(PageManager pageManager, AttachmentManager attachmentManager,
                             PermissionManager permissionManager, SettingsManager settingsManager,
                             SpaceSettingsService settingsService) {
        this.pageManager = pageManager;
        this.attachmentManager = attachmentManager;
        this.permissionManager = permissionManager;
        this.settingsManager = settingsManager;
        this.settingsService = settingsService;
    }

    public ExportResult send(long pageId, ConfluenceUser user) throws ExportException {
        Page page = pageManager.getPage(pageId);
        if (page == null || !permissionManager.hasPermission(user, Permission.VIEW, page)) {
            throw new ExportException(404, "page-review.error.page-not-found");
        }
        String spaceKey = page.getSpaceKey();
        SpaceSettings settings = settingsService.get(spaceKey);
        if (!settings.isEnabled()) {
            throw new ExportException(403, "page-review.error.disabled");
        }
        if (!settings.isReady()) {
            throw new ExportException(409, "page-review.error.not-configured");
        }
        GitLabRepo repo = parseRepo(settings.getRepoUrl());
        GitLabClient gitlab = new GitLabClient(repo, settingsService.getToken(spaceKey));

        String slug = StorageToMarkdownConverter.slug(page.getTitle(), page.getIdAsString());
        String folder = SpaceSettingsService.normalizeFolder(settings.getBaseFolder());
        String prefix = folder.isEmpty() ? "" : folder + "/";
        String assetsDir = prefix + "assets/" + slug;

        ConversionResult converted = new StorageToMarkdownConverter(title -> pageUrl(spaceKey, title))
                .convert(page.getBodyContent().getBody(), page.getTitle(), "assets/" + slug);

        List<String> skipped = new ArrayList<>();
        List<GitLabClient.FileAction> actions = new ArrayList<>();
        try {
            addIfChanged(gitlab, actions, settings.getTargetBranch(), prefix + slug + ".md",
                    converted.getMarkdown().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            for (String name : converted.getAttachmentNames()) {
                byte[] data = readAttachment(page, name);
                if (data == null) {
                    skipped.add(name);
                    continue;
                }
                addIfChanged(gitlab, actions, settings.getTargetBranch(),
                        assetsDir + "/" + StorageToMarkdownConverter.safeFileName(name), data);
            }
            if (actions.isEmpty()) {
                return new ExportResult(true, null, 0, skipped);
            }
            String branch = "confluence/" + page.getIdAsString() + "-" + STAMP.format(ZonedDateTime.now(ZoneOffset.UTC));
            gitlab.createCommit(branch, settings.getTargetBranch(), "Confluence: " + page.getTitle(), actions);
            GitLabClient.MergeRequest mr = gitlab.createMergeRequest(branch, settings.getTargetBranch(),
                    "Confluence: " + page.getTitle(), description(page, user));
            return new ExportResult(false, mr.getWebUrl(), mr.getIid(), skipped);
        } catch (GitLabException e) {
            throw map(e);
        }
    }

    public String checkConnection(String spaceKey) throws ExportException {
        SpaceSettings settings = settingsService.get(spaceKey);
        if (settings.getRepoUrl().isEmpty() || !settings.isTokenSet()) {
            throw new ExportException(409, "page-review.error.not-configured");
        }
        GitLabClient gitlab = new GitLabClient(parseRepo(settings.getRepoUrl()), settingsService.getToken(spaceKey));
        try {
            String url = gitlab.checkProject();
            gitlab.checkBranch(settings.getTargetBranch());
            return url;
        } catch (GitLabException e) {
            throw map(e);
        }
    }

    private void addIfChanged(GitLabClient gitlab, List<GitLabClient.FileAction> actions, String ref,
                              String path, byte[] content) throws GitLabException {
        Optional<byte[]> existing = gitlab.getFile(path, ref);
        if (existing.isPresent()) {
            if (!Arrays.equals(existing.get(), content)) {
                actions.add(new GitLabClient.FileAction(path, content, true));
            }
        } else {
            actions.add(new GitLabClient.FileAction(path, content, false));
        }
    }

    private byte[] readAttachment(Page page, String name) {
        Attachment attachment = attachmentManager.getAttachment(page, name);
        if (attachment == null || attachment.getFileSize() > MAX_ATTACHMENT_BYTES) {
            return null;
        }
        try (InputStream in = attachmentManager.getAttachmentData(attachment)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.warn("Cannot read attachment {} of page {}", name, page.getId(), e);
            return null;
        }
    }

    private String pageUrl(String spaceKey, String title) {
        Page target = pageManager.getPage(spaceKey, title);
        if (target == null) {
            return null;
        }
        String base = settingsManager.getGlobalSettings().getBaseUrl();
        return base + target.getUrlPath();
    }

    private String description(Page page, ConfluenceUser user) {
        String base = settingsManager.getGlobalSettings().getBaseUrl();
        return "Source: " + base + page.getUrlPath() + "\n\nSent by " + user.getName() + " from Confluence.";
    }

    private static GitLabRepo parseRepo(String url) throws ExportException {
        try {
            return GitLabRepo.parse(url);
        } catch (IllegalArgumentException e) {
            throw new ExportException(409, "page-review.error.bad-repo");
        }
    }

    private static ExportException map(GitLabException e) {
        switch (e.getStatus()) {
            case 401:
                return new ExportException(502, "page-review.error.gitlab-401");
            case 403:
                return new ExportException(502, "page-review.error.gitlab-403");
            case 404:
                return new ExportException(502, "page-review.error.gitlab-404");
            case -1:
                log.warn("GitLab unreachable", e);
                return new ExportException(502, "page-review.error.gitlab-unreachable");
            default:
                log.warn("GitLab returned HTTP {}: {}", e.getStatus(), e.getMessage());
                return new ExportException(502, "page-review.error.gitlab-other", e.getStatus(), e.getMessage());
        }
    }
}
