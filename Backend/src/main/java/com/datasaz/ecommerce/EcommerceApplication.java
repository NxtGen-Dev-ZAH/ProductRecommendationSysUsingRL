package com.datasaz.ecommerce;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableJpaAuditing
@Slf4j
@SpringBootApplication
@EnableRetry
@EnableCaching
@EnableScheduling
@EnableAsync
public class EcommerceApplication {
//    private static final Logger logger = LoggerFactory.getLogger(EcommerceApplication.class);

    public static void main(String[] args) {

//        logger.debug("Application started, initializing logging...");
//        log.debug("Application started with debug, initializing logging...");
//        System.out.println("*******************Working directory: " + System.getProperty("user.dir"));
        log.info("EcommerceApplication started at : {}", System.currentTimeMillis());
//        log.info("Working directory: {}", System.getProperty("user.dir"));
//        log.info("Java version: {}", System.getProperty("java.version"));
//        log.info("Java home: {}", System.getProperty("java.home"));
//        log.info("Java vendor: {}", System.getProperty("java.vendor"));
//        log.info("Java vendor url: {}", System.getProperty("java.vendor.url"));
//        log.info("Java class version: {}", System.getProperty("java.class.version"));
//        log.info("Java class path: {}", System.getProperty("java.class.path"));
//        log.info("Java library path: {}", System.getProperty("java.library.path"));
//        log.info("Java compiler: {}", System.getProperty("java.compiler"));
//        log.info("Java specification version: {}", System.getProperty("java.specification.version"));
//        log.info("Java specification vendor: {}", System.getProperty("java.specification.vendor"));
//        log.info("Java specification name: {}", System.getProperty("java.specification.name"));
//        log.info("Java vm specification version: {}", System.getProperty("java.vm.specification.version"));
//        log.info("Java vm specification vendor: {}", System.getProperty("java.vm.specification.vendor"));
//        log.info("Java vm specification name: {}", System.getProperty("java.vm.specification.name"));
//        log.info("Java vm version: {}", System.getProperty("java.vm.version"));
//        log.info("Java vm vendor: {}", System.getProperty("java.vm.vendor"));
//        log.info("Java vm name: {}", System.getProperty("java.vm.name"));
//        log.info("Java runtime version: {}", System.getProperty("java.runtime.version"));
//        log.info("Java runtime vendor: {}", System.getProperty("java.runtime.vendor"));
//        log.info("Java runtime name: {}", System.getProperty("java.runtime.name"));
//        log.info("Java home directory: {}", System.getProperty("java.home"));
//        log.info("Java io temporary directory: {}", System.getProperty("java.io.tmpdir"));
//        log.info("Java file encoding: {}", System.getProperty("file.encoding"));
//        log.info("Java file separator: {}", System.getProperty("file.separator"));
//        log.info("Java path separator: {}", System.getProperty("path.separator"));
//        log.info("Java line separator: {}", System.getProperty("line.separator"));
//        log.info("Java user name: {}", System.getProperty("user.name"));
//        log.info("Java user home: {}", System.getProperty("user.home"));
//        log.info("Java user dir: {}", System.getProperty("user.dir"));
//        log.info("Java user country: {}", System.getProperty("user.country"));
//        log.info("Java user language: {}", System.getProperty("user.language"));
//        log.info("Java user timezone: {}", System.getProperty("user.timezone"));
//        log.info("Java user variant: {}", System.getProperty("user.variant"));
        //log.info("Java user defined properties: {}", System.getProperties());

        SpringApplication.run(EcommerceApplication.class, args);
    }

//    @PostConstruct
//    public void init() {
//        logger.debug("Application started, initializing logging at {}", System.currentTimeMillis());
//        logger.debug("Working directory: {}", System.getProperty("user.dir"));
//    }

}
