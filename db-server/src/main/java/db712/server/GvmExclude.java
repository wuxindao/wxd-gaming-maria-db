package db712.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * gvm打包的时候需要排除的部分
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2024-07-15 09:13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.TYPE,
        ElementType.CONSTRUCTOR
})
public @interface GvmExclude {
}
