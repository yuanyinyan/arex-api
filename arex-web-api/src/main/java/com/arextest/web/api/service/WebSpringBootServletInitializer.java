package com.arextest.web.api.service;

import com.arextest.common.metrics.PrometheusConfiguration;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@EnableAsync
// @EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
@SpringBootApplication(scanBasePackages = "com.arextest.web")
public class WebSpringBootServletInitializer extends SpringBootServletInitializer {

  @Value("${arex.prometheus.port}")
  String prometheusPort;

  public static void main(String[] args) {
    SpringApplication.run(WebSpringBootServletInitializer.class, args);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(WebSpringBootServletInitializer.class);
  }

  @PostConstruct
  public void init() {
    PrometheusConfiguration.initMetrics(prometheusPort);
  }
}
