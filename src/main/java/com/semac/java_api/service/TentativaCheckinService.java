package com.semac.java_api.service;

import com.semac.java_api.dto.OperadorCheckinDTO;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.TentativaCheckinBloqueada;
import com.semac.java_api.repository.TentativaCheckinBloqueadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/* Log das tentativas de check-in recusadas por estarem fora da janela
   (ver InscricaoEventoService.marcarPresente).

   Bean separado do InscricaoEventoService de propósito, e não por
   organização: `registrar` precisa de REQUIRES_NEW, e a propagação só
   vale quando a chamada passa pelo proxy do Spring — método privado do
   mesmo bean seria ignorado em silêncio.

   O motivo da transação nova: quem chama isto lança um 409 logo em
   seguida, e a exceção faz rollback da transação do check-in. Gravado
   junto, o log iria embora com ela — o log sumiria exatamente nos casos
   que ele existe para registrar. */
@Service
public class TentativaCheckinService {

    private final TentativaCheckinBloqueadaRepository tentativaCheckinBloqueadaRepository;

    public TentativaCheckinService(TentativaCheckinBloqueadaRepository tentativaCheckinBloqueadaRepository) {
        this.tentativaCheckinBloqueadaRepository = tentativaCheckinBloqueadaRepository;
    }

    /* `minutosAntes` é o quanto a tentativa aconteceu antes de a janela
       abrir, não antes do início do evento. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Evento evento,
                          Pessoa participante,
                          OperadorCheckinDTO operador,
                          LocalDateTime tentadoEm,
                          long minutosAntes) {
        TentativaCheckinBloqueada tentativa = new TentativaCheckinBloqueada();
        tentativa.setEventoId(evento.getId());
        tentativa.setEventoNome(evento.getNome());
        tentativa.setParticipanteId(participante.getId());
        tentativa.setParticipanteNome(participante.getNome());
        tentativa.setOperadorId(operador.id());
        tentativa.setOperadorNome(operador.nome());
        tentativa.setOperadorRole(operador.role());
        tentativa.setTentadoEm(tentadoEm);
        tentativa.setMinutosAntes(minutosAntes);
        tentativaCheckinBloqueadaRepository.save(tentativa);
    }
}
