package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/* Publicação do painel do debate: o estado inteiro do telão e o instante
   da mudança, já no relógio do servidor (ver TelaoDebateService). */
public record TelaoDebateRequestDTO(
        @NotNull Map<String, Object> estado,
        @NotNull Long alteradoEm
) {}
