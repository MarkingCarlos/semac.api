package com.semac.java_api.controller;

import com.semac.java_api.dto.MetaDoacaoRequestDTO;
import com.semac.java_api.dto.MetaDoacaoResponseDTO;
import com.semac.java_api.model.MetaDoacao;
import com.semac.java_api.repository.MetaDoacaoRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/* Meta de arrecadacao da campanha de doacao — uma linha por edição,
   editada no /admin (seção "Informações SEMAC"). Como é registro único
   por ano, a rota não tem /{id}: o GET devolve a meta do ano (ou zero
   quando ainda não foi cadastrada) e o PUT atualiza a existente ou cria
   a primeira, mesmo padrão de CamisetaExtraController.

   O GET é público: a página de doação precisa da meta para montar a
   barra de progresso. */
@RestController
@RequestMapping("/api/meta-doacao")
public class MetaDoacaoController {

    private final MetaDoacaoRepository repository;

    public MetaDoacaoController(MetaDoacaoRepository repository) {
        this.repository = repository;
    }

    /* Ano sem meta cadastrada devolve um registro zerado (200) em vez de
       404 — a barra precisa renderizar antes do primeiro cadastro. */
    @GetMapping
    public MetaDoacaoResponseDTO buscar(@RequestParam Integer ano) {
        return repository.findByAno(ano)
                .map(this::paraResposta)
                .orElseGet(() -> new MetaDoacaoResponseDTO(null, ano, BigDecimal.ZERO));
    }

    @PutMapping
    public MetaDoacaoResponseDTO salvar(@Valid @RequestBody MetaDoacaoRequestDTO dto) {
        MetaDoacao meta = repository.findByAno(dto.ano())
                .orElseGet(MetaDoacao::new);

        meta.setAno(dto.ano());
        meta.setValor(dto.valor());

        return paraResposta(repository.save(meta));
    }

    private MetaDoacaoResponseDTO paraResposta(MetaDoacao meta) {
        return new MetaDoacaoResponseDTO(meta.getId(), meta.getAno(), meta.getValor());
    }
}
