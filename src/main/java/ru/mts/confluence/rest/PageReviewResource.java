package ru.mts.confluence.rest;

import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.mts.confluence.service.ExportException;
import ru.mts.confluence.service.ExportResult;
import ru.mts.confluence.service.PageExportService;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class PageReviewResource {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PageExportService exportService;
    private final PermissionManager permissionManager;
    private final SpaceManager spaceManager;
    private final I18nResolver i18n;

    public PageReviewResource(PageExportService exportService, PermissionManager permissionManager,
                              SpaceManager spaceManager, I18nResolver i18n) {
        this.exportService = exportService;
        this.permissionManager = permissionManager;
        this.spaceManager = spaceManager;
        this.i18n = i18n;
    }

    @POST
    @Path("pages/{pageId}/send")
    public Response send(@PathParam("pageId") long pageId) {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        if (user == null) {
            return json(401, error("page-review.error.login"));
        }
        try {
            ExportResult result = exportService.send(pageId, user);
            ObjectNode body = MAPPER.createObjectNode();
            body.put("unchanged", result.isUnchanged());
            if (!result.isUnchanged()) {
                body.put("mrUrl", result.getMrUrl());
                body.put("iid", result.getMrIid());
            }
            if (!result.getSkippedAttachments().isEmpty()) {
                body.put("skipped", String.join(", ", result.getSkippedAttachments()));
            }
            return json(200, body);
        } catch (ExportException e) {
            return json(e.getHttpStatus(), error(e.getI18nKey(), e.getArgs()));
        }
    }

    @POST
    @Path("spaces/{spaceKey}/test")
    public Response test(@PathParam("spaceKey") String spaceKey) {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        Space space = spaceManager.getSpace(spaceKey);
        if (user == null || space == null || !permissionManager.hasPermission(user, Permission.ADMINISTER, space)) {
            return json(403, error("page-review.error.forbidden"));
        }
        try {
            String url = exportService.checkConnection(spaceKey);
            ObjectNode body = MAPPER.createObjectNode();
            body.put("projectUrl", url);
            return json(200, body);
        } catch (ExportException e) {
            return json(e.getHttpStatus(), error(e.getI18nKey(), e.getArgs()));
        }
    }

    private ObjectNode error(String key, Object... args) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("error", i18n.getText(key, args));
        return body;
    }

    private static Response json(int status, ObjectNode body) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body.toString()).build();
    }
}
