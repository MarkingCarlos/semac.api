package com.semac.java_api.service;

import com.semac.java_api.dto.*;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.TermoDia;
import com.semac.java_api.model.TermoJogo;
import com.semac.java_api.model.TermoPalavra;
import com.semac.java_api.model.TermoTentativa;
import com.semac.java_api.repository.NivelRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.TermoDiaRepository;
import com.semac.java_api.repository.TermoJogoRepository;
import com.semac.java_api.repository.TermoPalavraRepository;
import com.semac.java_api.repository.projection.ContagemJogosPalavraTermoView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/* Jogo do Termo (/termo).

   A regra que organiza esta classe: a palavra secreta não sai daqui. O
   navegador manda o palpite e recebe de volta só o padrão de cores; a
   palavra só entra numa resposta quando a partida já acabou, para a tela
   de fim revelar. Antes disso ela estava hardcoded no bundle JS e qualquer
   um a lia no DevTools — era esse o furo.

   Cada dia tem várias palavras (de 5 a 10 letras) e cada participante
   joga contra uma delas, sorteada na primeira vez que abre o jogo no dia
   (ver sortearJogo). O sorteio fica gravado no termo_jogo, então reabrir a
   página não troca a palavra.

   Consequência prática: quem confere o palpite é o servidor, e o limite de
   6 tentativas mora no banco (termo_jogo + termo_tentativa), não no
   navegador. F5 e aba anônima não devolvem tentativa; reabrir a página
   retoma a partida onde parou. */
@Service
public class TermoService {

    private static final Logger log = LoggerFactory.getLogger(TermoService.class);

    public static final int TOTAL_TENTATIVAS = 6;
    public static final int TOTAL_DIAS = 4;

    /* Padrão de cores, uma letra por posição. */
    private static final char CERTO = 'C';
    private static final char PRESENTE = 'P';
    private static final char AUSENTE = 'A';

    private final TermoDiaRepository diaRepository;
    private final TermoPalavraRepository palavraRepository;
    private final TermoJogoRepository jogoRepository;
    private final PessoaRepository pessoaRepository;
    private final NivelRepository nivelRepository;

    /* Xp de uma vitória, creditado uma vez por dia (unique pessoa+dia em
       termo_jogo). Vem da regra TERMO_ACERTO, editável no /admin ->
       Informações SEMAC; partidas já encerradas guardam em
       termo_jogo.xp_creditado o valor que valia na hora. */
    private final RegraXpService regraXpService;

    /* Vitória pode render a conquista LIGEIRINHO (primeiro do dia). */
    private final ConquistaService conquistaService;

    /* Dicionário de palpites aceitos. É só no servidor: com as palavras de
       6 a 10 letras ele ficou grande demais para ir no bundle. */
    private final DicionarioTermo dicionario;

    public TermoService(TermoDiaRepository diaRepository,
                        TermoPalavraRepository palavraRepository,
                        TermoJogoRepository jogoRepository,
                        PessoaRepository pessoaRepository,
                        NivelRepository nivelRepository,
                        RegraXpService regraXpService,
                        DicionarioTermo dicionario,
                        ConquistaService conquistaService) {
        this.diaRepository = diaRepository;
        this.palavraRepository = palavraRepository;
        this.jogoRepository = jogoRepository;
        this.pessoaRepository = pessoaRepository;
        this.nivelRepository = nivelRepository;
        this.regraXpService = regraXpService;
        this.dicionario = dicionario;
        this.conquistaService = conquistaService;
    }

    /* ── Jogador ─────────────────────────────────────────────────── */

    /* Estado do jogo de hoje: o que a tela precisa para se montar, já
       retomando a partida em andamento se houver.

       É aqui que a palavra é sorteada, na primeira abertura do dia: a
       grade precisa saber o tamanho dela antes do primeiro palpite. */
    @Transactional
    public TermoEstadoDTO estadoDeHoje(Integer pessoaId) {
        TermoDia dia = diaDeHoje();
        if (dia == null) {
            return indisponivel();
        }

        Pessoa pessoa = buscarPessoa(pessoaId);
        TermoJogo jogo = jogoRepository.findByPessoaIdAndDiaId(pessoa.getId(), dia.getId())
                .orElseGet(() -> sortearJogo(pessoa, dia));

        /* Dia com data mas ainda sem palavra cadastrada: para o jogador é
           o mesmo que não ter Termo hoje. */
        if (jogo == null) {
            return indisponivel();
        }
        return montarEstado(jogo);
    }

