package db712.server;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.io.Serializable;

/**
 * 为了同步修改日志记录的时间
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2022-08-31 21:11
 **/
public class LogbackResetTimeFilter extends Filter<LoggingEvent> implements Serializable {

    public static boolean out = false;

    /**
     * <!-- 过滤重设时间，一般是当调试服务器或者调试活动的时候手动改时间需要触发 -->
     * <filter class="logback.LogbackResetTimeFilter">
     * <onMatch>ACCEPT</onMatch>
     * <onMismatch>DENY</onMismatch>
     * </filter>
     */
    @Override public FilterReply decide(LoggingEvent event) {
        if (out) {
            System.out.println(event.getFormattedMessage());
            IThrowableProxy ithrowableProxy = event.getThrowableProxy();
            if (ithrowableProxy != null) {
                ThrowableProxy throwableProxy = (ThrowableProxy) ithrowableProxy;
                Throwable throwable = throwableProxy.getThrowable();
                throwable.printStackTrace(System.out);
            }
        }
        return FilterReply.NEUTRAL;
    }

}
