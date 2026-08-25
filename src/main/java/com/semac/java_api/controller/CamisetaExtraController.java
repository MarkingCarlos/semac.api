package com.semac.java_api.controller;

import com.semac.java_api.dto.CamisetaExtraRequestDTO;
import com.semac.java_api.dto.CamisetaExtraResponseDTO;
import com.semac.java_api.model.CamisetaExtra;
import com.semac.java_api.repository.CamisetaExtraRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/* Preço da camiseta avulsa — uma linha por edição, editada no /admin
   (seção "Informações SEMAC"). Como é registro único por ano, a rota não
   tem /{id}: o GET devolve o preço do ano (ou zero quando ainda não foi
   cadastrado) e o PUT atualiza o existente ou cria o primeiro, mesmo
   padrão de CaixaFundunespController.

   O GET é público: o cadastro em /inscricoes precisa do preço para montar
   a oferta de camiseta adicional. */
@RestController
@RequestMapping("/api/camiseta-extra")
public class CamisetaExtraController {

    private final CamisetaExtraRepository repository;

    public CamisetaExtraController(CamisetaExtraRepository repository) {
        this.repository = repository;
    }

    /* Ano sem preço cadastrado devolve um registro zerado (200) em vez de
       404 — a tela precisa renderizar antes do primeiro cadastro. */
    @GetMapping
    public CamisetaExtraResponseDTO buscar(@RequestParam Integer ano) {
        return repository.findByAno(ano)
                .map(this::paraResposta)
                .orElseGet(() -> new CamisetaExtraResponseDTO(null, ano, BigDecimal.ZERO));
    }

    @PutMapping
    public CamisetaExtraResponseDTO salvar(@Valid @RequestBody CamisetaExtraRequestDTO dto) {
        CamisetaExtra preco = repository.findByAno(dto.ano())
                .orElseGet(CamisetaExtra::new);

        preco.setAno(dto.ano());
        preco.setValor(dto.valor());

        return paraResposta(repository.save(preco));
    }

    private CamisetaExtraResponseDTO paraResposta(CamisetaExtra preco) {
        return new CamisetaExtraResponseDTO(preco.getId(), preco.getAno(), preco.getValor());
    }
}
