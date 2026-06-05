package com.duoc.ms_administracion_archivos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Configuración del cliente AWS S3 usando SDK v2.
 * Las credenciales se leen desde variables de entorno inyectadas por docker run:
 *   AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN, AWS_REGION
 */
@Configuration
public class StorageConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public S3Client s3Client() {
        String accessKeyId     = System.getenv("AWS_ACCESS_KEY_ID");
        String secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        String sessionToken    = System.getenv("AWS_SESSION_TOKEN");

        if (accessKeyId == null || secretAccessKey == null || sessionToken == null) {
            throw new IllegalStateException(
                "Las variables de entorno AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY " +
                "y AWS_SESSION_TOKEN deben estar configuradas."
            );
        }

        AwsSessionCredentials credentials = AwsSessionCredentials.create(
                accessKeyId,
                secretAccessKey,
                sessionToken
        );

        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
