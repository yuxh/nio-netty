package bio;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author yuxiaohui@cmiot.chinamobile.com
 * @Date: 2020-10-09.
 * @Time: 12:55
 */
@Data
@AllArgsConstructor
public class Message implements Serializable {

    private String content;
}
