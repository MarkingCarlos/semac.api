package com.semac.java_api.service;

import com.semac.java_api.dto.TelaoDebateResponseDTO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* Estado do telão de cada debate, para o painel poder ser controlado de
   outro aparelho (celular) além do notebook ligado ao telão. Antes tudo ia
   por BroadcastChannel, que só alcança guias do mesmo navegador; agora o
   painel publica aqui e o telão e os outros painéis consultam a cada ~1s.

   Fica só em memória, de propósito: é estado de palco, que vale durante
   o debate e não precisa sobreviver a nada. Se a API reiniciar no meio,
   o próximo painel que publicar repõe tudo (cada painel guarda a sua
   cópia no navegador).

   O conteúdo do estado é do frontend (tela, timer, pergunta, listas do
   mediador) e passa direto, sem o backend interpretar. O que importa
   aqui é a ordem: `alteradoEm` é o instante da mudança no relógio do
   servidor (cada aparelho corrige o seu pela hora devolvida em
   `servidorAgora`), e uma publicação mais antiga que a guardada é
   ignorada — dois aparelhos mexendo quase juntos, vale o último. */
@Service
public class TelaoDebateService {

    private record EstadoGuardado(Map<String, Object> estado, long alteradoEm) {}

    private final Map<Integer, EstadoGuardado> estadosPorDebate = new ConcurrentHashMap<>();
    private final Map<Integer, Long> telaoVistoEmPorDebate = new ConcurrentHashMap<>();

    /* `doTelao`: a consulta veio da guia projetada, o que conta como sinal
       de vida dela — é o que acende "Telão conectado" no celular. */
    public TelaoDebateResponseDTO buscar(Integer eventoId, boolean doTelao) {
        if (doTelao) telaoVistoEmPorDebate.put(eventoId, System.currentTimeMillis());
        return resposta(eventoId, estadosPorDebate.get(eventoId));
    }

    public TelaoDebateResponseDTO publicar(Integer eventoId, Map<String, Object> estado, long alteradoEm) {
        EstadoGuardado vigente = estadosPorDebate.compute(eventoId, (id, atual) ->
                atual != null && atual.alteradoEm() > alteradoEm ? atual : new EstadoGuardado(estado, alteradoEm));
        return resposta(eventoId, vigente);
    }

    private TelaoDebateResponseDTO resposta(Integer eventoId, EstadoGuardado guardado) {
        return new TelaoDebateResponseDTO(
                guardado == null ? null : guardado.estado(),
                guardado == null ? 0 : guardado.alteradoEm(),
                System.currentTimeMillis(),
                telaoVistoEmPorDebate.get(eventoId));
    }
}
