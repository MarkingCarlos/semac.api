package com.semac.java_api.dto;

import java.util.List;

/* Alimenta a confirmação antes do disparo: o número é o que importa, a
   amostra serve para a pessoa reconhecer o público. */
public record DestinatariosResponseDTO(Integer total, List<String> amostra) {}
