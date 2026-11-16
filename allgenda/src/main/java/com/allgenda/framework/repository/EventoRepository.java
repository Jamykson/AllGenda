package com.allgenda.framework.repository;

import com.allgenda.framework.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {
    List<Evento> findByGrupoIdOrderByDataAscIdAsc(UUID grupoId);
}
