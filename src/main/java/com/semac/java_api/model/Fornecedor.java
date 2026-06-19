package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

/* Cadastro central de fornecedores. Referenciado pelas compras
   (FK fornecedor_id) — sem campos monetários. */
@Entity
@Table(name = "fornecedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Fornecedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    private String contato;

    private String observacao;
}
