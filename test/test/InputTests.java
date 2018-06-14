package test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Scanner;


public class InputTests extends Tests{


    @Test
    public void getInt() throws Exception{
        String fileName = "test/resources/input/getInt.pins";

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
        String fileName = "test/resources/input/getString.pins";

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

    @Test
    public void getIntoArray() throws Exception{
        String fileName = "test/resources/input/getIntoArray.pins";

        captureOut();
        this.intInputs = new LinkedList<>();
        intInputs.add(55);

        this.stringInputs = new LinkedList<>();
        stringInputs.add("pet pet");
        run(fileName);

        String output = getOut();

        String expected = pre
                + "1\n" + "2\n" + "55\n" + "ena\n" + "dva\n" + "pet pet\n";

        Assertions.assertEquals(expected,output);
    }
}
