package test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Scanner;


public class ManualTests extends Tests{


    @Test
    public void getInt() throws Exception{
        String fileName = "test/resources/manual/getInt.pins";

        captureOut();

        this.intInputs = new LinkedList<>();
        intInputs.add(33);
        intInputs.add(66);
        run(fileName);

        String output = getOut();

        String expected = pre
                + "33\n" + "66\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void getString() throws Exception{
        String fileName = "test/resources/manual/getString.pins";

        captureOut();

        this.stringInputs = new LinkedList<>();
        stringInputs.add("Banana in krema");
        stringInputs.add("Zmaj in grozdje");
        run(fileName);

        String output = getOut();

        String expected = pre
                + "Banana in krema\n" + "Zmaj in grozdje\n";

        Assertions.assertEquals(expected,output);
    }
}