    /* Histórico dos dias de Termo que já aconteceram, com o que a pessoa
       fez em cada um — é o que a aba "Desafios" de /participantes desenha,
       um card por dia.

       Dia que passou continua na lista mesmo que a pessoa não tenha jogado
       (vem com `tentativasUsadas` 0): o card existe para mostrar que houve
       Termo naquele dia, e não só o que ela fez. Dias ainda por vir ficam
       de fora — quem está no dia 2 não precisa saber que o 3 existe.

       Esta é a única leitura do Termo que atravessa vários dias, e por isso
       a que mais pediria a palavra secreta de volta. Nenhuma sai:
       TermoDiaDTO não tem campo para ela. */
    @Transactional(readOnly = true)
    public List<TermoDiaDTO> listarDiasDoParticipante(Integer pessoaId) {
        LocalDate hoje = LocalDate.now();
        List<TermoDia> ocorridos =
                diaRepository.findByAnoAndDataLessThanEqualOrderByNumeroAsc(hoje.getYear(), hoje);
        if (ocorridos.isEmpty()) {
            return List.of();
        }

        Pessoa pessoa = buscarPessoa(pessoaId);
        Map<Integer, TermoJogo> jogosPorDia = jogoRepository
                .findByPessoaIdAndDiaIdIn(pessoa.getId(),
                        ocorridos.stream().map(TermoDia::getId).toList())
                .stream()
                /* Só existe um jogo por pessoa+dia (unique no banco), mas o
                   join das tentativas pode repetir a raiz — a função de
                   desempate fica para não trocar isso por um estouro. */
                .collect(Collectors.toMap(jogo -> jogo.getDia().getId(), jogo -> jogo,
                        (primeiro, repetido) -> primeiro));

        return ocorridos.stream()
                .map(dia -> montarDia(dia, jogosPorDia.get(dia.getId()), hoje))
                .toList();
    }

