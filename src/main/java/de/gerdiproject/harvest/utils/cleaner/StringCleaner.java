package de.gerdiproject.harvest.utils.cleaner;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;

public class StringCleaner implements ICleaner<String>
{
    private static final int MIN_ESCAPE = 2;
    private static final int MAX_ESCAPE = 6;
    private static final int DECIMAL_RADIX = 10;
    private static final int HEXADECIMAL_RADIX = 16;
    private static final int MAX_SINGLE_CHAR_VALUE = 0xFFFF;

    /**
     * HTML 3 escapes
     */
    private static final Map<String, String> ESCAPE_LOOKUP_TABLE = createEscapeMap();

    /**
     * Creates a HashMap that maps escape sequences to the sign that is escaped.
     * @return a HTML 3 escape hash map
     */
    private static Map<String, String> createEscapeMap()
    {
        Map<String, String> m = new HashMap<>();
        m.put("quot", "\"");        // " - double-quote
        m.put("amp", "&");          // & - ampersand
        m.put("lt", "<");           // < - less-than
        m.put("gt", ">");           // > - greater-than
        m.put("nbsp", "\u00A0");    // non-breaking space
        m.put("iexcl", "\u00A1");   // inverted exclamation mark
        m.put("cent", "\u00A2");    // cent sign
        m.put("pound", "\u00A3");   // pound sign
        m.put("curren", "\u00A4");  // currency sign
        m.put("yen", "\u00A5");     // yen sign = yuan sign
        m.put("brvbar", "\u00A6");  // broken bar = broken vertical bar
        m.put("sect", "\u00A7");    // section sign
        m.put("uml", "\u00A8");     // diaeresis = spacing diaeresis
        m.put("copy", "\u00A9");    // © - copyright sign
        m.put("ordf", "\u00AA");    // feminine ordinal indicator
        m.put("laquo", "\u00AB");   // left-pointing double angle quotation mark = left pointing    // guillemet
        m.put("not", "\u00AC");     // not sign
        m.put("shy", "\u00AD");     // soft hyphen = discretionary hyphen
        m.put("reg", "\u00AE");     // ® - registered trademark sign
        m.put("macr", "\u00AF");    // macron = spacing macron = overline = APL overbar
        m.put("deg", "\u00B0");     // degree sign
        m.put("plusmn", "\u00B1");  // plus-minus sign = plus-or-minus sign
        m.put("sup2", "\u00B2");    // superscript two = superscript digit two = squared
        m.put("sup3", "\u00B3");    // superscript three = superscript digit three = cubed
        m.put("acute", "\u00B4");   // acute accent = spacing acute
        m.put("micro", "\u00B5");   // micro sign
        m.put("para", "\u00B6");    // pilcrow sign = paragraph sign
        m.put("middot", "\u00B7");  // middle dot = Georgian comma = Greek middle dot
        m.put("cedil", "\u00B8");   // cedilla = spacing cedilla
        m.put("sup1", "\u00B9");    // superscript one = superscript digit one
        m.put("ordm", "\u00BA");    // masculine ordinal indicator
        m.put("raquo", "\u00BB");   // right-pointing double angle quotation mark = right pointing  // guillemet
        m.put("frac14", "\u00BC");  // vulgar fraction one quarter = fraction one quarter
        m.put("frac12", "\u00BD");  // vulgar fraction one half = fraction one half
        m.put("frac34", "\u00BE");  // vulgar fraction three quarters = fraction three quarters
        m.put("iquest", "\u00BF");  // inverted question mark = turned question mark
        m.put("Agrave", "\u00C0");  // А - uppercase A, grave accent
        m.put("Aacute", "\u00C1");  // Б - uppercase A, acute accent
        m.put("Acirc", "\u00C2");   // В - uppercase A, circumflex accent
        m.put("Atilde", "\u00C3");  // Г - uppercase A, tilde
        m.put("Auml", "\u00C4");    // Д - uppercase A, umlaut
        m.put("Aring", "\u00C5");   // Е - uppercase A, ring
        m.put("AElig", "\u00C6");   // Ж - uppercase AE
        m.put("Ccedil", "\u00C7");  // З - uppercase C, cedilla
        m.put("Egrave", "\u00C8");  // И - uppercase E, grave accent
        m.put("Eacute", "\u00C9");  // Й - uppercase E, acute accent
        m.put("Ecirc", "\u00CA");   // К - uppercase E, circumflex accent
        m.put("Euml", "\u00CB");    // Л - uppercase E, umlaut
        m.put("Igrave", "\u00CC");  // М - uppercase I, grave accent
        m.put("Iacute", "\u00CD");  // Н - uppercase I, acute accent
        m.put("Icirc", "\u00CE");   // О - uppercase I, circumflex accent
        m.put("Iuml", "\u00CF");    // П - uppercase I, umlaut
        m.put("ETH", "\u00D0");     // Р - uppercase Eth, Icelandic
        m.put("Ntilde", "\u00D1");  // С - uppercase N, tilde
        m.put("Ograve", "\u00D2");  // Т - uppercase O, grave accent
        m.put("Oacute", "\u00D3");  // У - uppercase O, acute accent
        m.put("Ocirc", "\u00D4");   // Ф - uppercase O, circumflex accent
        m.put("Otilde", "\u00D5");  // Х - uppercase O, tilde
        m.put("Ouml", "\u00D6");    // Ц - uppercase O, umlaut
        m.put("times", "\u00D7");   // multiplication sign
        m.put("Oslash", "\u00D8");  // Ш - uppercase O, slash
        m.put("Ugrave", "\u00D9");  // Щ - uppercase U, grave accent
        m.put("Uacute", "\u00DA");  // Ъ - uppercase U, acute accent
        m.put("Ucirc", "\u00DB");   // Ы - uppercase U, circumflex accent
        m.put("Uuml", "\u00DC");    // Ь - uppercase U, umlaut
        m.put("Yacute", "\u00DD");  // Э - uppercase Y, acute accent
        m.put("THORN", "\u00DE");   // Ю - uppercase THORN, Icelandic
        m.put("szlig", "\u00DF");   // Я - lowercase sharps, German
        m.put("agrave", "\u00E0");  // а - lowercase a, grave accent
        m.put("aacute", "\u00E1");  // б - lowercase a, acute accent
        m.put("acirc", "\u00E2");   // в - lowercase a, circumflex accent
        m.put("atilde", "\u00E3");  // г - lowercase a, tilde
        m.put("auml", "\u00E4");    // д - lowercase a, umlaut
        m.put("aring", "\u00E5");   // е - lowercase a, ring
        m.put("aelig", "\u00E6");   // ж - lowercase ae
        m.put("ccedil", "\u00E7");  // з - lowercase c, cedilla
        m.put("egrave", "\u00E8");  // и - lowercase e, grave accent
        m.put("eacute", "\u00E9");  // й - lowercase e, acute accent
        m.put("ecirc", "\u00EA");   // к - lowercase e, circumflex accent
        m.put("euml", "\u00EB");    // л - lowercase e, umlaut
        m.put("igrave", "\u00EC");  // м - lowercase i, grave accent
        m.put("iacute", "\u00ED");  // н - lowercase i, acute accent
        m.put("icirc", "\u00EE");   // о - lowercase i, circumflex accent
        m.put("iuml", "\u00EF");    // п - lowercase i, umlaut
        m.put("eth", "\u00F0");     // р - lowercase eth, Icelandic
        m.put("ntilde", "\u00F1");  // с - lowercase n, tilde
        m.put("ograve", "\u00F2");  // т - lowercase o, grave accent
        m.put("oacute", "\u00F3");  // у - lowercase o, acute accent
        m.put("ocirc", "\u00F4");   // ф - lowercase o, circumflex accent
        m.put("otilde", "\u00F5");  // х - lowercase o, tilde
        m.put("ouml", "\u00F6");    // ц - lowercase o, umlaut
        m.put("divide", "\u00F7");  // division sign
        m.put("oslash", "\u00F8");  // ш - lowercase o, slash
        m.put("ugrave", "\u00F9");  // щ - lowercase u, grave accent
        m.put("uacute", "\u00FA");  // ъ - lowercase u, acute accent
        m.put("ucirc", "\u00FB");   // ы - lowercase u, circumflex accent
        m.put("uuml", "\u00FC");    // ь - lowercase u, umlaut
        m.put("yacute", "\u00FD");  // э - lowercase y, acute accent
        m.put("thorn", "\u00FE");   // ю - lowercase thorn, Icelandic
        m.put("yuml", "\u00FF");    // я - lowercase y, umlaut
        return m;
    }


