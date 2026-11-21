
import com.giaccneto.triagem_suporte.TriagemSuporteApplication;
import com.giaccneto.triagem_suporte.dto.ClassificacaoResposta;
import org.springframework.boot.test.context.SpringBootTest;
import com.giaccneto.triagem_suporte.service.ClassificacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(classes = TriagemSuporteApplication.class)
public class ClassificacaoServiceTest {


    @Autowired
    private ClassificacaoService classificacaoService;

    @Test
    void deveClassificarChamadoComIA() {

        String mensagem = "Fui cobrado duas vezes pela mesma compra.";

        ClassificacaoResposta resposta = classificacaoService.classificar(mensagem);

        System.out.println("Categoria: " + resposta.getCategoria());
        System.out.println("Prioridade: " + resposta.getPrioridade());
        System.out.println("Equipe: " + resposta.getEquipeDestino());
    }


}
