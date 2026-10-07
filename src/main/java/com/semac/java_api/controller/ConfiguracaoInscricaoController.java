package com.semac.java_api.controller;

import com.semac.java_api.dto.ConfiguracaoInscricaoRequestDTO;
import com.semac.java_api.dto.ConfiguracaoInscricaoResponseDTO;
import com.semac.java_api.dto.EscolhaMinicursosRequestDTO;
import com.semac.java_api.dto.EscolhaMinicursosResponseDTO;
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

    /* Escolha de minicursos no /participantes — rota à parte para que
       salvar um botão nunca sobrescreva o outro, e porque quem edita é a
       diretoria de conteúdo, não a financeira (ver SecurityConfig). O GET
       é de qualquer usuário logado: o /participantes precisa saber se
       mostra o botão de escolha. Ano sem configuração volta fechado. */
    @GetMapping("/minicursos")
    public EscolhaMinicursosResponseDTO buscarEscolhaMinicursos(@RequestParam Integer ano) {
        boolean aberta = repository.findByAno(ano)
                .map(ConfiguracaoInscricao::getEscolhaMinicursosAberta)
                .orElse(Boolean.FALSE);
        return new EscolhaMinicursosResponseDTO(ano, aberta);
    }

    /* Criando a linha do ano por aqui, o botão "Inscreva-se" nasce aberto
       — o mesmo valor que o GET dele já devolvia sem configuração. */
    @PutMapping("/minicursos")
    public EscolhaMinicursosResponseDTO salvarEscolhaMinicursos(@Valid @RequestBody EscolhaMinicursosRequestDTO dto) {
        ConfiguracaoInscricao config = repository.findByAno(dto.ano())
                .orElseGet(() -> {
                    ConfiguracaoInscricao nova = new ConfiguracaoInscricao();
                    nova.setAno(dto.ano());
                    nova.setInscricoesAbertas(Boolean.TRUE);
                    return nova;
                });

        config.setEscolhaMinicursosAberta(dto.escolhaMinicursosAberta());

        ConfiguracaoInscricao salva = repository.save(config);
        return new EscolhaMinicursosResponseDTO(salva.getAno(), salva.getEscolhaMinicursosAberta());
    }

    private ConfiguracaoInscricaoResponseDTO paraResposta(ConfiguracaoInscricao config) {
        return new ConfiguracaoInscricaoResponseDTO(config.getId(), config.getAno(), config.getInscricoesAbertas());
    }
}
