package test;

import org.junit.Test;

import java.util.regex.Pattern;

public class RegularExpressionTest {
    @Test
    public void distinguishVideoFile() {
        String[] fileNames = {"aaa.mp4","aaa.avi","aaa.jar"};
        for (String fileName : fileNames) {
            System.out.println(fileName + "::::::" + Pattern.matches(".+(\\.mp4|\\.webm|\\.wmv|\\.avi|\\.dat|\\.asf|\\.mpeg|\\.mpg|\\.rm|\\.rmvb|\\.ram|\\.flv|\\.3gp|\\.mov|\\.divx|\\.dv|\\.vob|\\.mkv|\\.qt|\\.cpk|\\.fli|\\.flc|\\.f4v|\\.m4v|\\.mod|\\.m2t|\\.swf|\\.mts|\\.m2ts|\\.3g2|\\.mpe|\\.ts|\\.div|\\.lavf|\\.dirac){1}", fileName));
        }
    }
}
