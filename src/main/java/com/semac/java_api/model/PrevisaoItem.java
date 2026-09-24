package com.semac.java_api.model;

import com.semac.java_api.model.enums.EscalaPrevisao;
import com.semac.java_api.model.enums.StatusPrevisao;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/* Uma linha de gasto previsto — o elo entre a cotação (preço pesquisado)
   e a compra (dinheiro que já saiu).

   Diferente de Compra, NÃO há coluna valorTotal. O total depende da
   escala, e o fator de escala (nº de inscritos, de membros, de
   palestrantes) muda ao longo da organização: um total persistido
   nasceria desatualizado. Quem calcula é o PrevisaoService, na leitura. */
@Entity
@Table(name = "previsao_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PrevisaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String descricao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private PrevisaoCategoria categoria;

    /* Opcional: nem todo gasto previsto tem fornecedor definido. */
    @ManyToOne
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    /* Pode ser 0: item com preço já pesquisado mas quantidade ainda não
       decidida (os colecionáveis, por exemplo). */
    @Column(nullable = false)
    private Integer quantidade = 1;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal frete = BigDecimal.ZERO;

    /* Escalas por cabeça, somadas no fator. Vazio = valor fechado
       (fator 1). EAGER porque o resumo lê todos os itens fora de
       transação e sempre precisa delas. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "previsao_item_escala", joinColumns = @JoinColumn(name = "previsao_item_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "escala", nullable = false, length = 20)
    private Set<EscalaPrevisao> escalas = EnumSet.noneOf(EscalaPrevisao.class);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPrevisao status = StatusPrevisao.PREVISTO;

    @Column(name = "data_prevista")
    private LocalDate dataPrevista;

    @Column(length = 500)
    private String observacao;

    /* Preenchido quando a previsão vira compra real; a previsão passa a
       status PAGO e deixa de contar como valor ainda por gastar. */
    @ManyToOne
    @JoinColumn(name = "compra_id")
    private Compra compra;
}
