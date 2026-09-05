package com.semac.java_api.controller;

import com.semac.java_api.dto.ConfiguracaoInscricaoRequestDTO;
import com.semac.java_api.dto.ConfiguracaoInscricaoResponseDTO;
import com.semac.java_api.model.ConfiguracaoInscricao;
import com.semac.java_api.repository.ConfiguracaoInscricaoRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/* Liga/desliga o botão "Inscreva-se" da Home — um registro por edição,
   editado no /admin (seção "Informações SEMAC"). Registro único por ano,
   então a rota não tem /{id}: o GET devolve a configuração do ano (ou
   aberta=true quando ainda não foi configurada) e o PUT atualiza a
   existente ou cria a primeira, mesmo padrão de MetaDoacaoController.

   O GET é público: a Home precisa saber se mostra o botão. */
@RestController
@RequestMapping("/api/configuracao-inscricao")
public class ConfiguracaoInscricaoController {

    private final ConfiguracaoInscricaoRepository repository;

    public ConfiguracaoInscricaoController(ConfiguracaoInscricaoRepository repository) {
        this.repository = repository;
    }

    /* Ano sem configuração cadastrada devolve aberta=true (200) — sem
       configuração explícita, o botão continua visível. */
    @GetMapping
    public ConfiguracaoInscricaoResponseDTO buscar(@RequestParam Integer ano) {
        return repository.findByAno(ano)
                .map(this::paraResposta)
                .orElseGet(() -> new ConfiguracaoInscricaoResponseDTO(null, ano, Boolean.TRUE));
    }

    @PutMapping
    public ConfiguracaoInscricaoResponseDTO salvar(@Valid @RequestBody ConfiguracaoInscricaoRequestDTO dto) {
        ConfiguracaoInscricao config = repository.findByAno(dto.ano())
                .orElseGet(ConfiguracaoInscricao::new);

        config.setAno(dto.ano());
        config.setInscricoesAbertas(dto.inscricoesAbertas());

        return paraResposta(repository.save(config));
    }

    private ConfiguracaoInscricaoResponseDTO paraResposta(ConfiguracaoInscricao config) {
        return new ConfiguracaoInscricaoResponseDTO(config.getId(), config.getAno(), config.getInscricoesAbertas());
    }
}
