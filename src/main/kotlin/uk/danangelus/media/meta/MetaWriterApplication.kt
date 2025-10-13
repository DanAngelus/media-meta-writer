package uk.danangelus.media.meta

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate

@EnableScheduling
@EnableConfigurationProperties
@ConfigurationPropertiesScan
@SpringBootApplication
class MetaWriterApplication {

    @Bean
    fun restTemplate() = RestTemplate()
}

fun main(args: Array<String>) {
	runApplication<MetaWriterApplication>(*args)
}
