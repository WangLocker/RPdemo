package com.transitnet.rpdemo;

import com.transitnet.rpdemo.service.parse.parseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class prework implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(prework.class);

    @Value("${app.setupMode}")
    private String setupMode;

    private final DataSource dataSource;

    public prework(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Autowired
    parseService parseService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("根据启动参数执行初始化工作");

        if (setupMode.equals("start")) {
            logger.info("[START]执行数据库初始化");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("scripts/initDb.sql"));
            populator.populate(dataSource.getConnection());
            logger.info("[START]数据库初始化完成");
            logger.info("[START]开始解析OSM数据");
            parseService.parseData();

        } else if(setupMode.equals("clean")) {
            logger.info("[CLEAN]清理应用数据库与缓存");
            //待完成
        }
    }
}
