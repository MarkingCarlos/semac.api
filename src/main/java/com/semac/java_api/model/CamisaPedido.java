package com.semac.java_api.model;

import com.semac.java_api.model.enums.Modelo;
import com.semac.java_api.model.enums.Tamanho;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "camisa_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class CamisaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tamanho tamanho;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modelo modelo;
}