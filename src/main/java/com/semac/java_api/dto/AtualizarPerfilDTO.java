package com.semac.java_api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/* Campos que o usuário pode alterar no próprio perfil (seção Início):
   RA (opcional) e a lista de camisetas (edição só — modelo/tamanho de
   cada uma). `camisetas` precisa trazer exatamente os mesmos `id`s que a
   pessoa já tem (nem a mais, nem a menos): essa rota nunca cria nem
   apaga camiseta, e nunca altera `avulsa` — isso é exclusivo do editor
   do admin (PUT /api/pessoa/{id}/camisetas). Lista vazia é válida para
   quem não tem nenhuma camiseta registrada. */
public record AtualizarPerfilDTO(
        String ra,
        @NotNull(message = "Informe as camisetas.") @Valid List<AtualizarCamisetaPerfilDTO> camisetas
) {}
