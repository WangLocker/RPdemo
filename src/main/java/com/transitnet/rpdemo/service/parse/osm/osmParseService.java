package com.transitnet.rpdemo.service.parse.osm;

import de.topobyte.osm4j.core.access.OsmInputException;
import de.topobyte.osm4j.core.access.OsmReader;
import de.topobyte.osm4j.xml.dynsax.OsmXmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

@Service
public class osmParseService {
    @Value("${rpdemo.osmfile}")
    private String osmFile;
    @Autowired
    osmDatabaseHandler databaseHandler;
    @Autowired
    osmRoadHandler roadHandler;


    private static final Logger LOGGER = LoggerFactory.getLogger(osmParseService.class);

    public void parseData() throws OsmInputException, IOException {
        final Path osmfile = Path.of(osmFile);
        final BufferedInputStream bufferedInput = new BufferedInputStream(Files.newInputStream(osmfile));
        Collection<IOsmFileHandler> interestedHandler = new java.util.ArrayList<>();
        interestedHandler.add(databaseHandler);
        interestedHandler.add(roadHandler);
        try (InputStream input = bufferedInput) {
            // Ignore file
            if (input == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("File type is not supported, skipping: {}", osmFile);
                }
                return;
            }
            final osmHandlerForwarder forwarder = new osmHandlerForwarder(interestedHandler);
            final OsmReader reader = new OsmXmlReader(input, false);
            reader.setHandler(forwarder);
            reader.read();
        }
    }

}
