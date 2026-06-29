package com.duoc.ms_administracion_archivos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "aws.s3.bucket-name=test-bucket",
    "efs.mount-path=/tmp/efs-test",
    "AZURE_ISSUER_URI=https://guiasdespacho2.b2clogin.com/43c80967-57ec-45e0-a2e2-57d0859a95d1/v2.0/"
})


class MsAdministracionArchivosApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranque correctamente
    }
}
