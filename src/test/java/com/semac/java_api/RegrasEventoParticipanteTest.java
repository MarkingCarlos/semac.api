package com.semac.java_api;

import com.semac.java_api.dto.EventoRequestDTO;
import com.semac.java_api.dto.OperadorCheckinDTO;
import com.semac.java_api.dto.PresencaConfirmadaDTO;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.TipoEvento;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.model.pk.EventoParticipantePK;
import com.semac.java_api.repository.EventoPalestranteRepository;
import com.semac.java_api.repository.EventoParticipanteRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.NivelRepository;
import com.semac.java_api.repository.PalestranteRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.TipoEventoRepository;
import com.semac.java_api.repository.TrilhaRepository;
import com.semac.java_api.service.ConquistaService;
import com.semac.java_api.service.EventoService;
import com.semac.java_api.service.InscricaoEventoService;
import com.semac.java_api.service.RegraXpService;
import com.semac.java_api.service.TentativaCheckinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/* Responde duas perguntas de operacao exercitando o codigo de producao
   com repositorios falsos em memoria (sem banco):

   1. Palestra criada depois da confirmacao pega quem ja estava confirmado?
   2. Quando a janela de check-in abre, e quanto xp ela credita?

   Os prints deixam o resultado legivel no log do surefire. */
class RegrasEventoParticipanteTest {

    private static final int ID_PALESTRA = 99;
    private static final int ID_PALESTRA_DIA_SEGUINTE = 100;
    private static final int PONTOS_PALESTRA = 10;

    /* Espelha InscricaoEventoService.ANTECEDENCIA_MAXIMA_CHECKIN_MINUTOS,
       que e privada. Se um dia divergirem, estes testes acusam. */
    private static final int JANELA_CHECKIN_MINUTOS = 15;

    /* Banco falso de evento_participante. */
    private final Map<EventoParticipantePK, EventoParticipante> tabelaEventoParticipante = new LinkedHashMap<>();

    private EventoRepository eventoRepository;
    private EventoParticipanteRepository eventoParticipanteRepository;
    private PessoaRepository pessoaRepository;
    private NivelRepository nivelRepository;
    private ConquistaService conquistaService;
    private TentativaCheckinService tentativaCheckinService;
    private RegraXpService regraXpService;
    private TipoEventoRepository tipoEventoRepository;
    private TrilhaRepository trilhaRepository;
    private PalestranteRepository palestranteRepository;
    private EventoPalestranteRepository eventoPalestranteRepository;

    private InscricaoEventoService inscricaoEventoService;
    private EventoService eventoService;

    private List<Pessoa> participantesConfirmados;

    @BeforeEach
    void preparar() {
        tabelaEventoParticipante.clear();

        eventoRepository = mock(EventoRepository.class);
        eventoParticipanteRepository = mock(EventoParticipanteRepository.class);
        pessoaRepository = mock(PessoaRepository.class);
        nivelRepository = mock(NivelRepository.class);
        conquistaService = mock(ConquistaService.class);
        tentativaCheckinService = mock(TentativaCheckinService.class);
        regraXpService = mock(RegraXpService.class);
        tipoEventoRepository = mock(TipoEventoRepository.class);
        trilhaRepository = mock(TrilhaRepository.class);
        palestranteRepository = mock(PalestranteRepository.class);
        eventoPalestranteRepository = mock(EventoPalestranteRepository.class);

        /* evento_participante em memoria */
        when(eventoParticipanteRepository.save(any(EventoParticipante.class))).thenAnswer(chamada -> {
            EventoParticipante linha = chamada.getArgument(0);
            tabelaEventoParticipante.put(linha.getPk(), linha);
            return linha;
        });
        when(eventoParticipanteRepository.existsById(any())).thenAnswer(
                chamada -> tabelaEventoParticipante.containsKey(chamada.getArgument(0)));
        when(eventoParticipanteRepository.findById(any())).thenAnswer(
                chamada -> Optional.ofNullable(tabelaEventoParticipante.get(chamada.getArgument(0))));
        when(eventoParticipanteRepository.countByPk_EventoIdAndStatusIn(any(), any())).thenReturn(0L);

        /* Tres participantes ja confirmados antes de a palestra existir. */
        participantesConfirmados = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Pessoa pessoa = new Pessoa();
            pessoa.setId(i);
            pessoa.setNome("Participante " + i);
            pessoa.setEmail("p" + i + "@semac.cc");
            pessoa.setRole(Role.PARTICIPANTE);
            pessoa.setXp(0);
            participantesConfirmados.add(pessoa);
        }
        when(pessoaRepository.findAllByRole(Role.PARTICIPANTE)).thenReturn(participantesConfirmados);
        when(pessoaRepository.getReferenceById(any())).thenAnswer(
                chamada -> participantesConfirmados.get((Integer) chamada.getArgument(0) - 1));
        when(pessoaRepository.findById(any())).thenAnswer(
                chamada -> Optional.of(participantesConfirmados.get((Integer) chamada.getArgument(0) - 1)));

