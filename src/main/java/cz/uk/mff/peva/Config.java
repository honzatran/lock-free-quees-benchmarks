package cz.uk.mff.peva;


import com.google.common.base.Splitter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Created by honza on 17/06/2017.
 */
public class Config {
    private static final Splitter MULTI_OPTION_SPLITTER = Splitter.on(',')
            .omitEmptyStrings()
            .trimResults();

    public static final String CONFIG = "application.properties";


    private final Properties properties = new Properties();

    public Config() throws IOException {
        InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(CONFIG);

        if (inputStream == null) {
            throw new RuntimeException(CONFIG + " not found on classpath");
        }

        this.properties.load(inputStream);
    }

    public int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public boolean getBool(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    public void updateString(String key, String newValue) {
        containKeyGuard(key);
        properties.setProperty(key, newValue);
    }

    public String getString(String key) {
        return properties.getProperty(key);
    }

    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    public List<String> getStrings(String key) {
        String multiOption = properties.getProperty(key);
        return MULTI_OPTION_SPLITTER.splitToList(multiOption);
    }

    public List<Integer> getIntegers(String key) {
        return getStrings(key)
                .stream()
                .mapToInt(Integer::parseInt)
                .collect(ArrayList::new, List::add, List::addAll);
    }

    private void containKeyGuard(String key) {
        if (!properties.containsKey(key)) {
            throw  new RuntimeException("Key " + key + " not present in the config");
        }
    }

}
