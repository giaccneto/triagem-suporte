import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.model.StatusChamado;
import com.giaccneto.triagem_suporte.repository.ChamadoRepository;

import com.giaccneto.triagem_suporte.service.ChamadoService;
import com.giaccneto.triagem_suporte.service.ClassificacaoService;
import com.giaccneto.triagem_suporte.service.EncaminhamentoService;
import com.giaccneto.triagem_suporte.service.ValidacaoClassificacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private ClassificacaoService classificacaoService;

    @Mock
    private ValidacaoClassificacaoService validacaoClassificacaoService;

    @Mock
    private EncaminhamentoService encaminhamentoService;

    @InjectMocks
    private ChamadoService chamadoService;

    private Chamado criarChamadoTeste(String ticketId) {

        Chamado chamado = new Chamado();

        chamado.setTicketId(ticketId);
        chamado.setCustomerId("CLIENTE-TESTE");
        chamado.setMessage(
                "Preciso da segunda via da minha fatura."
        );

        return chamado;
    }

    private ClassificacaoResposta criarClassificacaoValida() {

        ClassificacaoResposta resposta = new ClassificacaoResposta();

        resposta.setCategoria("COBRANCA");
        resposta.setPrioridade("NORMAL");
        resposta.setEquipeDestino("FINANCEIRO");
        resposta.setRevisaoNecessaria(false);
        resposta.setJustificativa(
                "Solicitação relacionada a cobrança."
        );

        return resposta;
    }

    private void simularRepositorio(String ticketId) {

        when(chamadoRepository.findByTicketId(ticketId))
                .thenReturn(Optional.empty());

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveRegistrarFalhaDeEncaminhamento() {

        Chamado chamado = criarChamadoTeste("TESTE-FALHA-001");
        ClassificacaoResposta resposta = criarClassificacaoValida();

        simularRepositorio(chamado.getTicketId());

        when(classificacaoService.classificar(chamado.getMessage()))
                .thenReturn(resposta);

        when(validacaoClassificacaoService.validar(resposta))
                .thenReturn(true);

        when(encaminhamentoService.encaminhar(any(Chamado.class)))
                .thenThrow(new RuntimeException("Trello indisponível"));

        Chamado resultado = chamadoService.processarChamado(chamado);

        assertEquals(
                StatusChamado.FALHA_ENCAMINHAMENTO,
                resultado.getStatus()
        );

        assertNull(resultado.getIdExterno());
    }

    @Test
    void deveManterChamadoEmRevisaoHumana() {

        Chamado chamado = criarChamadoTeste("TESTE-REVISAO-001");

        ClassificacaoResposta resposta = criarClassificacaoValida();

        resposta.setPrioridade(null);
        resposta.setEquipeDestino("REVISAO_HUMANA");
        resposta.setRevisaoNecessaria(true);

        simularRepositorio(chamado.getTicketId());

        when(classificacaoService.classificar(chamado.getMessage()))
                .thenReturn(resposta);

        when(validacaoClassificacaoService.validar(resposta))
                .thenReturn(true);

        Chamado resultado = chamadoService.processarChamado(chamado);

        assertEquals(
                StatusChamado.REVISAO_PENDENTE,
                resultado.getStatus()
        );

        assertNull(resultado.getIdExterno());

        verify(encaminhamentoService, never())
                .encaminhar(any(Chamado.class));
    }

    @Test
    void deveEncaminharChamadoComSucesso() {

        Chamado chamado = criarChamadoTeste("TESTE-SUCESSO-001");
        ClassificacaoResposta resposta = criarClassificacaoValida();

        simularRepositorio(chamado.getTicketId());

        when(classificacaoService.classificar(chamado.getMessage()))
                .thenReturn(resposta);

        when(validacaoClassificacaoService.validar(resposta))
                .thenReturn(true);

        when(encaminhamentoService.encaminhar(any(Chamado.class)))
                .thenReturn("CARTAO-TRELLO-123");

        Chamado resultado = chamadoService.processarChamado(chamado);

        assertEquals(
                StatusChamado.ENCAMINHADO,
                resultado.getStatus()
        );

        assertEquals(
                "CARTAO-TRELLO-123",
                resultado.getIdExterno()
        );

        verify(encaminhamentoService, times(1))
                .encaminhar(any(Chamado.class));
    }

    @Test
    void deveRejeitarClassificacaoInvalida() {

        Chamado chamado = criarChamadoTeste("TESTE-INVALIDO-001");
        ClassificacaoResposta resposta = criarClassificacaoValida();

        simularRepositorio(chamado.getTicketId());

        when(classificacaoService.classificar(chamado.getMessage()))
                .thenReturn(resposta);

        when(validacaoClassificacaoService.validar(resposta))
                .thenReturn(false);

        Chamado resultado = chamadoService.processarChamado(chamado);

        assertEquals(
                StatusChamado.REVISAO_PENDENTE,
                resultado.getStatus()
        );

        verify(encaminhamentoService, never())
                .encaminhar(any(Chamado.class));
    }

    @Test
    void deveRegistrarFalhaNaClassificacaoDaIA() {

        Chamado chamado = criarChamadoTeste("TESTE-IA-FALHA-001");

        simularRepositorio(chamado.getTicketId());

        when(classificacaoService.classificar(chamado.getMessage()))
                .thenThrow(new RuntimeException("Ollama indisponível"));

        Chamado resultado = chamadoService.processarChamado(chamado);

        assertEquals(
                StatusChamado.REVISAO_PENDENTE,
                resultado.getStatus()
        );

        verify(encaminhamentoService, never())
                .encaminhar(any(Chamado.class));
    }
}
