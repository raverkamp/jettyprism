package spinat.jettyprism;

import java.io.FileInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

// a simple replacement for JConfiguration or so

public class Configuration {

    final HashMap<String, Category> categories;
    final HashMap<String, String> variables;
    final HashMap<String, String> general;

    private Configuration(HashMap<String, Category> categories,
            HashMap<String, String> variables) {
        this.categories = categories;
        this.variables = variables;
        if (categories.containsKey("general")) {
            general = categories.get("general").map;
        } else {
            general = new HashMap<String, String>();
        }
    }

    public String getPropertyi(String name, String cat) {
        if (categories.containsKey(cat)) {
            Category c = categories.get(cat);
            if (c.map.containsKey(name)) {
                return c.map.get(name);
            } else {
                if (general.containsKey(name)) {
                    return general.get(name);
                } else {
                    return null;
                }
            }
        } else {
            throw new RuntimeException("no category: " + cat);
        }
    }

    public String getProperty(String name, String default_, String cat) {
        String s = getPropertyi(name, cat);
        if (null == s) {
            return default_;
        } else {
            return s;
        }
    }

    public String getProperty(String name, String default_) {
        String s = general.get(name);
        if (null == s) {
            return default_;
        } else {
            return s;
        }

    }

    public String getProperty(String name) {
        return general.get(name);
    }

//    public void setProperty(String s1, String s2) {
//        throw new UnsupportedOperationException();
//    }
//
//    public void setProperty(String s1, String s2, String s3) {
//        throw new UnsupportedOperationException();
//    }
    public boolean getBooleanProperty(String name, boolean default_, String cat) {
        String s = getPropertyi(name, cat);
        if (null == s) {
            return default_;
        } else {
            if (s.equals("1") || s.equals("true")) {
                return true;
            } else if (s.equals("0") || s.equals("false")) {
                return false;
            } else {
                throw new RuntimeException("not a bool value");
            }
        }
    }

    public boolean getBooleanProperty(String name, boolean default_) {
        String s = getProperty(name);
        if (null == s) {
            return default_;
        } else {
            if (s.equals("1") || s.equals("true")) {
                return true;
            } else if (s.equals("0") || s.equals("false")) {
                return false;
            } else {
                throw new RuntimeException("not a bool value");
            }
        }
    }

    public int getIntProperty(String name, int default_, String cat) {
        String s = getPropertyi(name, cat);
        if (null == s) {
            return default_;
        } else {
            return Integer.parseInt(s);
        }
    }

    public int getIntProperty(String name, int default_) {
        String s = getProperty(name);
        if (null == s) {
            return default_;
        } else {
            return Integer.parseInt(s);
        }
    }

//    public void setCategory(String s) {
//        throw new UnsupportedOperationException();
//    }
    static XMLEvent peekTag(XMLEventReader er) throws XMLStreamException {
        if (!er.hasNext()) {
            return null;
        }
        while (true) {
            XMLEvent e = er.peek();
            if (e == null) {
                return null;
            }
            if (e.isCharacters()) {
                if (e.asCharacters().isWhiteSpace()) {
                    er.nextEvent(); // skip
                } else {
                    throw new RuntimeException("text entity is not only whitespace at " + e.getLocation());
                }
            } else if (e.isProcessingInstruction() || e.getEventType() == XMLStreamConstants.COMMENT) {
                er.nextEvent();
            } else {
                return e;
            }
        }
    }

    static String getProperty(StartElement e, String name) throws XMLStreamException {
        Attribute a = e.getAttributeByName(new QName(name));
        if (a == null) {
            throw new XMLStreamException("expecting attribute " + name + " on element " + e, e.getLocation());
        } else {
            return a.getValue();
        }
    }

    public static HashMap<String, String> parseVariables(XMLEventReader xsr) throws XMLStreamException {
        XMLEvent e = xsr.nextTag(); // this should be the starter
        HashMap<String, String> m = new HashMap<String, String>();
        while (true) {
            XMLEvent e2 = xsr.nextTag();
            if (e2.isEndElement() && e2.asEndElement().getName().getLocalPart().equals("variables")) {
                return m;
            } else if (e2.isStartElement() && e2.asStartElement().getName().getLocalPart().equals("variable")) {
                String name = getProperty(e2.asStartElement(), "name");
                String value = getProperty(e2.asStartElement(), "value");
                m.put(name, value);
                XMLEvent e3 = xsr.nextTag();
                if (!(e3.isEndElement() && e3.asEndElement().getName().getLocalPart().equals("variable"))) {
                    throw new XMLStreamException("expecting variable end", e3.getLocation());
                }
            } else {
                throw new XMLStreamException("expecting new variable or variables end", e2.getLocation());
            }
        }
    }

