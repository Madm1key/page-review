package ru.mts.confluence.convert;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public class StorageToMarkdownConverter {
    private static final Set<String> PANELS = Set.of("info", "note", "warning", "tip");
    private static final Set<String> DROPPED_MACROS = Set.of("toc", "children", "pagetree", "recently-updated", "livesearch");

    private final Function<String, String> pageLinkResolver;

    /** @param pageLinkResolver maps a Confluence page title to its URL, may return null; may itself be null */
    public StorageToMarkdownConverter(Function<String, String> pageLinkResolver) {
        this.pageLinkResolver = pageLinkResolver;
    }

    public ConversionResult convert(String storageXhtml, String title, String assetsPath) {
        Document doc = Jsoup.parse(storageXhtml == null ? "" : storageXhtml, "", Parser.xmlParser());
        doc.outputSettings().prettyPrint(false);
        Set<String> attachments = new LinkedHashSet<>();

        convertImages(doc, assetsPath, attachments);
        convertLinks(doc);
        convertEmoticons(doc);
        convertTasks(doc);
        convertMacros(doc);
        unwrapRemainingNamespaced(doc);

        String html = doc.outerHtml();
        MutableDataSet options = new MutableDataSet()
                .set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false)
                .set(FlexmarkHtmlConverter.UNORDERED_LIST_DELIMITER, '-');
        String md = FlexmarkHtmlConverter.builder(options).build().convert(html).trim();
        String result = "# " + title + "\n\n" + md + "\n";
        return new ConversionResult(result, new ArrayList<>(attachments));
    }

    private void convertImages(Document doc, String assetsPath, Set<String> attachments) {
        for (Element image : doc.select("ac|image")) {
            Element img = new Element("img");
            Element attachment = image.selectFirst("ri|attachment");
            Element url = image.selectFirst("ri|url");
            if (attachment != null) {
                String name = safeFileName(attachment.attr("ri:filename"));
                attachments.add(attachment.attr("ri:filename"));
                img.attr("src", assetsPath + "/" + name);
                img.attr("alt", name);
            } else if (url != null) {
                img.attr("src", url.attr("ri:value"));
                img.attr("alt", "image");
            } else {
                image.remove();
                continue;
            }
            image.replaceWith(img);
        }
    }

    private void convertLinks(Document doc) {
        for (Element link : doc.select("ac|link")) {
            Element page = link.selectFirst("ri|page");
            Element attachment = link.selectFirst("ri|attachment");
            Element user = link.selectFirst("ri|user");
            String fallback = page != null ? page.attr("ri:content-title")
                    : attachment != null ? attachment.attr("ri:filename")
                    : user != null ? "@user" : link.attr("ac:anchor");
            String text = linkText(link, fallback);
            String href = page != null && pageLinkResolver != null
                    ? pageLinkResolver.apply(page.attr("ri:content-title")) : null;
            if (href != null) {
                Element a = new Element("a").attr("href", href);
                a.appendChild(new TextNode(text));
                link.replaceWith(a);
            } else {
                link.replaceWith(new TextNode(text));
            }
        }
    }

    private static String linkText(Element link, String fallback) {
        Element body = link.selectFirst("ac|plain-text-link-body");
        if (body == null) {
            body = link.selectFirst("ac|link-body");
        }
        String t = body == null ? "" : body.text();
        return t.isEmpty() ? fallback : t;
    }

    private void convertEmoticons(Document doc) {
        for (Element e : doc.select("ac|emoticon")) {
            String fallback = e.attr("ac:emoji-fallback");
            if (fallback.isEmpty()) {
                e.remove();
            } else {
                e.replaceWith(new TextNode(fallback));
            }
        }
    }

    private void convertTasks(Document doc) {
        for (Element list : doc.select("ac|task-list")) {
            Element ul = new Element("ul");
            for (Element task : list.select("> ac|task")) {
                boolean done = "complete".equals(task.selectFirst("> ac|task-status") == null
                        ? "" : task.selectFirst("> ac|task-status").text());
                Element body = task.selectFirst("> ac|task-body");
                Element li = new Element("li");
                li.appendChild(new TextNode((done ? "[x] " : "[ ] ")));
                if (body != null) {
                    for (Node child : new ArrayList<>(body.childNodes())) {
                        li.appendChild(child);
                    }
                }
                ul.appendChild(li);
            }
            list.replaceWith(ul);
        }
    }

    private void convertMacros(Document doc) {
        // inner-first, so nested macros are processed before their containers
        List<Element> macros = new ArrayList<>(doc.select("ac|structured-macro"));
        for (int i = macros.size() - 1; i >= 0; i--) {
            Element macro = macros.get(i);
            String name = macro.attr("ac:name").toLowerCase(Locale.ROOT);
            Element richBody = macro.selectFirst("> ac|rich-text-body");
            if (name.equals("code") || name.equals("noformat")) {
                macro.replaceWith(codeBlock(macro, name.equals("code")));
            } else if (PANELS.contains(name)) {
                macro.replaceWith(panel(name, richBody));
            } else if (DROPPED_MACROS.contains(name)) {
                macro.remove();
            } else if (richBody != null) {
                // keep the content of containers such as expand/panel/section
                Element holder = new Element("div");
                for (Node child : new ArrayList<>(richBody.childNodes())) {
                    holder.appendChild(child);
                }
                macro.replaceWith(holder);
            } else {
                macro.replaceWith(new Comment(" macro: " + name + " "));
            }
        }
    }

    private Element codeBlock(Element macro, boolean withLanguage) {
        Element pre = new Element("pre");
        Element code = new Element("code");
        if (withLanguage) {
            for (Element p : macro.select("> ac|parameter")) {
                if (p.attr("ac:name").equals("language") && !p.text().isEmpty()) {
                    code.addClass("language-" + p.text());
                }
            }
        }
        Element body = macro.selectFirst("> ac|plain-text-body");
        String text = body == null ? "" : body.wholeText();
        code.appendChild(new TextNode(text));
        pre.appendChild(code);
        return pre;
    }

    private Element panel(String name, Element richBody) {
        Element quote = new Element("blockquote");
        Element head = new Element("p");
        Element strong = new Element("strong");
        strong.appendChild(new TextNode(name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1)));
        head.appendChild(strong);
        quote.appendChild(head);
        if (richBody != null) {
            for (Node child : new ArrayList<>(richBody.childNodes())) {
                quote.appendChild(child);
            }
        }
        return quote;
    }

    private void unwrapRemainingNamespaced(Document doc) {
        for (Element e : new ArrayList<>(doc.select("*"))) {
            String tag = e.tagName();
            if (tag.startsWith("ac:") || tag.startsWith("ri:")) {
                if (tag.equals("ac:parameter") || tag.equals("ac:placeholder")) {
                    e.remove();
                } else {
                    e.unwrap();
                }
            }
        }
        for (Node n : new ArrayList<>(doc.childNodes())) {
            replaceCData(n);
        }
    }

    private void replaceCData(Node node) {
        for (Node child : new ArrayList<>(node.childNodes())) {
            if (child instanceof CDataNode) {
                child.replaceWith(new TextNode(((CDataNode) child).text()));
            } else {
                replaceCData(child);
            }
        }
    }

    public static String safeFileName(String name) {
        return name.replace('/', '_').replace('\\', '_');
    }

    public static String slug(String title, String fallback) {
        String s = title == null ? "" : title.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}]+", "-")
                .replaceAll("^-+|-+$", "");
        return s.isEmpty() ? fallback : s;
    }
}
