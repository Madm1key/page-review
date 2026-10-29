package ru.mts.confluence.service;

import java.util.List;

public class ExportResult {
    private final boolean unchanged;
    private final String mrUrl;
    private final long mrIid;
    private final List<String> skippedAttachments;

    public ExportResult(boolean unchanged, String mrUrl, long mrIid, List<String> skippedAttachments) {
        this.unchanged = unchanged;
        this.mrUrl = mrUrl;
        this.mrIid = mrIid;
        this.skippedAttachments = skippedAttachments;
    }

    public boolean isUnchanged() {
        return unchanged;
    }

    public String getMrUrl() {
        return mrUrl;
    }

    public long getMrIid() {
        return mrIid;
    }

    public List<String> getSkippedAttachments() {
        return skippedAttachments;
    }
}
