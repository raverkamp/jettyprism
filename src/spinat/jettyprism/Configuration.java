package spinat.jettyprism;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// a simple replacement for JConfiguration or so
public class Configuration {

    public static class Category {

        final String name;
        final HashMap<String, String> map;

        public Category(String name, HashMap<String, String> map) {
            this.name = name;
            this.map = map;
        }
    }

    final HashMap<String, Category> categories;
    final HashMap<String, String> general;

    public static Configuration loadFromProperties(java.util.Properties props) {
        HashMap<String, Category> cats = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            int p = key.indexOf(".");
            int p2 = key.lastIndexOf(".");
            // exactly one . in string
            if (p < 0 || p != p2) {
                continue;
            }
            String cat = key.substring(0, p);
            String subkey = key.substring(p + 1);

            if (!cats.containsKey(cat)) {
                cats.put(cat, new Category(cat, new HashMap<String, String>()));
            }
            String value = (String) props.getProperty(key);
            final String realValue;
            if (value == null || !value.startsWith("$")) {
                realValue = value;
            } else if (value.startsWith("$$")) {
                realValue = value.substring(1);
            } else {
                String envKey = value.substring(1);
                realValue = System.getenv(envKey);
            }
            cats.get(cat).map.put(subkey, realValue);
        }
        return new Configuration(cats, null);
    }

    public static Configuration loadFromPropertiesFile(String fileName) throws IOException {
        try (InputStream ins = new FileInputStream(fileName)) {
            return loadFromStream(ins);
        }
    }

    public static Configuration loadFromStream(InputStream ins) throws IOException {
        java.util.Properties props = new java.util.Properties();
        props.load(ins);
        return loadFromProperties(props);
    }

    public static Configuration loadFromPropertiesString(String s) throws IOException {
        StringReader sr = new StringReader(s);
        java.util.Properties props = new java.util.Properties();
        props.load(sr);
        return loadFromProperties(props);
    }

    public static String storePropertiesAsString(Properties props) throws IOException {
        StringWriter w = new StringWriter();
        props.store(w, "");
        return w.toString();
    }

    private Configuration(HashMap<String, Category> categories,
            HashMap<String, String> variables) {
        this.categories = categories;
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

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("--- categories ---\n");
        for (Map.Entry<String, Category> cm : this.categories.entrySet()) {
            for (Map.Entry<String, String> x : cm.getValue().map.entrySet()) {
                b.append(cm.getKey() + "." + x.getKey() + ": " + x.getValue() + "\n");
            }
        }
        return b.toString();
    }
}
