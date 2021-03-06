package io.github.thisisnozaku.charactercreator.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.lang.String.format;

/**
 * Created by Damie on 1/30/2017.
 */
@Configuration
public class AWSConfig {
    @Value("${aws.region}")
    private String region;

    @Bean
    public AmazonS3 amazonS3() {
        AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard();
        clientBuilder.setRegion(getRegion().getName());
        return clientBuilder.build();
    }

    @Bean
    public AmazonSQS amazonSQS() {
        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();

        String serviceEndpoint = format("sqs.%s.amazonaws.com", getRegion().getName());

        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(serviceEndpoint,
                getRegion().getName());
        builder.setEndpointConfiguration(endpointConfiguration);
        return builder.build();
    }

    private Region getRegion() {
        return Region.getRegion(Regions.fromName(region));
    }
}