    /**
     * Cleans up a String, removing unwanted character escapes and trimming it.
     *
     * @param dirtyObject
     *            the String which is to be cleaned
     * @return a cleaned String
     */
    @Override
    public String clean(String dirtyObject)
    {
        // remove HTML tags from text if they exist
        if (dirtyObject.indexOf('<') != -1)
            dirtyObject = Jsoup.parse(dirtyObject).text();

        // unescape characters
        dirtyObject = unescapeHtml(dirtyObject);

        // merge whitespaces
        dirtyObject = dirtyObject.replaceAll("[\\u00A0\\s]{2,}", " ");

        // remove whitespaces at the beginning and end
        return dirtyObject.trim();
    }


    /**
     * Unescapes escaped HTML characters.
     *
     * @param input
     *            a HTML input text
     * @return a text with unescaped characters.
     */
    public static final String unescapeHtml(final String input)
    {
        StringWriter writer = null;
        int st = 0;
        int startIndex = 0;

        while (true) {
            // look for '&'
            startIndex = input.indexOf('&', startIndex) + 1;

            if (startIndex == 0)
                break;

            int endIndex = input.indexOf(';', startIndex);

            // found '&', look for ';'
            int len = endIndex - startIndex;

            if (endIndex == -1 || len < MIN_ESCAPE || len > MAX_ESCAPE) {
                startIndex++;
                continue;
            }

            // found escape
            if (input.charAt(startIndex) == '#') {
                // numeric escape
                int numberStartIndex = startIndex + 1;
                int radix = DECIMAL_RADIX;

                // check if the number is hexadecimal
                final char firstChar = input.charAt(numberStartIndex);

                if (firstChar == 'x' || firstChar == 'X') {
                    numberStartIndex++;
                    radix = HEXADECIMAL_RADIX;
                }

                try {
                    int entityValue = Integer.parseUnsignedInt(input.substring(numberStartIndex, endIndex), radix);

                    if (writer == null)
                        writer = new StringWriter(input.length());

                    writer.append(input.substring(st, startIndex - 1));

                    if (entityValue > MAX_SINGLE_CHAR_VALUE) {
                        final char[] chrs = Character.toChars(entityValue);
                        writer.write(chrs[0]);
                        writer.write(chrs[1]);
                    } else
                        writer.write(entityValue);

                } catch (NumberFormatException ex) {
                    startIndex++;
                    continue;
                }
            } else {
                // named escape
                CharSequence value = ESCAPE_LOOKUP_TABLE.get(input.substring(startIndex, endIndex));

                if (value == null) {
                    startIndex++;
                    continue;
                }

                if (writer == null)
                    writer = new StringWriter(input.length());

                writer.append(input.substring(st, startIndex - 1));

                writer.append(value);
            }

            // skip escape sequence
            st = endIndex + 1;
            startIndex = st;
        }

        return (writer == null)
               ? input
               : writer.append(input.substring(st)).toString();
    }
}
