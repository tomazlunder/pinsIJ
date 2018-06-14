package test;

import org.junit.jupiter.api.*;

public class SuccessTests extends Tests {

    @Test
    public void simple() throws Exception{
        String fileName = "test/resources/success/simple.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
            + "5\n" + "10\n" + "15\n" + "3\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void math() throws Exception{
        String fileName = "test/resources/success/math.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "30\n" + "10\n" + "200\n" + "2\n" + "3\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void functions() throws Exception{
        String fileName = "test/resources/success/functions.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "30\n" + "-10\n" + "99\n" + "76\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void functions2() throws Exception{
        String fileName = "test/resources/success/functions2.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "1\n" + "2\n" + "3\n" + "4\n" + "5\n" + "10\n" + "-100\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void loops() throws Exception{
        String fileName = "test/resources/success/loops.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "1\n" + "2\n" + "3\n" + "4\n" + "5\n" + "1\n" + "2\n" + "3\n" + "4\n" + "5\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void types() throws Exception{
        String fileName = "test/resources/success/types.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "10\n" + "20\n" + "30\n" + "40str\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void logical() throws Exception{
        String fileName = "test/resources/success/logical.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        Assertions.assertFalse(output.contains("0"));
    }

    @Test
    public void putInt() throws Exception{
        String fileName = "test/resources/success/putInt.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "3\n" + "6\n" + "9\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void putString() throws Exception{
        String fileName = "test/resources/success/putString.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "const!\n" + "global!\n" + "local!\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void arrays() throws Exception{
        String fileName = "test/resources/success/arrays.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "0\n" + "-100\n" + "-101\n"+ "-102\n" + "-103\n" + "1\n" + "-200\n" + "-201\n" + "-202\n" + "-203\n";

        Assertions.assertEquals(expected,output);
    }

    @Test
    public void arrays2() throws Exception{
        String fileName = "test/resources/success/arrays2.pins";
        captureOut();
        run(fileName);

        String output = getOut();
        String expected = pre
                + "-100\n" + "-104\n" + "-200\n"+ "-204\n" + "100\n" + "102\n" + "200\n" + "202\n";

        Assertions.assertEquals(expected,output);
    }

}
