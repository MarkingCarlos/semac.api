package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;

/* Abrir ou fechar o quiz de um laboratório no /admin
   (PATCH /api/laboratorio/laboratorios/<id>/quiz-aberto). */
public record QuizAbertoRequestDTO(@NotNull Boolean aberto) {}
