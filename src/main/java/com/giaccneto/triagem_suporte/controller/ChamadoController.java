package com.giaccneto.triagem_suporte.controller;

import com.giaccneto.triagem_suporte.dto.CriarChamadoRequest;
import com.giaccneto.triagem_suporte.model.Chamado;
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
    public ResponseEntity<Chamado> criarChamado(@Valid @RequestBody CriarChamadoRequest entrada) {
        Chamado chamado = new Chamado();
        chamado.setTicketId(entrada.ticketId());
        chamado.setCustomerId(entrada.customerId());
        chamado.setMessage(entrada.message());

        return ResponseEntity.status(CREATED).body(chamadoService.processarChamado(chamado));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<Chamado> buscarPorTicketId(@PathVariable String ticketId) {
        return ResponseEntity.of(chamadoService.buscarPorTicketId(ticketId));
    }
}
