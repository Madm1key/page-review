package ru.mts.confluence.convert;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StorageToMarkdownConverterTest {
    private final StorageToMarkdownConverter converter =
            new StorageToMarkdownConverter(t -> t.equals("Other") ? "https://c.example/pages/1" : null);

    private ConversionResult convert(String xhtml) {
        return converter.convert(xhtml, "My Page", "assets/my-page");
    }

    @Test
    public void addsTitleAndConvertsBasics() {
        String md = convert("<h2>Head</h2><p>Hello <strong>bold</strong></p><ul><li>a</li><li>b</li></ul>").getMarkdown();
        assertTrue(md, md.startsWith("# My Page\n"));
        assertTrue(md, md.contains("## Head"));
        assertTrue(md, md.contains("**bold**"));
        assertTrue(md, md.contains("- a"));
    }

    @Test
    public void convertsCodeMacro() {
        String md = convert("<ac:structured-macro ac:name=\"code\"><ac:parameter ac:name=\"language\">java</ac:parameter>"
                + "<ac:plain-text-body><![CDATA[int a = 1 < 2;]]></ac:plain-text-body></ac:structured-macro>").getMarkdown();
        assertTrue(md, md.contains("```java"));
        assertTrue(md, md.contains("int a = 1 < 2;"));
    }

    @Test
    public void convertsPanelToBlockquote() {
        String md = convert("<ac:structured-macro ac:name=\"info\"><ac:rich-text-body><p>Careful</p></ac:rich-text-body>"
                + "</ac:structured-macro>").getMarkdown();
        assertTrue(md, md.contains("> **Info**"));
        assertTrue(md, md.contains("> Careful"));
    }

    @Test
    public void collectsAttachmentImages() {
        ConversionResult r = convert("<p><ac:image><ri:attachment ri:filename=\"pic 1.png\"/></ac:image>"
                + "<ac:image><ri:attachment ri:filename=\"pic 1.png\"/></ac:image></p>");
        assertEquals(Collections.singletonList("pic 1.png"), r.getAttachmentNames());
        assertTrue(r.getMarkdown(), r.getMarkdown().contains("assets/my-page/pic"));
    }

    @Test
    public void resolvesPageLinks() {
        String md = convert("<p><ac:link><ri:page ri:content-title=\"Other\"/>"
                + "<ac:plain-text-link-body><![CDATA[see this]]></ac:plain-text-link-body></ac:link></p>").getMarkdown();
        assertTrue(md, md.contains("[see this](https://c.example/pages/1)"));
    }

    @Test
    public void dropsTocAndMarksUnknownMacros() {
        String md = convert("<ac:structured-macro ac:name=\"toc\"/><ac:structured-macro ac:name=\"jira\"/><p>text</p>")
                .getMarkdown();
        assertFalse(md, md.contains("toc"));
        assertTrue(md, md.contains("text"));
    }

    @Test
    public void keepsContentOfContainerMacros() {
        String md = convert("<ac:structured-macro ac:name=\"expand\"><ac:rich-text-body><p>inside</p></ac:rich-text-body>"
                + "</ac:structured-macro>").getMarkdown();
        assertTrue(md, md.contains("inside"));
    }

    @Test
    public void slugKeepsUnicodeLetters() {
        assertEquals("мой-документ-v2", StorageToMarkdownConverter.slug("Мой документ (v2)!", "1"));
        assertEquals("42", StorageToMarkdownConverter.slug("???", "42"));
    }
}
