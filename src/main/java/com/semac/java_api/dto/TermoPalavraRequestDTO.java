package com.semac.java_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record TermoPalavraRequestDTO(
        @NotNull Integer ano,
        @NotNull LocalDate data,
        /* Cinco letras A-Z. Acento e minúscula são normalizados no serviço;
           o que não se aceita é tamanho errado ou caractere estranho.
           Não se exige que esteja no dicionário: "SEMAC" e "CYBER" não
           estão em dicionário nenhum e são palavras legítimas aqui. */
        @NotBlank @Pattern(
                regexp = "^[A-Za-zÁÀÂÃÉÈÊÍÌÎÓÒÔÕÚÙÛÇáàâãéèêíìîóòôõúùûç]{5}$",
                message = "A palavra precisa ter exatamente 5 letras."
        ) String palavra
) {}
