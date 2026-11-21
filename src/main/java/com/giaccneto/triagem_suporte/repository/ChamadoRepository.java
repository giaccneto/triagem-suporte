package com.giaccneto.triagem_suporte.repository;

import com.giaccneto.triagem_suporte.model.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

   Optional<Chamado> findByTicketId(String id);

}
