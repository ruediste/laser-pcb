package com.github.ruediste.laserPcb;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.github.ruediste.gerberLib.linAlg.CoordinatePoint;

@SpringBootApplication
public class LaserPcbApplication {

	public static void main(String[] args) {
		SpringApplication.run(LaserPcbApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**").allowedMethods("*");
			}
		};
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
		return builder -> builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
				.deserializerByType(CoordinatePoint.class, new JsonDeserializer<CoordinatePoint>() {

					@Override
					public CoordinatePoint deserialize(JsonParser p, DeserializationContext ctxt)
							throws IOException, JsonProcessingException {
						TreeNode tree = p.readValueAsTree();
						if (tree instanceof NullNode)
							return null;
						if (!tree.isObject())
							throw new RuntimeException("Expected start of object");
						NumericNode xNode = (NumericNode) tree.get("x");
						NumericNode yNode = (NumericNode) tree.get("y");
						return new CoordinatePoint(xNode.doubleValue(), yNode.doubleValue());
					}
				});
	}
}
