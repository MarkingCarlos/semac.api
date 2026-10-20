package com.semac.java_api.controller;

import com.semac.java_api.dto.CertificadoDTO;
import com.semac.java_api.repository.EventoPalestranteRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/* Lista dos certificados de palestrante: um por palestrante em cada evento
   que ele ministrou (palestra, minicurso, mesa redonda...). O PDF é gerado
   no navegador sobre o modelo em public/certificado (subaba Certificados
   do /admin).

   Restrita no SecurityConfig ao mesmo público da aba Pessoas; com o
   anyRequest().permitAll() de lá, sem a regra ela ficaria pública. */
@RestController
@RequestMapping("/api/certificado")
public class CertificadoController {

    private final EventoPalestranteRepository eventoPalestranteRepository;

    public CertificadoController(EventoPalestranteRepository eventoPalestranteRepository) {
        this.eventoPalestranteRepository = eventoPalestranteRepository;
    }

    @GetMapping
    public List<CertificadoDTO> listar() {
        return eventoPalestranteRepository.findAll().stream()
                .map(eventoPalestrante -> {
                    var evento = eventoPalestrante.getEvento();
                    var palestrante = eventoPalestrante.getPalestrante();
                    return new CertificadoDTO(
                            palestrante.getId(),
                            palestrante.getNome(),
                            evento.getId(),
                            evento.getNome(),
                            evento.getTipoEvento().getCodigo().name(),
                            evento.getDataHoraInicio(),
                            evento.getDataHoraFim());
                })
                .sorted(Comparator.comparing(CertificadoDTO::nomePalestrante, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CertificadoDTO::dataHoraInicio))
                .toList();
    }
}
