package guru.qas.martini.standalone.jcommander;

import com.beust.jcommander.converters.IParameterSplitter;
import java.util.Arrays;
import java.util.List;

public class NoSplitter implements IParameterSplitter {

    @Override
    public List<String> split(String value) {
        return Arrays.asList(value);
    }

}
