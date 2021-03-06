import cosm0s.stats4was.utils.DaemonContext;
import cosm0s.stats4was.utils.UtilsFile;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class UtilsFileTest {

    @Test
    public void checkFileTest(){
        System.setProperty("config-path", "resources/conf/");

        String properties = "resources/conf/soap.properties";
        String stats4was = "resources/conf/stats4was.properties";

        int valorEntero = (int) Math.floor(Math.random()*(0-1000000+1)+1000000);
        String falseProperties = "C://props"+ valorEntero +".properties";

        assertTrue(UtilsFile.checkFile(properties));
        assertTrue(UtilsFile.checkFile(stats4was));
        assertFalse(UtilsFile.checkFile(falseProperties));

        List<String> testList = new LinkedList<String>();
        testList.add(properties);
        testList.add(stats4was);
        testList.add(falseProperties);

        assertFalse(UtilsFile.checkFile(testList));

        testList.remove(falseProperties);

        assertTrue(UtilsFile.checkFile(testList));


    }

    @Test(expected=NullPointerException.class)
    public void testPrintNull(){
        System.setProperty("config-path", "resources/conf/");
        UtilsFile.print("", null);
    }

}
