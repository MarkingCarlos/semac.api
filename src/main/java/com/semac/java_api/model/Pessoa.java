package com.semac.java_api.model;

import com.semac.java_api.model.enums.FormaPagamento;
import com.semac.java_api.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pessoa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false, unique = true)
    private String uuid;

    private String ra;

    @Column(nullable = false)
    private Boolean ativo = true;

    /* Preenchido em InscricaoService.cadastrar. Cadastros anteriores à
       coluna (V22) ficam null — não há como saber a data real deles. */
    @Column(name = "inscrito_em")
    private LocalDateTime inscritoEm;

    @Enumerated(EnumType.STRING)
    private Role role;

    /* Ingresso escolhido na confirmação da inscrição (role = PARTICIPANTE).
       Null para pendentes e organizadores. */
    @ManyToOne
    @JoinColumn(name = "tipo_inscricao_id")
    private TipoInscricao tipoInscricao;

    /* Nível e xp acumulado (role = PARTICIPANTE). Atribuídos na
       confirmação da inscrição (ver PessoaService.atribuirRole); o
       acúmulo contínuo de xp por conquistas/presenças é uma entrega
       futura. Null para pendentes e organizadores, mesmo ciclo de vida
       de tipoInscricao. */
    /* Diárias escolhidas no cadastro, quando o ingresso é cobrado por dia.
       Null para ingresso de valor fixo e para organizadores. */
    @Column(name = "dias_inscricao")
    private Integer diasInscricao;

    @ManyToOne
    @JoinColumn(name = "nivel_id")
    private Nivel nivel;

    private Integer xp;

    @OneToMany(mappedBy = "participante")
    private List<EventoParticipante> eventoParticipantes = new ArrayList<>();

    @OneToMany(mappedBy = "participante")
    private List<ParticipanteConquista> participanteConquistas = new ArrayList<>();

    @OneToMany(mappedBy = "organizador")
    private List<Sorteio> sorteiosOrganizados = new ArrayList<>();

    @OneToMany(mappedBy = "participante")
    private List<GanhadoresSorteio> ganhadoresSorteio = new ArrayList<>();

    @Column(name = "comprovante_pagamento")
    private String comprovantePagamento;

    /* Pagamento por cartão (Mercado Pago) — alternativa ao Pix acima.
       Preenchidos só quando formaPagamento = CARTAO; ver PagamentoCartaoService.
       mpStatus guarda o valor cru devolvido pela Mercado Pago (approved,
       rejected, in_process...), não um enum próprio: esse vocabulário é
       da Mercado Pago e pode mudar. */
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento")
    private FormaPagamento formaPagamento;

    @Column(name = "mp_payment_id")
    private Long mpPaymentId;

    @Column(name = "mp_status")
    private String mpStatus;

    @Column(name = "mp_status_detail")
    private String mpStatusDetail;

    private Integer parcelas;

    @Column(name = "valor_cobrado", precision = 10, scale = 2)
    private BigDecimal valorCobrado;

    /* Quantas vezes o cartão foi cobrado para esta inscrição (aprovado,
       recusado ou em análise) — ver PagamentoCartaoService.
       LIMITE_TENTATIVAS_CARTAO trava novas tentativas acima disso. */
    @Column(name = "tentativas_cartao", nullable = false)
    private Integer tentativasCartao = 0;

    @OneToMany(mappedBy = "pessoa")
    private List<CamisaPedido> camisaPedidos = new ArrayList<>();
}
