package com.semac.java_api.service;

import com.semac.java_api.dto.RegraXpRequestDTO;
import com.semac.java_api.dto.RegraXpResponseDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.RegraXp;
import com.semac.java_api.model.TipoEvento;
import com.semac.java_api.model.enums.UnidadeRegraXp;
import com.semac.java_api.repository.RegraXpRepository;
import com.semac.java_api.repository.TipoEventoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/* Regras de xp num lugar só, para o card "COMO GANHAR XP" do
   /participantes e para o bloco de edição em /admin -> Informações SEMAC.

   A lista junta duas fontes, de propósito:

   - `tipo_evento` (chave "TIPO_EVENTO:<id>") — quanto vale a presença em
     cada tipo (palestra, minicurso, ...). Já era editável na aba
     Conteúdo e continua sendo a única fonte desse número; aqui só é
     exposta junto das outras. Editar o nome por aqui renomeia o tipo em
     todo lugar (programação pública, formulário de evento).

   - `regra_xp` (chave simples) — o que antes era constante em código: o
     acerto do Termo e os dois limites de atraso do check-in.

   Nada é criado nem excluído por aqui: tipo de evento nasce e morre na
   aba Conteúdo, e cada chave de `regra_xp` tem código que a lê. */
@Service
public class RegraXpService {

    /* Chaves de `regra_xp` procuradas pelo código. */
    public static final String CHAVE_TERMO_ACERTO = "TERMO_ACERTO";
    public static final String CHAVE_ATRASO_METADE_MINUTOS = "ATRASO_METADE_MINUTOS";
    public static final String CHAVE_ATRASO_ZERO_MINUTOS = "ATRASO_ZERO_MINUTOS";

    /* Valores de antes da V40, usados se a linha sumir do banco: o
       check-in e o Termo não podem quebrar por causa de configuração
       faltando. */
    public static final int PADRAO_TERMO_ACERTO = 5;
    public static final long PADRAO_ATRASO_METADE_MINUTOS = 20;
    public static final long PADRAO_ATRASO_ZERO_MINUTOS = 30;

    private static final String PREFIXO_TIPO_EVENTO = "TIPO_EVENTO:";
    private static final String ORIGEM_TIPO_EVENTO = "TIPO_EVENTO";
    private static final String ORIGEM_REGRA = "REGRA";

    private final RegraXpRepository regraXpRepository;
    private final TipoEventoRepository tipoEventoRepository;

    public RegraXpService(RegraXpRepository regraXpRepository,
                          TipoEventoRepository tipoEventoRepository) {
        this.regraXpRepository = regraXpRepository;
        this.tipoEventoRepository = tipoEventoRepository;
    }

    /* ── Leitura ─────────────────────────────────────────────────── */

    /* Tipos de evento primeiro (em ordem alfabética, como na aba
       Conteúdo), depois as regras fixas na ordem cadastrada. */
    @Transactional(readOnly = true)
    public List<RegraXpResponseDTO> listar() {
        List<RegraXpResponseDTO> regras = new ArrayList<>();
        tipoEventoRepository.findAll().stream()
                .sorted(Comparator.comparing(tipo -> tipo.getNome().toLowerCase()))
                .map(this::paraResposta)
                .forEach(regras::add);
        regraXpRepository.findAllByOrderByOrdemAsc().stream()
                .map(this::paraResposta)
                .forEach(regras::add);
        return regras;
    }

    /* Xp de uma vitória no Termo (TermoService). */
    @Transactional(readOnly = true)
    public int pontosTermoAcerto() {
        return valorOuPadrao(CHAVE_TERMO_ACERTO, PADRAO_TERMO_ACERTO);
    }

    /* A partir deste atraso o check-in credita metade do xp do tipo de
       evento (InscricaoEventoService). */
    @Transactional(readOnly = true)
    public long atrasoMetadeMinutos() {
        return valorOuPadrao(CHAVE_ATRASO_METADE_MINUTOS, (int) PADRAO_ATRASO_METADE_MINUTOS);
    }

