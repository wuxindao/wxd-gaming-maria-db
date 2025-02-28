package code;

/**
 * 测试
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-27 09:05
 **/
public class TailFTest {

    public static void main(String[] args) throws Exception {
        TailFN tot = new TailFN("target/logs/db.log",4, (line) -> {
            System.out.println(line);
        });
        tot.join();
    }

}
