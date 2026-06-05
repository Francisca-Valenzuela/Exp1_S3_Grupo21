package com.duoc.ms_administracion_archivos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "aws.s3.bucket-name=test-bucket",
    "efs.mount-path=/tmp/efs-test"
})
class MsAdministracionArchivosApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranque correctamente
    }
}
