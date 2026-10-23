package com.semac.java_api.service;

import com.semac.java_api.dto.*;
import com.semac.java_api.model.DesafioCifra;
import com.semac.java_api.model.DesafioCifraEtapa;
import com.semac.java_api.model.DesafioCifraJogo;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.repository.DesafioCifraJogoRepository;
import com.semac.java_api.repository.DesafioCifraRepository;
import com.semac.java_api.repository.NivelRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/* Desafio diário "Cifra do Dia" (/desafio-cifra).

   Mesma regra que organiza o TermoService: a resposta não sai daqui. O
   navegador manda a tentativa e recebe só se acertou. Diferente do Termo,
   nem ao perder a resposta é revelada — o desafio é o mesmo para todos, e
   revelá-la a quem gastou as tentativas a faria circular enquanto o dia
   ainda vale.

   Cada desafio tem 1 ou 2 etapas (ver DesafioCifraEtapa). As 5 tentativas
   são do desafio inteiro e só o erro gasta uma: acertar a etapa 1 não tira
   tentativa de quem ainda vai encarar a 2. O xp entra ao concluir a
   última. */
@Service
public class DesafioCifraService {

    private static final Logger log = LoggerFactory.getLogger(DesafioCifraService.class);

    public static final int TOTAL_TENTATIVAS = 5;

    private static final Pattern SOBRESCRITO = Pattern.compile("[⁰¹²³⁴⁵⁶⁷⁸⁹]");

    private final DesafioCifraRepository desafioRepository;
    private final DesafioCifraJogoRepository jogoRepository;
    private final PessoaRepository pessoaRepository;
    private final NivelRepository nivelRepository;
    private final RegraXpService regraXpService;

    public DesafioCifraService(DesafioCifraRepository desafioRepository,
                               DesafioCifraJogoRepository jogoRepository,
                               PessoaRepository pessoaRepository,
                               NivelRepository nivelRepository,
                               RegraXpService regraXpService) {
        this.desafioRepository = desafioRepository;
        this.jogoRepository = jogoRepository;
        this.pessoaRepository = pessoaRepository;
        this.nivelRepository = nivelRepository;
        this.regraXpService = regraXpService;
    }

    /* ── Jogador ─────────────────────────────────────────────────── */

    /* Estado do desafio de hoje. Só lê: o jogo da pessoa nasce na primeira
       tentativa, então abrir a página não conta como ter jogado. */
    @Transactional(readOnly = true)
    public DesafioCifraEstadoDTO estadoDeHoje(Integer pessoaId) {
        DesafioCifra desafio = desafioDeHoje();
        if (desafio == null || desafio.getEtapas().isEmpty()) {
            return indisponivel();
        }
        Pessoa pessoa = buscarPessoa(pessoaId);
        DesafioCifraJogo jogo = jogoRepository.findByPessoaIdAndDesafioId(pessoa.getId(), desafio.getId())
                .orElse(null);
        return montarEstado(desafio, jogo);
    }

    /* Histórico dos dias que já aconteceram, um card por dia na aba
       "Desafios" — mesmo critério de TermoService.listarDiasDoParticipante:
       dia que passou aparece mesmo sem jogo, dia por vir fica de fora. */
    @Transactional(readOnly = true)
    public List<DesafioCifraDiaDTO> listarDiasDoParticipante(Integer pessoaId) {
        LocalDate hoje = LocalDate.now();
        List<DesafioCifra> ocorridos =
                desafioRepository.findByAnoAndDataLessThanEqualOrderByDataAsc(hoje.getYear(), hoje);
        if (ocorridos.isEmpty()) {
            return List.of();
        }

        Pessoa pessoa = buscarPessoa(pessoaId);
        Map<Integer, DesafioCifraJogo> jogosPorDesafio = jogoRepository
                .findByPessoaIdAndDesafioIdIn(pessoa.getId(),
                        ocorridos.stream().map(DesafioCifra::getId).toList())
                .stream()
                .collect(Collectors.toMap(jogo -> jogo.getDesafio().getId(), jogo -> jogo));

        return ocorridos.stream()
                .map(desafio -> montarDia(desafio, jogosPorDesafio.get(desafio.getId()), hoje))
                .toList();
    }