        when(eventoPalestranteRepository.findByPk_EventoId(any())).thenReturn(List.of());
        when(nivelRepository.findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc(anyInt()))
                .thenReturn(Optional.empty());

        /* Cortes de atraso: os padroes da V40. */
        when(regraXpService.atrasoMetadeMinutos()).thenReturn(20L);
        when(regraXpService.atrasoZeroMinutos()).thenReturn(30L);

        inscricaoEventoService = new InscricaoEventoService(eventoRepository, eventoParticipanteRepository,
                pessoaRepository, nivelRepository, conquistaService, tentativaCheckinService, regraXpService);
        eventoService = new EventoService(eventoRepository, tipoEventoRepository, trilhaRepository,
                palestranteRepository, eventoPalestranteRepository, inscricaoEventoService);
    }

    /* Pergunta 1: palestra criada depois da confirmacao */

    @Test
    void palestraCriadaDepoisPegaQuemJaEstavaConfirmado() {
        prepararCriacaoDeEvento(tipoPalestra());

        eventoService.criar(requisicaoDeEvento(1, LocalDateTime.now().plusDays(1)));

        System.out.println(">>> [P1] linhas em evento_participante apos criar a palestra: "
                + tabelaEventoParticipante.size() + " de " + participantesConfirmados.size()
                + " participantes confirmados");
        tabelaEventoParticipante.values().forEach(linha ->
                System.out.println(">>> [P1]   participante " + linha.getPk().getParticipanteId()
                        + " -> status " + linha.getStatus()));

        assertEquals(3, tabelaEventoParticipante.size());
        tabelaEventoParticipante.values()
                .forEach(linha -> assertEquals(StatusPresenca.INSCRITO, linha.getStatus()));
    }

    @Test
    void editarAPalestraNaoDuplicaAsPreInscricoes() {
        Evento palestra = prepararCriacaoDeEvento(tipoPalestra());
        eventoService.criar(requisicaoDeEvento(1, LocalDateTime.now().plusDays(1)));

        when(eventoRepository.findById(ID_PALESTRA)).thenReturn(Optional.of(palestra));
        eventoService.atualizar(ID_PALESTRA, requisicaoDeEvento(1, LocalDateTime.now().plusDays(2)));

        System.out.println(">>> [P1] apos editar o mesmo evento: " + tabelaEventoParticipante.size() + " linhas");
        assertEquals(3, tabelaEventoParticipante.size());
        verify(eventoParticipanteRepository, times(3)).save(any(EventoParticipante.class));
    }

    @Test
    void minicursoCriadoDepoisNaoPreInscreveNinguem() {
        prepararCriacaoDeEvento(tipoMinicurso());

        eventoService.criar(requisicaoDeEvento(2, LocalDateTime.now().plusDays(1)));

        System.out.println(">>> [P1] minicurso criado: " + tabelaEventoParticipante.size()
                + " linhas (esperado 0)");
        assertEquals(0, tabelaEventoParticipante.size());
    }

    /* Pergunta 2: janela de check-in */

    @Test
    void checkinExatamenteNaAberturaDaJanelaPassaEDaXpCheio() {
        Evento palestra = palestraComecandoEm(LocalDateTime.now().plusMinutes(JANELA_CHECKIN_MINUTOS));
        Pessoa participante = inscreverNaPalestra(palestra, 1);

        PresencaConfirmadaDTO resposta = inscricaoEventoService.registrarPresencaPorId(
                ID_PALESTRA, participante.getId(), operador());

        System.out.println(">>> [P2] check-in " + JANELA_CHECKIN_MINUTOS + "min antes: PASSOU | xpGanho="
                + resposta.xpGanho()
                + " | atrasoMinutos=" + resposta.atrasoMinutos()
                + " | xp total da pessoa=" + participante.getXp());

        assertEquals(PONTOS_PALESTRA, resposta.xpGanho());
        assertEquals(0L, resposta.atrasoMinutos());
        assertEquals(PONTOS_PALESTRA, participante.getXp());
        assertEquals(StatusPresenca.PRESENTE,
                tabelaEventoParticipante.get(new EventoParticipantePK(ID_PALESTRA, 1)).getStatus());
        verify(tentativaCheckinService, never()).registrar(any(), any(), any(), any(), anyLong());
    }

    @Test
    void checkinUmMinutoAntesDaJanelaEhRecusadoEFicaRegistrado() {
        Evento palestra = palestraComecandoEm(LocalDateTime.now().plusMinutes(JANELA_CHECKIN_MINUTOS + 1));
        Pessoa participante = inscreverNaPalestra(palestra, 1);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () ->
                inscricaoEventoService.registrarPresencaPorId(ID_PALESTRA, participante.getId(), operador()));

        System.out.println(">>> [P2] check-in " + (JANELA_CHECKIN_MINUTOS + 1) + "min antes: "
                + erro.getStatusCode() + " | " + erro.getReason()
                + " | xp da pessoa=" + participante.getXp());

        assertEquals(409, erro.getStatusCode().value());
        assertEquals(0, participante.getXp());
        assertEquals(StatusPresenca.INSCRITO,
                tabelaEventoParticipante.get(new EventoParticipantePK(ID_PALESTRA, 1)).getStatus());
        verify(tentativaCheckinService).registrar(any(), any(), any(), any(), anyLong());
    }

    /* O caso que motivou a mudanca: 8h num evento das 9h. */
    @Test
    void checkinUmaHoraAntesEhRecusado() {
        Evento palestra = palestraComecandoEm(LocalDateTime.now().plusMinutes(60));
        Pessoa participante = inscreverNaPalestra(palestra, 1);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () ->
                inscricaoEventoService.registrarPresencaPorId(ID_PALESTRA, participante.getId(), operador()));

        System.out.println(">>> [P2] check-in 1h antes: " + erro.getStatusCode()
                + " | " + erro.getReason());

        assertEquals(409, erro.getStatusCode().value());
        assertEquals(0, participante.getXp());
        assertEquals(StatusPresenca.INSCRITO,
                tabelaEventoParticipante.get(new EventoParticipantePK(ID_PALESTRA, 1)).getStatus());
    }

    /* Duas palestras no mesmo horario, em dias seguidos. Estando no dia 10,
       pouco antes da palestra do dia 10, o check-in da palestra do dia 11
       tem que ser recusado — e o do dia 10, aceito. */
    @Test
    void checkinNaoAlcancaAPalestraDoDiaSeguinte() {
        LocalDateTime dia10As9h = LocalDateTime.now().plusMinutes(5);
        LocalDateTime dia11As9h = dia10As9h.plusDays(1);

        Evento palestraDia10 = palestraComecandoEm(dia10As9h);
        Evento palestraDia11 = palestraComecandoEm(dia11As9h);
        palestraDia11.setId(ID_PALESTRA_DIA_SEGUINTE);
        palestraDia11.setNome("Palestra do dia seguinte");

        Pessoa participante = inscreverNaPalestra(palestraDia10, 1);
        inscreverNaPalestra(palestraDia11, 1);

        /* 1. A palestra de amanha esta fora da janela. */
        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () ->
                inscricaoEventoService.registrarPresencaPorId(
                        ID_PALESTRA_DIA_SEGUINTE, participante.getId(), operador()));

        ArgumentCaptor<Long> minutosAntes = ArgumentCaptor.forClass(Long.class);
        verify(tentativaCheckinService).registrar(any(), any(), any(), any(), minutosAntes.capture());

        System.out.println(">>> [P3] check-in na palestra de AMANHA: " + erro.getStatusCode()
                + " | " + erro.getReason()
                + " | minutosAntes registrado=" + minutosAntes.getValue()
                + " (~" + (minutosAntes.getValue() / 60) + "h)");

        assertEquals(409, erro.getStatusCode().value());
        assertEquals(0, participante.getXp());
        assertEquals(StatusPresenca.INSCRITO, tabelaEventoParticipante
                .get(new EventoParticipantePK(ID_PALESTRA_DIA_SEGUINTE, 1)).getStatus());

        /* Abertura de amanha = 24h + 5min - 15min de distancia. O log tem
           que registrar isso, e nao um "chegou cedo demais" qualquer. */
        assertTrue(minutosAntes.getValue() >= 1425 && minutosAntes.getValue() <= 1430,
                "minutosAntes fora do esperado: " + minutosAntes.getValue());

        /* 2. No mesmo instante, a palestra de hoje aceita (faltam 5min). */
        PresencaConfirmadaDTO resposta = inscricaoEventoService.registrarPresencaPorId(
                ID_PALESTRA, participante.getId(), operador());

        System.out.println(">>> [P3] check-in na palestra de HOJE, mesmo instante: PASSOU | xpGanho="
                + resposta.xpGanho());

        assertEquals(PONTOS_PALESTRA, resposta.xpGanho());
        assertEquals(StatusPresenca.PRESENTE,
                tabelaEventoParticipante.get(new EventoParticipantePK(ID_PALESTRA, 1)).getStatus());
        assertEquals(StatusPresenca.INSCRITO, tabelaEventoParticipante
                .get(new EventoParticipantePK(ID_PALESTRA_DIA_SEGUINTE, 1)).getStatus());
    }

    @Test
    void checkinNaHoraMarcadaTambemDaXpCheio() {
        Evento palestra = palestraComecandoEm(LocalDateTime.now());
        Pessoa participante = inscreverNaPalestra(palestra, 1);

        PresencaConfirmadaDTO resposta = inscricaoEventoService.registrarPresencaPorId(
                ID_PALESTRA, participante.getId(), operador());

        System.out.println(">>> [P2] check-in no horario marcado: xpGanho=" + resposta.xpGanho()
                + " | atrasoMinutos=" + resposta.atrasoMinutos());
        assertEquals(PONTOS_PALESTRA, resposta.xpGanho());
    }

    @Test
    void checkinNaAberturaIgnoraIniciarEventoClicadoCedo() {
        Evento palestra = palestraComecandoEm(LocalDateTime.now().plusMinutes(JANELA_CHECKIN_MINUTOS));
        palestra.setIniciadoEm(LocalDateTime.now().minusMinutes(5)); // clique adiantado
        Pessoa participante = inscreverNaPalestra(palestra, 1);

        PresencaConfirmadaDTO resposta = inscricaoEventoService.registrarPresencaPorId(
                ID_PALESTRA, participante.getId(), operador());

        System.out.println(">>> [P2] check-in na abertura com INICIAR EVENTO adiantado: xpGanho="
                + resposta.xpGanho() + " | atrasoMinutos=" + resposta.atrasoMinutos());
        assertEquals(PONTOS_PALESTRA, resposta.xpGanho());
        assertEquals(0L, resposta.atrasoMinutos());
    }

    /* Auxiliares */

    private TipoEvento tipoPalestra() {
        TipoEvento tipo = new TipoEvento();
        tipo.setId(1);
        tipo.setNome("Palestra");
        tipo.setPontos(PONTOS_PALESTRA);
        tipo.setExigeInscricao(false);
        return tipo;
    }

    private TipoEvento tipoMinicurso() {
        TipoEvento tipo = new TipoEvento();
        tipo.setId(2);
        tipo.setNome("Minicurso");
        tipo.setPontos(20);
        tipo.setExigeInscricao(true);
        return tipo;
    }

    /* Deixa o EventoRepository pronto para receber um evento novo com id
       fixo, como faria o IDENTITY do banco. Devolve uma instancia
       equivalente a linha gravada, para os testes de edicao. */
    private Evento prepararCriacaoDeEvento(TipoEvento tipo) {
        when(tipoEventoRepository.findById(tipo.getId())).thenReturn(Optional.of(tipo));
        Evento[] gravado = new Evento[1];
        when(eventoRepository.save(any(Evento.class))).thenAnswer(chamada -> {
            Evento evento = chamada.getArgument(0);
            evento.setId(ID_PALESTRA);
            gravado[0] = evento;
            return evento;
        });
        when(eventoRepository.getReferenceById(ID_PALESTRA)).thenAnswer(chamada -> gravado[0]);

        Evento referencia = new Evento();
        referencia.setId(ID_PALESTRA);
        referencia.setTipoEvento(tipo);
        referencia.setNome("Palestra de abertura");
        referencia.setDataHoraInicio(LocalDateTime.now().plusDays(1));
        referencia.setDataHoraFim(LocalDateTime.now().plusDays(1).plusHours(1));
        referencia.setCapacidadeMaxima(500);
        return referencia;
    }

    private EventoRequestDTO requisicaoDeEvento(Integer tipoEventoId, LocalDateTime inicio) {
        return new EventoRequestDTO("Palestra de abertura", tipoEventoId, "Auditorio", "Descricao",
                null, inicio, inicio.plusHours(1), 500, List.of());
    }

    private Evento palestraComecandoEm(LocalDateTime inicio) {
        Evento palestra = new Evento();
        palestra.setId(ID_PALESTRA);
        palestra.setNome("Palestra de abertura");
        palestra.setTipoEvento(tipoPalestra());
        palestra.setDataHoraInicio(inicio);
        palestra.setDataHoraFim(inicio.plusHours(1));
        palestra.setCapacidadeMaxima(500);
        return palestra;
    }

    private Pessoa inscreverNaPalestra(Evento palestra, int participanteId) {
        Pessoa participante = participantesConfirmados.get(participanteId - 1);
        EventoParticipante linha = new EventoParticipante();
        linha.setPk(new EventoParticipantePK(palestra.getId(), participanteId));
        linha.setEvento(palestra);
        linha.setParticipante(participante);
        linha.setStatus(StatusPresenca.INSCRITO);
        tabelaEventoParticipante.put(linha.getPk(), linha);
        return participante;
    }

    private OperadorCheckinDTO operador() {
        return new OperadorCheckinDTO(90, "Operador Teste", "DIRETOR_SITE");
    }
}
