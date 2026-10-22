package com.semac.java_api.dto;

/* Resposta do envio de uma pergunta para o debate. `conquistaConcedida` =
   esta pergunta destravou a conquista PERGUNTA_DEBATE agora, e o modal do
   participante já pode comemorar. */
public record PerguntaDebateEnviadaDTO(boolean conquistaConcedida) {}
