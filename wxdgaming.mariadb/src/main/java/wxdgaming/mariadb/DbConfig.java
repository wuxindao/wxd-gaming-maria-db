package wxdgaming.mariadb;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
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
            if (dbConfigFile.exists()) {
                DumperOptions dumperOptions = new DumperOptions();
                Representer representer = new Representer(dumperOptions);
                representer.getPropertyUtils().setSkipMissingProperties(true);
                Yaml yaml = new Yaml(representer, dumperOptions);
                ins = yaml.loadAs(Files.newInputStream(dbConfigFile.toPath()), DbConfig.class);
            } else {
                ins = new DbConfig();
                ins.save();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private String serverTitle = "数据库引擎";
    private int port = 13306;
    private int webPort = 13301;
    private String user = "root";
    private String pwd = "root";
    private String dataBases = "local_game";
    private int autoBakSqlTimeM = 60;

    public String serverName() {
        if (StringUtils.isBlank(getServerTitle())) {
            return "数据库引擎";
        }
        return getServerTitle();
    }

    /** 把指定类型转换成 yaml 文件 */
    public void save() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Representer representer = new Representer(dumperOptions);
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(representer, dumperOptions);
        String string = yaml.dumpAsMap(this);
        GraalvmUtil.writeFile(dbConfigFile, string);
    }
}
