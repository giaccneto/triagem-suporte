package com.giaccneto.triagem_suporte.controller;

import com.giaccneto.triagem_suporte.dto.ChamadoResponse;
import com.giaccneto.triagem_suporte.dto.CriarChamadoRequest;
import com.giaccneto.triagem_suporte.model.Chamado;
import com.giaccneto.triagem_suporte.exception.ChamadoNaoEncontradoException;
import com.giaccneto.triagem_suporte.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> criarChamado(@Valid @RequestBody CriarChamadoRequest entrada) {
        var chamado = new Chamado(entrada.ticketId(), entrada.customerId(), entrada.message());

        return ResponseEntity.status(CREATED).body(ChamadoResponse.de(chamadoService.processarChamado(chamado)));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<ChamadoResponse> buscarPorTicketId(@PathVariable String ticketId) {
        return chamadoService.buscarPorTicketId(ticketId)
                .map(ChamadoResponse::de)
                .map(ResponseEntity::ok)
                .orElseThrow(ChamadoNaoEncontradoException::new);
    }
}
