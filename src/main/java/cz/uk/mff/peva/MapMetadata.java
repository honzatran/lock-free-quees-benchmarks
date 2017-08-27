package cz.uk.mff.peva;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by honza on 19/04/2017.
 */
public class MapMetadata implements IMetadata {
    private final Map<String, String> data;

    public MapMetadata() {
        this.data = new HashMap<>();
    }

    public void add(final String key, final String value) {
        data.put(key, value);
    }

    @Override
    public String getValue(String value) {
        return data.get(value);
    }
}
