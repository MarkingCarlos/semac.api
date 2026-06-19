package com.semac.java_api.dto;

import com.semac.java_api.model.enums.Role;
import jakarta.validation.constraints.NotNull;

/* Confirmação de inscrição no /admin: atribui o papel da pessoa.
   Aceita PARTICIPANTE ou qualquer papel de comissão (MEMBRO, DIRETOR_*,
   PRESIDENTE). tipoInscricaoId é obrigatório quando role = PARTICIPANTE. */
public record AtribuirRoleDTO(@NotNull Role role, Integer tipoInscricaoId) {}
