
import com.giaccneto.triagem_suporte.TriagemSuporteApplication;
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

        String resposta = classificacaoService.classificar(mensagem);

        System.out.println("Resposta da IA: " + resposta);
    }


}