    public static class Category {

        final String name;
        final HashMap<String, String> map;

        public Category(String name, HashMap<String, String> map) {
            this.name = name;
            this.map = map;
        }
    }

    public static Category parseCategory(XMLEventReader xsr) throws XMLStreamException {
        XMLEvent e = xsr.nextTag(); // this should be the starter
        String catName = getProperty(e.asStartElement(), "name");
        HashMap<String, String> m = new HashMap<String, String>();
        System.out.println("category: " + catName);
        while (true) {
            XMLEvent e2 = xsr.nextTag();
            if (e2.isEndElement() && e2.asEndElement().getName().getLocalPart().equals("category")) {
                return new Category(catName, m);
            } else if (e2.isStartElement() && e2.asStartElement().getName().getLocalPart().equals("property")) {
                String name = getProperty(e2.asStartElement(), "name");
                String value = getProperty(e2.asStartElement(), "value");
                m.put(name, value);
                System.out.println(" " + name + ": " + value);
                XMLEvent e3 = xsr.nextTag();
                if (!(e3.isEndElement() && e3.asEndElement().getName().getLocalPart().equals("property"))) {
                    throw new XMLStreamException("expecting property end", e3.getLocation());
                }
            } else {
                throw new XMLStreamException("expecting new category or end", e2.getLocation());
            }
        }
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("--- variables ---\n");
        for (Map.Entry<String, String> kv : this.variables.entrySet()) {
            b.append(kv.getKey() + " : " + kv.getValue() + "\n");
        }
        b.append("--- categories ---\n");
        for (Map.Entry<String, Category> cm : this.categories.entrySet()) {
            for (Map.Entry<String, String> x : cm.getValue().map.entrySet()) {
                b.append(cm.getKey() + "." + x.getKey() + ": " + x.getValue() + "\n");
            }
        }
        return b.toString();
    }

    public static Configuration load(String filename) throws Exception {
        XMLInputFactory xinf = XMLInputFactory.newFactory();
        xinf.setProperty("javax.xml.stream.isReplacingEntityReferences", true);
        FileInputStream is = new FileInputStream(filename);
        XMLEventReader xsr = xinf.createXMLEventReader(is);
        return load(xsr);
    }

    public static Configuration loadFromString(String s) throws Exception {
        Reader r = new StringReader(s);
        XMLInputFactory xinf = XMLInputFactory.newFactory();
        xinf.setProperty("javax.xml.stream.isReplacingEntityReferences", true);
        XMLEventReader xsr = xinf.createXMLEventReader(r);
        return load(xsr);
    }

    public static Configuration load(XMLEventReader xsr) throws Exception {
        XMLEvent e = xsr.nextEvent();
        if (e.getEventType() != XMLStreamConstants.START_DOCUMENT) {
            throw new RuntimeException("aua");
        }
        final HashMap<String, String> vm;
        final HashMap<String, Category> cm = new HashMap<String, Category>();
        XMLEvent e1 = xsr.nextTag();
        if (e1.isStartElement()
                && e1.asStartElement().getName().getLocalPart().equals("properties")) {
            XMLEvent e2 = peekTag(xsr);

            if (e2.isStartElement()
                    && e2.asStartElement().getName().getLocalPart().equals("variables")) {
                vm = parseVariables(xsr);
            } else {
                vm = new HashMap<String, String>();
            }
            while (true) {
                // now parsing categories
                XMLEvent ec = peekTag(xsr);
                System.out.println(ec.getClass());
                if (ec.isStartElement() && ec.asStartElement().getName().getLocalPart().equals("category")) {
                    Category c = parseCategory(xsr);
                    cm.put(c.name, c);
                } else if (ec.isEndElement() && ec.asEndElement().getName().getLocalPart().equals("properties")) {
                    XMLEvent ignore = xsr.nextTag();
                    break;
                }
            }

        } else {
            throw new RuntimeException("expecting properties");
        }
        return new Configuration(cm, vm);
    }
}
