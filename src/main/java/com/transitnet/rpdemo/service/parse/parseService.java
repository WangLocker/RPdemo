package com.transitnet.rpdemo.service.parse;

import com.transitnet.rpdemo.service.parse.osm.osmParseService;
import de.topobyte.osm4j.core.access.OsmInputException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
@Service
public class parseService {
    @Autowired
    private osmParseService osmparseService;

    public void parseData() throws IOException, OsmInputException {
        osmparseService.parseData();
    }
}
