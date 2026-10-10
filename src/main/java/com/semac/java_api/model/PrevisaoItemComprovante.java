package com.semac.java_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/* Comprovante de compra (nota, recibo, print do Pix) anexado a um item da
   previsão. O arquivo vive em disco; aqui fica só o nome gerado pelo
   servidor e o que a interface precisa para listar. */
@Entity
@Table(name = "previsao_item_comprovante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PrevisaoItemComprovante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "previsao_item_id", nullable = false)
    private PrevisaoItem previsaoItem;

    @Column(name = "nome_original", nullable = false)
    private String nomeOriginal;

    @Column(name = "nome_arquivo", nullable = false, unique = true)
    private String nomeArquivo;

    @Column(name = "tipo_conteudo", nullable = false, length = 50)
    private String tipoConteudo;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @Column(name = "enviado_em", nullable = false)
    private LocalDateTime enviadoEm;
}
