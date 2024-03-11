package com.transitnet.rpdemo.service.parse.osm;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class osmFilter implements IOsmFilter {

    private static final String COMMENT_PREFIX = "#";

    private static final String DROP_MODE = "--DROP";

    private static final String KEEP_MODE = "--KEEP";

    private static final Logger LOGGER = LoggerFactory.getLogger(osmFilter.class);

    private static final String TAG_VALUE_SEPARATOR = "=";

    private static boolean isTagInMap(final OsmTag tag, final Map<String, Set<String>> tagToValues) {
        final Set<String> values = tagToValues.get(tag.getKey());
        if (values == null) {
            return false;
        }
        return values.contains(tag.getValue());
    }

    @Value("${rpdemo.osmfilter.cfg}")
    private Path filterConfig;


    private final Map<String, Set<String>> mTagToValuesDrop;


    private final Map<String, Set<String>> mTagToValuesKeep;

    public osmFilter()  {
        mTagToValuesKeep = new HashMap<>();
        mTagToValuesDrop = new HashMap<>();
        initialize();
    }

    @Override
    public boolean filter(final OsmNode node) {
        // Never accept, we read nodes from ways instead since spatial data is not
        // needed
        return false;
    }

    @Override
    public boolean filter(final OsmRelation relation) {
        // Never accept, we are only interested in ways
        return false;
    }

    @Override
    public boolean filter(final OsmWay way) {
        // Iterate tags
        boolean hasOneKeepTag = false;
        for (int i = 0; i < way.getNumberOfTags(); i++) {
            final OsmTag tag = way.getTag(i);
            // Reject way if it contains a single drop tag
            if (isDropTag(tag)) {
                return false;
            }
            // Register the first keep tag
            if (!hasOneKeepTag && isKeepTag(tag)) {
                hasOneKeepTag = true;
            }
        }
        // Accept only if at least one keep tag was found
        return hasOneKeepTag;
    }

    private void initialize() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Initializing OSM road filter");
        }
        final Path filter = filterConfig;
        try (BufferedReader br = Files.newBufferedReader(filter)) {
            boolean keepMode = true;
            while (true) {
                final String line = br.readLine();
                if (line == null) {
                    break;
                }
                // Line is empty or comment
                if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                    continue;
                }
                // Line is mode changer
                if (line.equals(KEEP_MODE)) {
                    keepMode = true;
                    continue;
                } else if (line.equals(DROP_MODE)) {
                    keepMode = false;
                    continue;
                }

                // Line is regular tag-entry
                final String[] data = line.split(TAG_VALUE_SEPARATOR, 2);
                final String tag = data[0];
                final String value = data[1];
                // Get the correct map to insert into
                Map<String, Set<String>> tagToValues;
                if (keepMode) {
                    tagToValues = mTagToValuesKeep;
                } else {
                    tagToValues = mTagToValuesDrop;
                }
                tagToValues.computeIfAbsent(tag, k -> new HashSet<>()).add(value);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Error while reading filter configuration", e);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("OSM road filter data keep: {}", mTagToValuesKeep);
            LOGGER.debug("OSM road filter data drop: {}", mTagToValuesDrop);
        }
    }


    private boolean isDropTag(final OsmTag tag) {
        return osmFilter.isTagInMap(tag, mTagToValuesDrop);
    }


    private boolean isKeepTag(final OsmTag tag) {
        return osmFilter.isTagInMap(tag, mTagToValuesKeep);
    }

}
