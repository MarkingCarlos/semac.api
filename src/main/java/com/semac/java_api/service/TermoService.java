package com.semac.java_api.service;

import com.semac.java_api.dto.*;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.TermoJogo;
import com.semac.java_api.model.TermoPalavra;
import com.semac.java_api.model.TermoTentativa;
import com.semac.java_api.repository.NivelRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.TermoJogoRepository;
import com.semac.java_api.repository.TermoPalavraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* Jogo do Termo (/termo).

   A regra que organiza esta classe: a palavra secreta não sai daqui. O
   navegador manda o palpite e recebe de volta só o padrão de cores; a
   palavra só entra numa resposta quando a partida já acabou, para a tela
   de fim revelar. Antes disso ela estava hardcoded no bundle JS e qualquer
   um a lia no DevTools — era esse o furo.

   Consequência prática: quem confere o palpite é o servidor, e o limite de
   6 tentativas mora no banco (termo_jogo + termo_tentativa), não no
   navegador. F5 e aba anônima não devolvem tentativa; reabrir a página
   retoma a partida onde parou. */
@Service
public class TermoService {

    private static final Logger log = LoggerFactory.getLogger(TermoService.class);

    public static final int TOTAL_TENTATIVAS = 6;
    public static final int TAMANHO_PALAVRA = 5;

    /* Padrão de cores, uma letra por posição. */
    private static final char CERTO = 'C';
    private static final char PRESENTE = 'P';
    private static final char AUSENTE = 'A';

    private static final String ARQUIVO_PALAVRAS_VALIDAS = "termo/palavras-validas.txt";

    private final TermoPalavraRepository palavraRepository;
    private final TermoJogoRepository jogoRepository;
    private final PessoaRepository pessoaRepository;
    private final NivelRepository nivelRepository;

    /* Xp de uma vitória, creditado uma vez por dia (unique pessoa+palavra
       em termo_jogo). Vem da regra TERMO_ACERTO, editável no /admin ->
       Informações SEMAC; partidas já encerradas guardam em
       termo_jogo.xp_creditado o valor que valia na hora. */
    private final RegraXpService regraXpService;

    /* Dicionário de palpites aceitos, carregado uma vez no boot. O cliente
       valida com a mesma lista para dar resposta instantânea, mas a
       checagem dele é contornável por curl — esta é a que vale. */
    private final Set<String> palavrasValidas;

    public TermoService(TermoPalavraRepository palavraRepository,
                        TermoJogoRepository jogoRepository,
                        PessoaRepository pessoaRepository,
                        NivelRepository nivelRepository,
                        RegraXpService regraXpService) {
        this.palavraRepository = palavraRepository;
        this.jogoRepository = jogoRepository;
        this.pessoaRepository = pessoaRepository;
        this.nivelRepository = nivelRepository;
        this.regraXpService = regraXpService;
        this.palavrasValidas = carregarPalavrasValidas();
    }

    /* ── Jogador ─────────────────────────────────────────────────── */

    /* Estado do jogo de hoje: o que a tela precisa para se montar, já
       retomando a partida em andamento se houver. */
    @Transactional
    public TermoEstadoDTO estadoDeHoje(Integer pessoaId) {
        TermoPalavra palavra = palavraDeHoje();
        if (palavra == null) {
            return new TermoEstadoDTO(false, null, false, List.of(), false, false, null, null);
        }

        Pessoa pessoa = buscarPessoa(pessoaId);
        TermoJogo jogo = jogoRepository.findByPessoaIdAndPalavraId(pessoa.getId(), palavra.getId())
                .orElse(null);

        if (jogo == null) {
            return new TermoEstadoDTO(true, palavra.getDia(), ehUltimoDia(palavra),
                    List.of(), false, false, null, null);
        }
        return montarEstado(jogo, palavra);
    }

