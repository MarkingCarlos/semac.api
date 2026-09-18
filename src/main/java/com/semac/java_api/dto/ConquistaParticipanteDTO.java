package com.semac.java_api.dto;

import java.time.LocalDateTime;

/* Uma conquista como o participante a vê, em /participantes.

   Só conquistas ativas chegam aqui. `desbloqueada` decide se o card sai
   colorido ou em preto e branco; nome, descrição e pontos aparecem nos
   dois casos — a descrição bloqueada é justamente a meta a perseguir.

   `codigo` não é exposto: é chave interna e não diz nada ao participante.
   `obtidaEm` vem null enquanto a conquista não foi conquistada.

   `celebrar` é true só nas que foram conquistadas e ainda não tiveram a
   animação exibida — é a fila que a área do participante percorre ao
   abrir. Vira false assim que o front confirma a exibição
   (POST /api/conquista/{id}/vista).
   `imagemVersao` é o nome do arquivo (null se não houver) e serve de
   cache-buster na URL da imagem — ver ConquistaResponseDTO. */
public record ConquistaParticipanteDTO(
        Integer id,
        String nome,
        String descricao,
        Integer pontosBase,
        Integer raridade,
        Integer ordem,
        String imagemVersao,
        Boolean desbloqueada,
        LocalDateTime obtidaEm,
        Boolean celebrar
) {}
