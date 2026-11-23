
package com.giaccneto.triagem_suporte;

import com.giaccneto.triagem_suporte.service.CatalogoPoliticas;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CatalogoPoliticasTest {

    @Test
    void deveLerPoliticasDeAtendimento() throws IOException {

        // Preparação
        CatalogoPoliticas catalogo = new CatalogoPoliticas();

        // Execução
        String resultado = catalogo.texto();

        // Verificação
        assertTrue(resultado.contains("COBRANCA"));
    }
}