    /* Confere uma resposta da etapa atual; errar gasta uma tentativa.

       Resposta com o número errado de caracteres é recusada sem gastar
       tentativa — o campo da tela já impede isso, então só chega aqui por
       requisição montada à mão, e não vale punir por formato. */
    @Transactional
    public DesafioCifraTentativaRespostaDTO tentar(Integer pessoaId, String respostaBruta) {
        DesafioCifra hoje = desafioDeHoje();
        if (hoje == null || hoje.getEtapas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hoje não tem Cifra do Dia.");
        }

        DesafioCifra desafio = desafioRepository.buscarParaJogar(hoje.getId()).orElse(hoje);
        Pessoa pessoa = buscarPessoa(pessoaId);
        DesafioCifraJogo jogo = jogoRepository.findByPessoaIdAndDesafioId(pessoa.getId(), desafio.getId())
                .orElseGet(() -> novoJogo(pessoa, desafio));

        if (jogo.getEncerradoEm() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seu desafio de hoje já acabou.");
        }

        List<DesafioCifraEtapa> etapas = desafio.getEtapas();
        DesafioCifraEtapa etapa = etapas.get(jogo.getEtapasConcluidas());
        String esperada = normalizar(etapa.getResposta());
        String resposta = normalizar(respostaBruta);
        if (resposta.length() != esperada.length()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A resposta precisa ter " + esperada.length() + " caracteres.");
        }

        boolean acertou = resposta.equals(esperada);
        if (acertou) {
            jogo.setEtapasConcluidas(jogo.getEtapasConcluidas() + 1);
        } else {
            jogo.setTentativasUsadas(jogo.getTentativasUsadas() + 1);
        }

        boolean venceu = jogo.getEtapasConcluidas() == etapas.size();
        if (venceu) {
            int xp = regraXpService.pontosCifraAcerto();
            jogo.setVenceu(true);
            jogo.setEncerradoEm(LocalDateTime.now());
            jogo.setXpCreditado(xp);
            creditarXp(pessoa, xp);
            log.info("Cifra do Dia {}: {} (id {}) decifrou, +{} xp.",
                    desafio.getData(), pessoa.getNome(), pessoa.getId(), xp);
        } else if (jogo.getTentativasUsadas() >= TOTAL_TENTATIVAS) {
            jogo.setVenceu(false);
            jogo.setEncerradoEm(LocalDateTime.now());
            jogo.setXpCreditado(0);
        }
        jogoRepository.save(jogo);

        return new DesafioCifraTentativaRespostaDTO(acertou, montarEstado(desafio, jogo));
    }

    /* ── Diretoria (/admin -> Cifra do Dia) ──────────────────────── */

    /* Só leitura. Os desafios nascem e mudam por migration (V64 terça, V65
       quarta, ...): cifra e resposta passam por conferência antes de ir ao
       ar, e uma edição pela tela no meio da semana mexeria na resposta de
       quem já está jogando. O admin vê, revela a resposta e testa. */

    @Transactional(readOnly = true)
    public List<DesafioCifraAdminDTO> listarParaAdmin(Integer ano) {
        return desafioRepository.findByAnoOrderByDataAsc(ano).stream()
                .map(this::montarAdmin)
                .toList();
    }

    /* ── Modo teste (/admin -> Cifra do Dia -> Testar) ─────────── */

    /* A diretoria joga um desafio cadastrado, de qualquer data, para
       conferir cifra, casas e dicas. Nada aqui grava: sem jogo, sem
       tentativa e sem xp. As 5 tentativas e a troca de etapa são contadas
       na tela — perdeu, pode recomeçar, o que o participante não pode.

       Todas as etapas vêm de uma vez (a tela avança sozinha), mas sem
       resposta: mesmo critério do teste do Termo, que não a revela nem ao
       perder. Quem precisa dela usa o "Mostrar" do admin. */
    @Transactional(readOnly = true)
    public DesafioCifraEstadoDTO abrirTeste(Integer desafioId) {
        DesafioCifra desafio = buscarDesafio(desafioId);
        List<DesafioCifraEtapaDTO> etapas = desafio.getEtapas().stream()
                .map(etapa -> new DesafioCifraEtapaDTO(
                        etapa.getOrdem(),
                        grupos(etapa.getResposta()),
                        etapa.getDica(),
                        false,
                        null,
                        SOBRESCRITO.matcher(etapa.getResposta()).find()))
                .toList();
        return new DesafioCifraEstadoDTO(
                !etapas.isEmpty(),
                desafio.getData(),
                desafio.getPalestra(),
                desafio.getSenhaCifrada(),
                etapas.size(),
                etapas,
                TOTAL_TENTATIVAS,
                TOTAL_TENTATIVAS,
                false,
                false,
                null,
                regraXpService.pontosCifraAcerto());
    }