    /* Confere um palpite e gasta uma tentativa.

       Toda a decisão é aqui: se a palavra existe, se ainda há tentativa, o
       padrão de cores, se venceu e se credita xp. O cliente só desenha o
       que voltar. */
    @Transactional
    public TermoPalpiteRespostaDTO palpitar(Integer pessoaId, String palpiteBruto) {
        TermoPalavra palavra = palavraDeHoje();
        if (palavra == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hoje não é dia de Termo.");
        }

        String palpite = normalizar(palpiteBruto);
        if (palpite.length() != TAMANHO_PALAVRA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O palpite precisa ter " + TAMANHO_PALAVRA + " letras.");
        }
        /* A palavra do dia sempre passa, mesmo fora do dicionário: "SEMAC"
           e "CYBER" não constam em dicionário nenhum e precisam ser
           enviáveis, senão não dá para acertar. */
        String secreta = normalizar(palavra.getPalavra());
        if (!palpite.equals(secreta) && !palavrasValidas.contains(palpite)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Palavra não encontrada.");
        }

        Pessoa pessoa = buscarPessoa(pessoaId);
        TermoJogo jogo = jogoRepository.findByPessoaIdAndPalavraId(pessoa.getId(), palavra.getId())
                .orElseGet(() -> criarJogo(pessoa, palavra));

        if (jogoEncerrado(jogo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Seu jogo de hoje já acabou.");
        }
        if (jogo.getTentativas().size() >= TOTAL_TENTATIVAS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já usou suas " + TOTAL_TENTATIVAS + " tentativas de hoje.");
        }

        String resultado = calcularResultado(palpite, secreta);

        TermoTentativa tentativa = new TermoTentativa();
        tentativa.setJogo(jogo);
        tentativa.setOrdem(jogo.getTentativas().size() + 1);
        tentativa.setPalpite(palpite);
        tentativa.setResultado(resultado);
        tentativa.setCriadoEm(LocalDateTime.now());
        jogo.getTentativas().add(tentativa);

        boolean venceu = palpite.equals(secreta);
        boolean encerrado = venceu || jogo.getTentativas().size() >= TOTAL_TENTATIVAS;

        int xpVitoria = regraXpService.pontosTermoAcerto();
        if (encerrado) {
            jogo.setVenceu(venceu);
            jogo.setEncerradoEm(LocalDateTime.now());
            if (venceu) {
                jogo.setXpCreditado(xpVitoria);
                creditarXp(pessoa, xpVitoria);
                log.info("Termo dia {}: {} (id {}) acertou, +{} xp.",
                        palavra.getDia(), pessoa.getNome(), pessoa.getId(), xpVitoria);
            } else {
                jogo.setXpCreditado(0);
            }
        }
        jogoRepository.save(jogo);

        return new TermoPalpiteRespostaDTO(
                resultado,
                venceu,
                encerrado,
                TOTAL_TENTATIVAS - jogo.getTentativas().size(),
                encerrado ? (venceu ? xpVitoria : 0) : null,
                ehUltimoDia(palavra),
                encerrado ? secreta : null);
    }

    /* ── Diretoria (/admin -> Termo) ─────────────────────────────── */

    /* Os quatro dias do ano, sem a palavra — só se ela já foi definida.
       Dia ainda não cadastrado aparece com data null, para a tela sempre
       ter as quatro linhas. */
    public List<TermoPalavraAdminDTO> listarParaAdmin(Integer ano) {
        List<TermoPalavra> cadastradas = palavraRepository.findByAnoOrderByDiaAsc(ano);
        return java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(dia -> cadastradas.stream()
                        .filter(p -> p.getDia().equals(dia))
                        .findFirst()
                        .map(p -> new TermoPalavraAdminDTO(p.getDia(), p.getData(), true))
                        .orElseGet(() -> new TermoPalavraAdminDTO(dia, null, false)))
                .toList();
    }

    @Transactional
    public TermoPalavraAdminDTO salvar(Integer dia, TermoPalavraRequestDTO dto) {
        if (dia < 1 || dia > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O dia precisa ser de 1 a 4.");
        }

        TermoPalavra palavra = palavraRepository.findByAnoAndDia(dto.ano(), dia)
                .orElseGet(TermoPalavra::new);

        /* Uma data só pode valer por um dia — é ela que resolve qual
           palavra está ativa. Checar aqui evita um 500 pela constraint. */
        palavraRepository.findByDataAndIdNot(dto.data(),
                        palavra.getId() == null ? -1 : palavra.getId())
                .ifPresent(outro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "O dia " + outro.getDia() + " já usa essa data.");
                });

        palavra.setAno(dto.ano());
        palavra.setDia(dia);
        palavra.setData(dto.data());
        palavra.setPalavra(normalizar(dto.palavra()));

        TermoPalavra salva = palavraRepository.save(palavra);
        /* De propósito o log não imprime a palavra. */
        log.info("Termo: palavra do dia {} ({}) definida.", salva.getDia(), salva.getData());
        return new TermoPalavraAdminDTO(salva.getDia(), salva.getData(), true);
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    private TermoPalavra palavraDeHoje() {
        return palavraRepository.findByData(LocalDate.now()).orElse(null);
    }

