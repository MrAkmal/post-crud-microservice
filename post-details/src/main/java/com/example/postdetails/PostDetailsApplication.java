package com.example.postdetails;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import lombok.*;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootApplication
@EnableEurekaClient
@OpenAPIDefinition
@RequiredArgsConstructor
public class PostDetailsApplication {

    private final ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(PostDetailsApplication.class, args);
    }

    @Value("${spring.logstash.url:localhost:5044}")
    private String url;

    @Value("${spring.application.name}")
    private String name;



    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    @Bean
    public LogstashTcpSocketAppender logstashAppender() throws JsonProcessingException {
        Map<String, Object> customFields = new HashMap<>();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        LogstashTcpSocketAppender logstashTcpSocketAppender = new LogstashTcpSocketAppender();
        logstashTcpSocketAppender.setName("LOGSTASH_APPENDER");
        logstashTcpSocketAppender.setContext(loggerContext);
        logstashTcpSocketAppender.addDestination(url);
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(loggerContext);
        encoder.setIncludeContext(true);

        customFields.put("serviceName", name);
        encoder.setCustomFields(objectMapper.writeValueAsString(customFields));
        encoder.start();
        logstashTcpSocketAppender.setEncoder(encoder);
        logstashTcpSocketAppender.start();
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(logstashTcpSocketAppender);
        return logstashTcpSocketAppender;
    }


}


@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
class PostDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String body;
    private Long postId;
}

interface PostDetailsRepository extends JpaRepository<PostDetails, Long> {
    Optional<PostDetails> findPostDetailsByPostId(Long id);
}


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post-details")
class PostController {

    private final RestTemplate restTemplate;
    private final PostDetailsRepository postRepository;

    @PostMapping
    public PostDetails create(@RequestBody PostDetails postDetails) {
        return postRepository.save(postDetails);
    }

    @GetMapping
    public List<PostDetails> getAll() {
        return postRepository.findAll();
    }

    @GetMapping("/{id}")
    public PostDetails get(@PathVariable Long id) {
        return postRepository.findPostDetailsByPostId(id).orElseThrow(() -> {
            throw new RuntimeException("Post Details Not found");
        });
    }

}