    /* Confere um palpite e gasta uma tentativa.

       Toda a decisão é aqui: se a palavra existe, se ainda há tentativa, o
       padrão de cores, se venceu e se credita xp. O cliente só desenha o
       que voltar. */
    @Transactional
    public TermoPalpiteRespostaDTO palpitar(Integer pessoaId, String palpiteBruto) {
        TermoDia dia = diaDeHoje();
        if (dia == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hoje não é dia de Termo.");
        }

        Pessoa pessoa = buscarPessoa(pessoaId);
        /* Normalmente o GET /hoje já sorteou; o orElseGet cobre quem chama
           o palpite direto. */
        TermoJogo jogo = jogoRepository.findByPessoaIdAndDiaId(pessoa.getId(), dia.getId())
                .orElseGet(() -> sortearJogo(pessoa, dia));
        if (jogo == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hoje não é dia de Termo.");
        }

        String secreta = normalizar(jogo.getPalavra().getPalavra());
        String palpite = normalizar(palpiteBruto);
        validarPalpite(palpite, secreta);

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
                        dia.getNumero(), pessoa.getNome(), pessoa.getId(), xpVitoria);
            } else {
                jogo.setXpCreditado(0);
            }
        }
        jogoRepository.save(jogo);
        if (encerrado && venceu) {
            conquistaService.avaliarLigeirinho(pessoa);
        }

        return new TermoPalpiteRespostaDTO(
                resultado,
                venceu,
                encerrado,
                TOTAL_TENTATIVAS - jogo.getTentativas().size(),
                encerrado ? (venceu ? xpVitoria : 0) : null,
                ehUltimoDia(dia),
                encerrado ? secreta : null);
    }

    /* Consulta ao dicionário para o modo treino do /termo, que confere o
       palpite no navegador mas não tem a lista. Não diz nada sobre a
       palavra secreta de ninguém — é o mesmo dicionário para todos. */
    public TermoPalavraValidaDTO validarPalavra(String palavraBruta) {
        return new TermoPalavraValidaDTO(dicionario.contem(normalizar(palavraBruta)));
    }

    /* ── Diretoria (/admin -> Termo) ─────────────────────────────── */

    /* Os quatro dias do ano, cada um com suas palavras, o tamanho delas e
       quantos participantes as receberam. Dia ainda não
       cadastrado aparece com data null, para a tela sempre ter os quatro
       cards. */
    @Transactional(readOnly = true)
    public List<TermoDiaAdminDTO> listarParaAdmin(Integer ano) {
        List<TermoDia> dias = diaRepository.findByAnoOrderByNumeroAsc(ano);
        List<TermoPalavra> palavras = dias.isEmpty() ? List.of()
                : palavraRepository.findByDiaIdInOrderByIdAsc(dias.stream().map(TermoDia::getId).toList());
        Map<Integer, Long> jogadores = contarJogadores(palavras.stream().map(TermoPalavra::getId).toList());

        return java.util.stream.IntStream.rangeClosed(1, TOTAL_DIAS)
                .mapToObj(numero -> dias.stream()
                        .filter(d -> d.getNumero().equals(numero))
                        .findFirst()
                        .map(d -> montarDiaAdmin(d,
                                palavras.stream().filter(p -> p.getDia().getId().equals(d.getId())).toList(),
                                jogadores))
                        .orElseGet(() -> new TermoDiaAdminDTO(numero, null, List.of())))
                .toList();
    }

    /* Cria o dia ou troca a data dele. As palavras ficam como estão. */
    @Transactional
    public TermoDiaAdminDTO salvarDia(Integer numero, TermoDiaRequestDTO dto) {
        validarNumeroDia(numero);

        TermoDia dia = diaRepository.findByAnoAndNumero(dto.ano(), numero)
                .orElseGet(TermoDia::new);

        /* Uma data só pode valer por um dia — é ela que resolve qual dia
           está ativo. Checar aqui evita um 500 pela constraint. */
        diaRepository.findByDataAndIdNot(dto.data(), dia.getId() == null ? -1 : dia.getId())
                .ifPresent(outro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "O dia " + outro.getNumero() + " já usa essa data.");
                });

        dia.setAno(dto.ano());
        dia.setNumero(numero);
        dia.setData(dto.data());
        TermoDia salvo = diaRepository.save(dia);
        log.info("Termo: dia {} marcado para {}.", salvo.getNumero(), salvo.getData());
        return montarDiaAdmin(salvo);
    }

    /* Acrescenta palavras ao conjunto de um dia. Repetida — com outra da
       mesma lista ou com uma já cadastrada no dia — recusa o lote inteiro:
       a mesma palavra duas vezes faria dois participantes jogarem a mesma
       coisa, que é o que o sorteio existe para evitar. */
    @Transactional
    public TermoDiaAdminDTO adicionarPalavras(Integer numero, TermoPalavraRequestDTO dto) {
        validarNumeroDia(numero);
        TermoDia dia = diaRepository.findByAnoAndNumero(dto.ano(), numero)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Defina a data do dia " + numero + " antes de cadastrar palavras."));

        Set<String> novas = new LinkedHashSet<>();
        for (String bruta : dto.palavras()) {
            String palavra = normalizar(bruta);
            if (!novas.add(palavra)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        palavra + " aparece mais de uma vez na lista.");
            }
        }

        List<TermoPalavra> existentes = palavraRepository.findByDiaIdAndPalavraIn(dia.getId(), novas);
        if (!existentes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    normalizar(existentes.get(0).getPalavra()) + " já está cadastrada no dia " + numero + ".");
        }

        palavraRepository.saveAll(novas.stream().map(palavra -> {
            TermoPalavra nova = new TermoPalavra();
            nova.setDia(dia);
            nova.setPalavra(palavra);
            return nova;
        }).toList());

        /* De propósito o log não imprime as palavras. */
        log.info("Termo: {} palavra(s) adicionada(s) ao dia {}.", novas.size(), numero);
        return montarDiaAdmin(dia);
    }

    /* Só sai palavra que ainda não foi sorteada para ninguém: o jogo da
       pessoa aponta para ela, e apagá-la levaria junto as tentativas. */
    @Transactional
    public TermoDiaAdminDTO removerPalavra(Integer palavraId) {
        TermoPalavra palavra = buscarPalavra(palavraId);
        if (jogoRepository.existsByPalavraId(palavraId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Essa palavra já foi sorteada para um participante e não pode ser removida.");
        }

        TermoDia dia = palavra.getDia();
        palavraRepository.delete(palavra);
        palavraRepository.flush();
        log.info("Termo: palavra {} removida do dia {}.", palavraId, dia.getNumero());
        return montarDiaAdmin(dia);
    }

    /* ── Modo teste (/admin -> Termo -> Testar) ──────────────────── */

    /* A diretoria joga contra uma palavra cadastrada para conferir se está
       tudo certo. Nada aqui grava: sem termo_jogo, sem tentativa e sem xp —
       a palavra continua com zero jogadores e removível. As 6 tentativas
       são contadas só na tela.

       A palavra não sai nem no fim: sem estado no servidor, revelá-la ao
       perder seria entregá-la a qualquer pedido. */
    @Transactional(readOnly = true)
    public TermoTesteDTO abrirTeste(Integer palavraId) {
        return new TermoTesteDTO(normalizar(buscarPalavra(palavraId).getPalavra()).length());
    }

    @Transactional(readOnly = true)
    public TermoPalpiteTesteRespostaDTO palpitarTeste(Integer palavraId, String palpiteBruto) {
        String secreta = normalizar(buscarPalavra(palavraId).getPalavra());
        String palpite = normalizar(palpiteBruto);
        validarPalpite(palpite, secreta);
        return new TermoPalpiteTesteRespostaDTO(calcularResultado(palpite, secreta), palpite.equals(secreta));
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    private TermoDia diaDeHoje() {
        return diaRepository.findByData(LocalDate.now()).orElse(null);
    }

    /* Sorteia a palavra da pessoa entre as do dia e grava o jogo.

       A escolhida é sempre uma das que têm menos jogadores; entre as
       empatadas, ao acaso. Enquanto houver palavra que ninguém recebeu,
       ninguém repete a palavra de outro; quando há mais gente que palavra,
       a repetição se espalha por igual em vez de cair toda numa só.

       A linha do dia fica travada do começo ao fim — ver
       TermoDiaRepository.buscarParaSorteio. null = o dia não tem palavra. */
    private TermoJogo sortearJogo(Pessoa pessoa, TermoDia dia) {
        TermoDia travado = diaRepository.buscarParaSorteio(dia.getId()).orElse(dia);

        /* Outra requisição da mesma pessoa pode ter sorteado enquanto esta
           esperava a trava. */
        TermoJogo existente = jogoRepository.findByPessoaIdAndDiaId(pessoa.getId(), travado.getId())
                .orElse(null);
        if (existente != null) {
            return existente;
        }

        List<TermoPalavra> palavras = palavraRepository.findByDiaIdOrderByIdAsc(travado.getId());
        if (palavras.isEmpty()) {
            return null;
        }

        Map<Integer, Long> jogadores = contarJogadores(palavras.stream().map(TermoPalavra::getId).toList());
        long menor = palavras.stream()
                .mapToLong(p -> jogadores.getOrDefault(p.getId(), 0L))
                .min()
                .orElse(0L);
        List<TermoPalavra> candidatas = palavras.stream()
                .filter(p -> jogadores.getOrDefault(p.getId(), 0L) == menor)
                .toList();
        TermoPalavra sorteada = candidatas.get(ThreadLocalRandom.current().nextInt(candidatas.size()));

        TermoJogo jogo = new TermoJogo();
        jogo.setPessoa(pessoa);
        jogo.setDia(travado);
        jogo.setPalavra(sorteada);
        return jogoRepository.save(jogo);
    }

    private Map<Integer, Long> contarJogadores(Collection<Integer> palavraIds) {
        if (palavraIds.isEmpty()) {
            return Map.of();
        }
        return jogoRepository.contarJogosPorPalavra(palavraIds).stream()
                .collect(Collectors.toMap(ContagemJogosPalavraTermoView::getPalavraId,
                        ContagemJogosPalavraTermoView::getTotal));
    }

    private TermoDiaAdminDTO montarDiaAdmin(TermoDia dia) {
        List<TermoPalavra> palavras = palavraRepository.findByDiaIdOrderByIdAsc(dia.getId());
        return montarDiaAdmin(dia, palavras,
                contarJogadores(palavras.stream().map(TermoPalavra::getId).toList()));
    }

    private TermoDiaAdminDTO montarDiaAdmin(TermoDia dia, List<TermoPalavra> palavras,
                                            Map<Integer, Long> jogadores) {
        return new TermoDiaAdminDTO(
                dia.getNumero(),
                dia.getData(),
                palavras.stream()
                        .map(p -> new TermoPalavraAdminDTO(p.getId(), p.getPalavra(), p.getPalavra().length(),
                                jogadores.getOrDefault(p.getId(), 0L)))
                        .toList());
    }

    private void validarNumeroDia(Integer numero) {
        if (numero < 1 || numero > TOTAL_DIAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O dia precisa ser de 1 a " + TOTAL_DIAS + ".");
        }
    }

    /* Último dia cadastrado no ano — o modal de derrota troca de texto
       ("não foi dessa vez" em vez de "volte amanhã"). */
    private boolean ehUltimoDia(TermoDia dia) {
        return diaRepository.findTopByAnoOrderByNumeroDesc(dia.getAno())
                .map(ultimo -> ultimo.getNumero().equals(dia.getNumero()))
                .orElse(true);
    }

    private boolean jogoEncerrado(TermoJogo jogo) {
        return jogo.getEncerradoEm() != null;
    }

    private TermoEstadoDTO indisponivel() {
        return new TermoEstadoDTO(false, null, false, null, List.of(), false, false, null, null);
    }

    private TermoEstadoDTO montarEstado(TermoJogo jogo) {
        TermoDia dia = jogo.getDia();
        String palavra = normalizar(jogo.getPalavra().getPalavra());
        boolean encerrado = jogoEncerrado(jogo);
        boolean venceu = Boolean.TRUE.equals(jogo.getVenceu());
        List<TermoTentativaDTO> tentativas = jogo.getTentativas().stream()
                .map(t -> new TermoTentativaDTO(t.getPalpite(), t.getResultado()))
                .toList();

        return new TermoEstadoDTO(
                true,
                dia.getNumero(),
                ehUltimoDia(dia),
                palavra.length(),
                tentativas,
                encerrado,
                venceu,
                encerrado ? (jogo.getXpCreditado() == null ? 0 : jogo.getXpCreditado()) : null,
                encerrado ? palavra : null);
    }

    /* Uma linha do histórico. `jogo` null = a pessoa não abriu o Termo
       daquele dia; o card ainda aparece, só que sem tentativa nenhuma. */
    private TermoDiaDTO montarDia(TermoDia dia, TermoJogo jogo, LocalDate hoje) {
        boolean encerrado = jogo != null && jogoEncerrado(jogo);
        boolean venceu = jogo != null && Boolean.TRUE.equals(jogo.getVenceu());
        return new TermoDiaDTO(
                dia.getNumero(),
                dia.getData(),
                dia.getData().isEqual(hoje),
                encerrado,
                venceu,
                venceu ? (jogo.getXpCreditado() == null ? 0 : jogo.getXpCreditado()) : null,
                jogo == null ? 0 : jogo.getTentativas().size());
    }

    private TermoPalavra buscarPalavra(Integer palavraId) {
        return palavraRepository.findById(palavraId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Palavra não encontrada."));
    }

    /* Recusas que não gastam tentativa: tamanho errado ou palavra fora do
       dicionário. A secreta sempre passa, mesmo fora do dicionário: "SEMAC"
       e "CYBER" não constam em dicionário nenhum e precisam ser enviáveis,
       senão não dá para acertar. */
    private void validarPalpite(String palpite, String secreta) {
        if (palpite.length() != secreta.length()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O palpite precisa ter " + secreta.length() + " letras.");
        }
        if (!palpite.equals(secreta) && !dicionario.contem(palpite)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Palavra não encontrada.");
        }
    }

    private Pessoa buscarPessoa(Integer pessoaId) {
        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    /* Padrão de cores do palpite. Duas passadas, como manda o jogo: os
       acertos de posição primeiro, e só depois as letras presentes fora de
       lugar consomem as ocorrências que sobraram — sem isso "AAABB" contra
       "ABCDE" marcaria três amarelos para um único A. Palpite e secreta
       têm o mesmo tamanho (palpitar confere antes). */
    private String calcularResultado(String palpite, String secreta) {
        int tamanho = secreta.length();
        char[] p = palpite.toCharArray();
        char[] s = secreta.toCharArray();
        char[] estado = new char[tamanho];
        boolean[] usados = new boolean[tamanho];

        for (int i = 0; i < tamanho; i++) {
            if (p[i] == s[i]) {
                estado[i] = CERTO;
                usados[i] = true;
            }
        }
        for (int i = 0; i < tamanho; i++) {
            if (estado[i] == CERTO) continue;
            estado[i] = AUSENTE;
            for (int j = 0; j < tamanho; j++) {
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
}
