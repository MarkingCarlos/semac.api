package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/* Palavras novas para um dia — várias de uma vez, que é como a diretoria
   as cadastra (uma lista colada no /admin). */
public record TermoPalavraRequestDTO(
        @NotNull Integer ano,
        /* De 5 a 10 letras A-Z cada. Acento e minúscula são normalizados no
           serviço; o que não se aceita é tamanho errado ou caractere
           estranho. Não se exige que esteja no dicionário: "SEMAC" e
           "CYBER" não estão em dicionário nenhum e são palavras legítimas
           aqui. */
        @NotEmpty List<@NotBlank @Pattern(
                regexp = "^[A-Za-zÁÀÂÃÉÈÊÍÌÎÓÒÔÕÚÙÛÇáàâãéèêíìîóòôõúùûç]{5,10}$",
                message = "Cada palavra precisa ter de 5 a 10 letras."
        ) String> palavras
) {}
