package jettyprism;

import spinat.jettyprism.Configuration;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rav
 */
public class TestJConfig {
    
    public TestJConfig() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
     public void test0() throws Exception {
        System.out.println(Configuration.load("C:\\Users\\rav\\Documents\\JAVA\\JettyPrism\\prism.xconf.original"));
    }
    // @Test
    // public void hello() {}
    
    @Test
    public void test1() throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("<?xml version='1.0' encoding='ISO-8859-1'?>\n");
        b.append("<properties>");
        b.append("</properties>");
        Configuration c = Configuration.loadFromString(b.toString());
        System.out.println(c);
    }
    
    @Test
    public void test2() throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("<?xml version='1.0' encoding='ISO-8859-1'?>\n");
        b.append("<properties>");
        b.append("<variables>");
        b.append("</variables>");
        b.append("</properties>");
        Configuration c = Configuration.loadFromString(b.toString());
        System.out.println(c);
    }
    
    @Test
    public void test3() throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("<?xml version='1.0' encoding='ISO-8859-1'?>\n");
        b.append("<!-- bla -->");
        b.append("<properties>");
        b.append("<!-- bla -->");
        b.append("<variables>");
        b.append("<!-- bla1 -->");
        b.append("</variables>");
        b.append("<!-- bla1 -->");
        b.append("<!-- bla2 -->");
        b.append("</properties>");
        Configuration c = Configuration.loadFromString(b.toString());
        System.out.println(c);
    }
    
    @Test
    public void test4() throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("<?xml version='1.0' encoding='ISO-8859-1'?>\n");
        b.append("<!-- bla -->");
        b.append("<properties>");
        b.append("<!-- bla -->");
        b.append("<variables>");
        b.append("<variable name=\"x\" value=\"y\"/>");
        b.append("<!-- bla1 -->");
        b.append("<variable name=\"x\" value=\"y\">");
        b.append("</variable>");
        b.append("</variables>");
        b.append("<!-- bla1 -->");
        b.append("<category name=\"x\">");
        b.append("</category>");
        b.append("<!-- bla2 -->");
        b.append("</properties>");
        Configuration c = Configuration.loadFromString(b.toString());
        System.out.println(c);
    }
    
    public void test5() throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("<?xml version='1.0' encoding='ISO-8859-1'?>\n");
        b.append("<properties>");
        b.append("<category name=\"x\">");
        b.append("<property name=\"a\" value=\"bx\"/>");
        b.append("</category>");
        b.append("<category name=\"y\">");
        b.append("<property name=\"a\" value=\"by\"/>");
        
        b.append("<category name=\"general\">");
        b.append("<property name=\"c\" value=\"cx\"/>");
        b.append("</category>");       
        b.append("</properties>");
        
        Configuration c = Configuration.loadFromString(b.toString());
        assertEquals( c.getProperty("c"),"cx");
        assertEquals( c.getProperty("cx","4"),"4");
        assertEquals(c.getProperty("a",null,"x"),"bx");
        assertEquals(c.getProperty("a",null,"y"),"by");
        
        assertEquals(c.getProperty("nichtda",null,"x"),"cx");
        
        assertEquals(c.getProperty("nichtda","default","x"),"default");
        
        assertTrue(c.getProperty("nix")==null);
        
        assertTrue(c.getProperty("nix","def").equals("def"));
           
        System.out.println(c);
    }
    
}