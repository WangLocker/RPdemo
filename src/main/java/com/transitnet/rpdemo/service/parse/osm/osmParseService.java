package com.transitnet.rpdemo.service.parse.osm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class osmParseService {
    @Value("${rpdemo.osmfile}")
    private Path osmFile;

}
