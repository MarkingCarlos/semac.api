package com.semac.java_api.model;

import com.semac.java_api.model.enums.NivelPatrocinio;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/* Valor de cada nível de patrocínio (editável a cada edição). Um registro
   por nível (nivel único). Referenciada por patrocinador.cota_id. */
@Entity
@Table(name = "cota")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Cota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private NivelPatrocinio nivel;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
}
