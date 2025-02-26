package wxdgaming.mariadb;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 配置文件
 *
 * @author: wxd-gaming(無心道, 15388152619)
 * @version: 2025-02-26 13:09
 **/
@Getter
@Setter
public class DbConfig {

    public static final File dbConfigFile = new File("db-config.yml");
    public static DbConfig ins = null;

    public static void loadYaml() {
        try {
            DumperOptions dumperOptions = new DumperOptions();
            Representer representer = new Representer(dumperOptions);
            representer.getPropertyUtils().setSkipMissingProperties(true);
            Yaml yaml = new Yaml(representer, dumperOptions);
            ins = yaml.loadAs(Files.newInputStream(dbConfigFile.toPath()), DbConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 把指定类型转换成 yaml 文件 */
    public void saveYaml() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Representer representer = new Representer(dumperOptions);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(representer, dumperOptions);
        String string = yaml.dumpAsMap(this);
        GraalvmUtil.writeFile(dbConfigFile, string);
    }

    private String serverTitle = "数据库引擎";
    private int port = 13306;
    private int webPort = 13301;
    private String user = "root";
    private String pwd = "root";
    private String dataBases = "local_game";
    private int autoBakSqlTimeM = 60;
    private int fontSize = 13;
    private String bgColor = null;
    private int showMaxLine = 1500;
    private boolean autoWarp = true;

    public String serverName() {
        if (StringUtils.isBlank(getServerTitle())) {
            return "数据库引擎";
        }
        return getServerTitle();
    }

}
