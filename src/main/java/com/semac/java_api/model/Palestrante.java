package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "palestrante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Palestrante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 2000)
    private String descricao;

    /* Cargo ou instituição, curto: é o que aparece embaixo do nome no
       telão do debate (a descrição é a minibiografia, longa demais). */
    @Column(length = 150)
    private String cargo;

    /* Só o nome do arquivo em app.upload.dir.palestrantes — ver
       PalestranteController. Null enquanto ninguém enviar foto. */
    @Column(name = "foto_arquivo")
    private String fotoArquivo;

    /* Enquadramento da foto na moldura do telão: ponto da imagem que fica
       no centro (em %, 50/50 = centro) e zoom (1 = cobrindo a moldura).
       Trocar a foto volta para o padrão — ver PalestranteFotoService. */
    @Column(name = "foto_pos_x", nullable = false, precision = 5, scale = 2)
    private BigDecimal fotoPosX = ENQUADRAMENTO_CENTRO;

    @Column(name = "foto_pos_y", nullable = false, precision = 5, scale = 2)
    private BigDecimal fotoPosY = ENQUADRAMENTO_CENTRO;

    @Column(name = "foto_zoom", nullable = false, precision = 4, scale = 2)
    private BigDecimal fotoZoom = BigDecimal.ONE;

    public static final BigDecimal ENQUADRAMENTO_CENTRO = BigDecimal.valueOf(50);

    @OneToMany(mappedBy = "palestrante")
    private List<EventoPalestrante> eventoPalestrantes = new ArrayList<>();
}
