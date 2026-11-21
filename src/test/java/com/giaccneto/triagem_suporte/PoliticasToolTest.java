
package com.giaccneto.triagem_suporte;

import com.giaccneto.triagem_suporte.tool.PoliticasTool;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PoliticasToolTest {

    @Test
    void deveLerPoliticasDeAtendimento() throws IOException {

        // Preparação
        PoliticasTool ferramenta = new PoliticasTool();

        // Execução
        String resultado = ferramenta.consultarPoliticas();

        // Verificação
        assertTrue(resultado.contains("COBRANCA"));
    }
}