    /* Mesma comparação e mesma recusa de tamanho da partida valendo. */
    @Transactional(readOnly = true)
    public DesafioCifraTesteRespostaDTO tentarTeste(DesafioCifraTesteTentativaDTO dto) {
        DesafioCifra desafio = buscarDesafio(dto.desafioId());
        DesafioCifraEtapa etapa = desafio.getEtapas().stream()
                .filter(e -> e.getOrdem().equals(dto.ordem()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Etapa não encontrada."));
        String esperada = normalizar(etapa.getResposta());
        String resposta = normalizar(dto.resposta());
        if (resposta.length() != esperada.length()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A resposta precisa ter " + esperada.length() + " caracteres.");
        }
        return new DesafioCifraTesteRespostaDTO(resposta.equals(esperada));
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    private DesafioCifra desafioDeHoje() {
        return desafioRepository.findByData(LocalDate.now()).orElse(null);
    }

    /* Outra requisição da mesma pessoa pode ter criado o jogo enquanto
       esta esperava a trava do desafio — por isso a busca de novo. */
    private DesafioCifraJogo novoJogo(Pessoa pessoa, DesafioCifra desafio) {
        return jogoRepository.findByPessoaIdAndDesafioId(pessoa.getId(), desafio.getId())
                .orElseGet(() -> {
                    DesafioCifraJogo jogo = new DesafioCifraJogo();
                    jogo.setPessoa(pessoa);
                    jogo.setDesafio(desafio);
                    return jogo;
                });
    }

    private DesafioCifraEstadoDTO indisponivel() {
        return new DesafioCifraEstadoDTO(false, null, null, null, 0, List.of(),
                TOTAL_TENTATIVAS, TOTAL_TENTATIVAS, false, false, null, regraXpService.pontosCifraAcerto());
    }

    /* `jogo` null = a pessoa ainda não enviou nenhuma resposta hoje. */
    private DesafioCifraEstadoDTO montarEstado(DesafioCifra desafio, DesafioCifraJogo jogo) {
        List<DesafioCifraEtapa> etapas = desafio.getEtapas();
        int concluidas = jogo == null ? 0 : jogo.getEtapasConcluidas();
        int usadas = jogo == null ? 0 : jogo.getTentativasUsadas();
        boolean encerrado = jogo != null && jogo.getEncerradoEm() != null;
        boolean venceu = jogo != null && Boolean.TRUE.equals(jogo.getVenceu());

        /* As concluídas e a atual; quem venceu não tem etapa atual. */
        int visiveis = Math.min(etapas.size(), concluidas + 1);
        List<DesafioCifraEtapaDTO> etapasDTO = IntStream.range(0, visiveis)
                .mapToObj(i -> {
                    DesafioCifraEtapa etapa = etapas.get(i);
                    boolean concluida = i < concluidas;
                    return new DesafioCifraEtapaDTO(
                            etapa.getOrdem(),
                            grupos(etapa.getResposta()),
                            etapa.getDica(),
                            concluida,
                            concluida ? normalizar(etapa.getResposta()) : null,
                            SOBRESCRITO.matcher(etapa.getResposta()).find());
                })
                .toList();

        return new DesafioCifraEstadoDTO(
                true,
                desafio.getData(),
                desafio.getPalestra(),
                desafio.getSenhaCifrada(),
                etapas.size(),
                etapasDTO,
                TOTAL_TENTATIVAS,
                TOTAL_TENTATIVAS - usadas,
                encerrado,
                venceu,
                encerrado ? (jogo.getXpCreditado() == null ? 0 : jogo.getXpCreditado()) : null,
                regraXpService.pontosCifraAcerto());
    }

    private DesafioCifraDiaDTO montarDia(DesafioCifra desafio, DesafioCifraJogo jogo, LocalDate hoje) {
        boolean encerrado = jogo != null && jogo.getEncerradoEm() != null;
        boolean venceu = jogo != null && Boolean.TRUE.equals(jogo.getVenceu());
        return new DesafioCifraDiaDTO(
                desafio.getId(),
                desafio.getData(),
                desafio.getData().isEqual(hoje),
                encerrado,
                venceu,
                encerrado ? (jogo.getXpCreditado() == null ? 0 : jogo.getXpCreditado()) : null,
                jogo == null ? 0 : jogo.getTentativasUsadas());
    }

    private DesafioCifraAdminDTO montarAdmin(DesafioCifra desafio) {
        return new DesafioCifraAdminDTO(
                desafio.getId(),
                desafio.getAno(),
                desafio.getData(),
                desafio.getPalestra(),
                desafio.getSenhaCifrada(),
                desafio.getEtapas().stream()
                        .map(e -> new DesafioCifraEtapaAdminDTO(e.getOrdem(), e.getResposta(), e.getDica()))
                        .toList(),
                jogoRepository.countByDesafioId(desafio.getId()),
                jogoRepository.countByDesafioIdAndVenceuTrue(desafio.getId()));
    }

    private DesafioCifra buscarDesafio(Integer id) {
        return desafioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Desafio não encontrado."));
    }

    private Pessoa buscarPessoa(Integer pessoaId) {
        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    /* Tamanho de cada bloco de casas: "MAKE THEM BELIEVE" -> [4, 4, 7]. */
    private List<Integer> grupos(String resposta) {
        return Arrays.stream(resposta.trim().split("\\s+"))
                .map(bloco -> normalizar(bloco).length())
                .toList();
    }

    /* Sem acento, sem espaço e em maiúsculas. Os números sobrescritos (¹ ²)
       sobrevivem de propósito: a NFD não os decompõe, e na cifra de
       segunda "S²" e "S2" são símbolos diferentes. */
    private String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", "")
                .toUpperCase();
    }

    /* Mesmo critério de TermoService.creditarXp. */
    private void creditarXp(Pessoa pessoa, int xp) {
        int xpAtual = pessoa.getXp() == null ? 0 : pessoa.getXp();
        int novoXp = xpAtual + xp;
        pessoa.setXp(novoXp);
        nivelRepository.findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc(novoXp)
                .ifPresent(pessoa::setNivel);
        pessoaRepository.save(pessoa);
    }
}
