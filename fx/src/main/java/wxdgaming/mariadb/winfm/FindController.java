package wxdgaming.mariadb.winfm;

import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class FindController {

    public TextField txt_find;
    public Label lab_result;
    protected Function<String, String> function;

    public void init(Function<String, String> function) {
        this.function = function;
    }

    public void btn_find(ActionEvent event) {
        String text = txt_find.getText();
        if (text.isEmpty()) {
            return;
        }
        String result = function.apply(text);
        lab_result.setText(result);
    }

}