    /* A partir deste atraso a presença é registrada sem xp nenhum. */
    @Transactional(readOnly = true)
    public long atrasoZeroMinutos() {
        return valorOuPadrao(CHAVE_ATRASO_ZERO_MINUTOS, (int) PADRAO_ATRASO_ZERO_MINUTOS);
    }

    /* ── Edição (/admin) ─────────────────────────────────────────── */

    @Transactional
    public RegraXpResponseDTO atualizar(String chave, RegraXpRequestDTO dto) {
        if (chave.startsWith(PREFIXO_TIPO_EVENTO)) {
            return atualizarTipoEvento(idDoTipoEvento(chave), dto);
        }
        return atualizarRegra(chave, dto);
    }

    private RegraXpResponseDTO atualizarTipoEvento(Integer id, RegraXpRequestDTO dto) {
        TipoEvento tipo = tipoEventoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tipo de evento não encontrado."));
        if (tipoEventoRepository.existsByNomeIgnoreCaseAndIdNot(dto.nome(), id)) {
            throw new RecursoDuplicadoException("Já existe um tipo de evento com esse nome.");
        }
        tipo.setNome(dto.nome());
        tipo.setPontos(dto.valor());
        return paraResposta(tipoEventoRepository.save(tipo));
    }

    private RegraXpResponseDTO atualizarRegra(String chave, RegraXpRequestDTO dto) {
        RegraXp regra = regraXpRepository.findByChave(chave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Regra de xp não encontrada."));
        regra.setNome(dto.nome());
        regra.setValor(dto.valor());
        validarLimitesDeAtraso(regra);
        return paraResposta(regraXpRepository.save(regra));
    }

    /* Os dois cortes de atraso são lidos juntos no check-in, então a
       ordem entre eles tem que valer sempre: metade antes de zero. Sem
       isso um corte anula o outro em silêncio. */
    private void validarLimitesDeAtraso(RegraXp alterada) {
        if (!CHAVE_ATRASO_METADE_MINUTOS.equals(alterada.getChave())
                && !CHAVE_ATRASO_ZERO_MINUTOS.equals(alterada.getChave())) {
            return;
        }
        long metade = CHAVE_ATRASO_METADE_MINUTOS.equals(alterada.getChave())
                ? alterada.getValor()
                : atrasoMetadeMinutos();
        long zero = CHAVE_ATRASO_ZERO_MINUTOS.equals(alterada.getChave())
                ? alterada.getValor()
                : atrasoZeroMinutos();
        if (metade >= zero) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O atraso que corta o XP pela metade precisa ser menor que o que zera o XP.");
        }
    }

    private Integer idDoTipoEvento(String chave) {
        try {
            return Integer.valueOf(chave.substring(PREFIXO_TIPO_EVENTO.length()));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Regra de xp não encontrada.");
        }
    }

    private int valorOuPadrao(String chave, int padrao) {
        return regraXpRepository.findByChave(chave)
                .map(RegraXp::getValor)
                .orElse(padrao);
    }

    /* `nome` sai cru (o nome do tipo, como está em `tipo_evento`) porque
       é ele que volta no PUT. Quem monta a frase "Presença em palestra"
       é a tela do participante, a partir de `origem`. */
    private RegraXpResponseDTO paraResposta(TipoEvento tipo) {
        return new RegraXpResponseDTO(
                PREFIXO_TIPO_EVENTO + tipo.getId(),
                tipo.getNome(),
                tipo.getPontos(),
                UnidadeRegraXp.PONTOS.name(),
                ORIGEM_TIPO_EVENTO);
    }

    private RegraXpResponseDTO paraResposta(RegraXp regra) {
        return new RegraXpResponseDTO(
                regra.getChave(),
                regra.getNome(),
                regra.getValor(),
                regra.getUnidade().name(),
                ORIGEM_REGRA);
    }
}
