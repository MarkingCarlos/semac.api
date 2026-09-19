package com.semac.java_api.service;

import com.semac.java_api.config.CatalogoVariaveisEmail;
import com.semac.java_api.config.CatalogoVariaveisEmail.ModeloSemeado;
import com.semac.java_api.config.CatalogoVariaveisEmail.VariavelEmail;
import com.semac.java_api.dto.ModeloEmailRequestDTO;
import com.semac.java_api.dto.ModeloEmailResponseDTO;
import com.semac.java_api.dto.PreviaEmailResponseDTO;
import com.semac.java_api.dto.VariavelEmailDTO;
import com.semac.java_api.model.ModeloEmail;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.repository.ModeloEmailRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/* Regras das mensagens editáveis em /admin -> Mensagens. */
@Service
public class ModeloEmailService {

    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ModeloEmailRepository modeloEmailRepository;
    private final PessoaRepository pessoaRepository;
    private final RenderizadorEmailService renderizador;

    public ModeloEmailService(ModeloEmailRepository modeloEmailRepository,
                              PessoaRepository pessoaRepository,
                              RenderizadorEmailService renderizador) {
        this.modeloEmailRepository = modeloEmailRepository;
        this.pessoaRepository = pessoaRepository;
        this.renderizador = renderizador;
    }

    @Transactional(readOnly = true)
    public List<ModeloEmailResponseDTO> listar() {
        return modeloEmailRepository.findAllByOrderByChaveAsc().stream()
                .filter(m -> CatalogoVariaveisEmail.existe(m.getChave()))
                .map(this::paraResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public ModeloEmailResponseDTO buscar(String chave) {
        return paraResposta(exigirModelo(chave));
    }

    @Transactional
    public ModeloEmailResponseDTO atualizar(String chave, ModeloEmailRequestDTO dto, Integer idEditor) {
        ModeloEmail modelo = exigirModelo(chave);
        validarVariaveis(chave, dto.corpoMarkdown());

        modelo.setAssunto(dto.assunto().trim());
        modelo.setCorpoMarkdown(dto.corpoMarkdown());
        if (dto.ativo() != null) {
            modelo.setAtivo(dto.ativo());
        }
        modelo.setAtualizadoEm(LocalDateTime.now());
        pessoaRepository.findById(idEditor).ifPresent(modelo::setAtualizadoPor);

        return paraResposta(modeloEmailRepository.save(modelo));
    }

    /* Renderiza o conteúdo recebido (não o salvo) com os valores de exemplo
       do catálogo — é a prévia da tela. */
    public PreviaEmailResponseDTO previa(String chave, String assunto, String corpoMarkdown) {
        exigirCatalogo(chave);
        validarVariaveis(chave, corpoMarkdown);
        return new PreviaEmailResponseDTO(assunto, renderizador.renderizar(corpoMarkdown, valoresDeExemplo(chave)));
    }

    /* Corpo salvo + valores reais -> HTML pronto. Usado pelo envio de
       verdade; o assunto sai junto porque também é editável. */
    @Transactional(readOnly = true)
    public MensagemPronta montar(String chave, Map<String, String> variaveis) {
        ModeloEmail modelo = modeloEmailRepository.findByChave(chave).orElse(null);

        /* Sem linha no banco (ou desligada no /admin), cai no texto padrão
           do catálogo. Apagar uma linha por engano não pode calar um e-mail
           que o participante espera receber. */
        if (modelo == null) {
            ModeloSemeado padrao = exigirCatalogo(chave);
            return new MensagemPronta(padrao.assuntoPadrao(),
                    renderizador.renderizar(padrao.corpoPadrao(), variaveis), true);
        }
        if (!Boolean.TRUE.equals(modelo.getAtivo())) {
            return new MensagemPronta(modelo.getAssunto(), null, true);
        }
        return new MensagemPronta(modelo.getAssunto(),
                renderizador.renderizar(modelo.getCorpoMarkdown(), variaveis), false);
    }

    /* `html` nulo significa mensagem desligada no /admin — não envie. */
    public record MensagemPronta(String assunto, String html, boolean usouPadrao) {
    }

    public Map<String, String> valoresDeExemplo(String chave) {
        Map<String, String> exemplos = new LinkedHashMap<>();
        for (VariavelEmail variavel : exigirCatalogo(chave).variaveis()) {
            exemplos.put(variavel.nome(), variavel.exemplo());
        }
        return exemplos;
    }

    private void validarVariaveis(String chave, String corpoMarkdown) {
        renderizador.validarVariaveis(corpoMarkdown, exigirCatalogo(chave).variaveis().stream()
                .map(VariavelEmail::nome)
                .collect(Collectors.toSet()));
    }

    private ModeloEmail exigirModelo(String chave) {
        exigirCatalogo(chave);
        return modeloEmailRepository.findByChave(chave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Mensagem não encontrada. Reinicie a aplicação para o catálogo ser semeado."));
    }

    private ModeloSemeado exigirCatalogo(String chave) {
        ModeloSemeado semeado = CatalogoVariaveisEmail.porChave(chave);
        if (semeado == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensagem desconhecida: " + chave + ".");
        }
        return semeado;
    }

    private ModeloEmailResponseDTO paraResposta(ModeloEmail modelo) {
        ModeloSemeado semeado = exigirCatalogo(modelo.getChave());
        Pessoa editor = modelo.getAtualizadoPor();

        return new ModeloEmailResponseDTO(
                modelo.getChave(),
                semeado.nomeExibicao(),
                semeado.descricao(),
                modelo.getAssunto(),
                modelo.getCorpoMarkdown(),
                modelo.getAtivo(),
                modelo.getAtualizadoEm() == null ? null : modelo.getAtualizadoEm().format(FORMATO_DATA_HORA),
                editor == null ? null : editor.getNome(),
                semeado.variaveis().stream()
                        .map(v -> new VariavelEmailDTO(v.nome(), v.descricao(), v.exemplo()))
                        .toList());
    }
}