    /* Último dia cadastrado no ano — o modal de derrota troca de texto
       ("não foi dessa vez" em vez de "volte amanhã"). */
    private boolean ehUltimoDia(TermoPalavra palavra) {
        return palavraRepository.findTopByAnoOrderByDiaDesc(palavra.getAno())
                .map(ultima -> ultima.getDia().equals(palavra.getDia()))
                .orElse(true);
    }

    private boolean jogoEncerrado(TermoJogo jogo) {
        return jogo.getEncerradoEm() != null;
    }

    private TermoJogo criarJogo(Pessoa pessoa, TermoPalavra palavra) {
        TermoJogo jogo = new TermoJogo();
        jogo.setPessoa(pessoa);
        jogo.setPalavra(palavra);
        return jogoRepository.save(jogo);
    }

    private TermoEstadoDTO montarEstado(TermoJogo jogo, TermoPalavra palavra) {
        boolean encerrado = jogoEncerrado(jogo);
        boolean venceu = Boolean.TRUE.equals(jogo.getVenceu());
        List<TermoTentativaDTO> tentativas = jogo.getTentativas().stream()
                .map(t -> new TermoTentativaDTO(t.getPalpite(), t.getResultado()))
                .toList();

        return new TermoEstadoDTO(
                true,
                palavra.getDia(),
                ehUltimoDia(palavra),
                tentativas,
                encerrado,
                venceu,
                encerrado ? (jogo.getXpCreditado() == null ? 0 : jogo.getXpCreditado()) : null,
                encerrado ? normalizar(palavra.getPalavra()) : null);
    }

    private Pessoa buscarPessoa(Integer pessoaId) {
        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    /* Padrão de cores do palpite. Duas passadas, como manda o jogo: os
       acertos de posição primeiro, e só depois as letras presentes fora de
       lugar consomem as ocorrências que sobraram — sem isso "AAABB" contra
       "ABCDE" marcaria três amarelos para um único A. */
    private String calcularResultado(String palpite, String secreta) {
        char[] p = palpite.toCharArray();
        char[] s = secreta.toCharArray();
        char[] estado = new char[TAMANHO_PALAVRA];
        boolean[] usados = new boolean[TAMANHO_PALAVRA];

        for (int i = 0; i < TAMANHO_PALAVRA; i++) {
            if (p[i] == s[i]) {
                estado[i] = CERTO;
                usados[i] = true;
            }
        }
        for (int i = 0; i < TAMANHO_PALAVRA; i++) {
            if (estado[i] == CERTO) continue;
            estado[i] = AUSENTE;
            for (int j = 0; j < TAMANHO_PALAVRA; j++) {
                if (!usados[j] && s[j] == p[i]) {
                    estado[i] = PRESENTE;
                    usados[j] = true;
                    break;
                }
            }
        }
        return new String(estado);
    }

    /* Sem acento e em maiúsculas: é assim que palavra e palpite são
       comparados e gravados, igual ao que o front faz na digitação. */
    private String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase();
    }

    /* Mesmo critério de InscricaoEventoService.creditarXp: soma em
       pessoa.xp e recalcula o nível alcançado. */
    private void creditarXp(Pessoa pessoa, int xp) {
        int xpAtual = pessoa.getXp() == null ? 0 : pessoa.getXp();
        int novoXp = xpAtual + xp;
        pessoa.setXp(novoXp);
        nivelRepository.findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc(novoXp)
                .ifPresent(pessoa::setNivel);
        pessoaRepository.save(pessoa);
    }

    private Set<String> carregarPalavrasValidas() {
        Set<String> palavras = new HashSet<>();
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(
                new ClassPathResource(ARQUIVO_PALAVRAS_VALIDAS).getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                String palavra = linha.trim();
                if (palavra.isEmpty() || palavra.startsWith("#")) continue;
                palavras.add(palavra.toUpperCase());
            }
        } catch (IOException e) {
            /* Sem dicionário o servidor aceitaria qualquer sequência de 5
               letras — é uma falha de empacotamento, não um estado de
               operação normal. */
            throw new IllegalStateException(
                    "Não foi possível ler " + ARQUIVO_PALAVRAS_VALIDAS + " para o jogo do Termo.", e);
        }
        log.info("Termo: {} palavras carregadas para validação de palpite.", palavras.size());
        return Set.copyOf(palavras);
    }
}
