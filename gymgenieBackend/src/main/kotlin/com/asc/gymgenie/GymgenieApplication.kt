package com.asc.gymgenie

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class GymgenieApplication

fun main(args: Array<String>) {
	runApplication<GymgenieApplication>(*args)
}
