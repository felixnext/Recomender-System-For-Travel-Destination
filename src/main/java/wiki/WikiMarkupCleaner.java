package wiki;


import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.builder.HtmlDocumentBuilder;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Cleans the wiki markup.
 */
public class WikiMarkupCleaner {

    /**
     * Returns cleaned string
     * @param markup Text with wiki markup
     * @return Raw text
     */
    public static String clean(String markup) {

        StringWriter writer = new StringWriter();

        HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
        builder.setEmitAsDocument(false);

        MarkupParser parser = new MarkupParser(new MediaWikiDialect());
        parser.setBuilder(builder);
        parser.parse(markup);

        final String html = writer.toString();
        final StringBuilder cleaned = new StringBuilder();

        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
            public void handleText(char[] data, int pos) {
                cleaned.append(new String(data)).append(' ');
            }
        };
        try {
            new ParserDelegator().parse(new StringReader(html), callback, false);
        } catch (Exception e) {
            System.out.println(e);
        }

        return cleaned.toString();
    }


}
