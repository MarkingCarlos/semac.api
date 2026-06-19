package com.semac.java_api.model;

import com.semac.java_api.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;

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

    @Enumerated(EnumType.STRING)
    private Role role;

    /* Ingresso escolhido na confirmação da inscrição (role = PARTICIPANTE).
       Null para pendentes e organizadores. */
    @ManyToOne
    @JoinColumn(name = "tipo_inscricao_id")
    private TipoInscricao tipoInscricao;

    @OneToMany(mappedBy = "participante")
    private List<EventoParticipante> eventoParticipantes = new ArrayList<>();

    @OneToMany(mappedBy = "participante")
    private List<ParticipanteConquista> participanteConquistas = new ArrayList<>();

    @OneToMany(mappedBy = "organizador")
    private List<Sorteio> sorteiosOrganizados = new ArrayList<>();

    @OneToMany(mappedBy = "participante")
    private List<GanhadoresSorteio> ganhadoresSorteio = new ArrayList<>();

    @OneToMany(mappedBy = "pessoa")
    private List<CamisaPedido> camisaPedidos = new ArrayList<>();
}
