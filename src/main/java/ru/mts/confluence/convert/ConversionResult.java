package ru.mts.confluence.convert;

import java.util.List;

public class ConversionResult {
    private final String markdown;
    private final List<String> attachmentNames;

    public ConversionResult(String markdown, List<String> attachmentNames) {
        this.markdown = markdown;
        this.attachmentNames = attachmentNames;
    }

    public String getMarkdown() {
        return markdown;
    }

    /** Attachment file names referenced by images on the page, in document order, without duplicates. */
    public List<String> getAttachmentNames() {
        return attachmentNames;
    }
}
