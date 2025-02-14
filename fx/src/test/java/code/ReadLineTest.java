package code;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;

/**
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-13 17:26
 **/
public class ReadLineTest {

    @Test
    public void r1() throws Exception {
        String tmpString = "123\n456\n789";
        try (StringReader strReader = new StringReader(tmpString);
             BufferedReader bufferedReader = new BufferedReader(strReader);) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
