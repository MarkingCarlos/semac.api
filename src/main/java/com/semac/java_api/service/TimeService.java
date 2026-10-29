package com.semac.java_api.service;

import com.semac.java_api.dto.MeuTimeDTO;
import com.semac.java_api.dto.MeuTimeResumoDTO;
import com.semac.java_api.dto.TimeAdminDTO;
import com.semac.java_api.dto.TimesPainelDTO;
import com.semac.java_api.model.ConfiguracaoInscricao;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.Time;
import com.semac.java_api.model.TimeIntegrante;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.ConfiguracaoInscricaoRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.TimeIntegranteRepository;
import com.semac.java_api.repository.TimeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/* Times da SEMAC: sorteio, revelação em /time e o cadastro editável
   (nome e mascote) do /admin, aba Times.

   O time é revelado ao participante por GET /api/time/meu, chamado pela
   página /time (link do QR code do cartão). O /participantes só mostra o
   time depois disso, por GET /api/time/meu/resumo (resumoMeuTime), que é
   só leitura e não revela nada sozinho. */
@Service
public class TimeService {

    private final SecureRandom aleatorio = new SecureRandom();

    private final TimeRepository timeRepository;
    private final TimeIntegranteRepository timeIntegranteRepository;
    private final PessoaRepository pessoaRepository;
    private final ConfiguracaoInscricaoRepository configuracaoInscricaoRepository;

    public TimeService(TimeRepository timeRepository,
                       TimeIntegranteRepository timeIntegranteRepository,
                       PessoaRepository pessoaRepository,
                       ConfiguracaoInscricaoRepository configuracaoInscricaoRepository) {
        this.timeRepository = timeRepository;
        this.timeIntegranteRepository = timeIntegranteRepository;
        this.pessoaRepository = pessoaRepository;
        this.configuracaoInscricaoRepository = configuracaoInscricaoRepository;
    }

    /* ── Participante (/time) ────────────────────────────────────── */

    /* Quem chegou depois do sorteio (pagamento confirmado mais tarde) entra
       aqui mesmo, no time com menos gente — assim os times seguem
       equilibrados sem precisar sortear de novo. Antes do primeiro sorteio
       ninguém é encaixado: a tela mostra "aguardando sorteio". */
    @Transactional
    public MeuTimeDTO meuTime(Integer pessoaId) {
        if (!timesAtivos()) {
            return MeuTimeDTO.desativado();
        }

        Optional<TimeIntegrante> existente = timeIntegranteRepository.findById(pessoaId);
        TimeIntegrante integrante;
        if (existente.isPresent()) {
            integrante = existente.get();
        } else {
            if (timeIntegranteRepository.count() == 0) {
                return MeuTimeDTO.aguardandoSorteio();
            }
            integrante = timeIntegranteRepository.save(
                    new TimeIntegrante(pessoaId, timeComMenosIntegrantes(), null));
        }

        boolean jaRevelado = integrante.getReveladoEm() != null;
        if (!jaRevelado) {
            integrante.setReveladoEm(LocalDateTime.now());
        }

        Time time = integrante.getTime();
        boolean oculto = rankingOculto();
        Map<Integer, Double> xpMedioPorTime = oculto ? Map.of() : xpMedioPorTime();
        return new MeuTimeDTO(
                true,
                true,
                jaRevelado,
                time.getId(),
                time.getNome(),
                time.getCor(),
                time.getMascoteArquivo(),
                timeIntegranteRepository.countByTime_Id(time.getId()),
                oculto ? 0 : posicaoDoTime(time.getId(), xpMedioPorTime),
                oculto ? 0 : xpMedioArredondado(time.getId(), xpMedioPorTime),
                oculto
        );
    }

    /* ── /admin ──────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public TimesPainelDTO painel() {
        Map<Integer, Double> xpMedioPorTime = xpMedioPorTime();
        List<TimeAdminDTO> times = timeRepository.findAllByOrderByIdAsc().stream()
                .map(t -> paraAdmin(t, xpMedioPorTime))
                .toList();
        LocalDateTime revelacaoProgramadaEm = configuracaoDoAno()
                .map(ConfiguracaoInscricao::getTimesRevelacaoEm)
                .orElse(null);
        return new TimesPainelDTO(
                configuracaoDoAno().map(ConfiguracaoInscricao::getTimesAtivos).orElse(Boolean.FALSE),
                revelacaoProgramadaEm,
                timesAtivos(),
                pessoaRepository.countByRoleAndAtivoTrue(Role.PARTICIPANTE),
                timeIntegranteRepository.count(),
                timeIntegranteRepository.countByReveladoEmIsNotNull(),
                times
        );
    }

    @Transactional
    public TimesPainelDTO definirAtivos(boolean ativos) {
        int ano = LocalDate.now().getYear();
        ConfiguracaoInscricao config = configuracaoInscricaoRepository.findByAno(ano)
                .orElseGet(() -> {
                    ConfiguracaoInscricao nova = new ConfiguracaoInscricao();
                    nova.setAno(ano);
                    nova.setInscricoesAbertas(Boolean.TRUE);
                    return nova;
                });
        config.setTimesAtivos(ativos);
        configuracaoInscricaoRepository.save(config);
        return painel();
    }

    @Transactional
    public TimesPainelDTO renomear(Integer id, String nome) {
        Time time = buscarOuFalhar(id);
        time.setNome(nome.trim().toUpperCase());
        timeRepository.save(time);
        return painel();
    }

    /* Grava o nome do novo arquivo e devolve o anterior, para o controller
       apagar do disco depois que o novo já valeu. */
    @Transactional
    public String definirMascote(Integer id, String nomeArquivo) {
        Time time = buscarOuFalhar(id);
        String anterior = time.getMascoteArquivo();
        time.setMascoteArquivo(nomeArquivo);
        timeRepository.save(time);
        return anterior;
    }

    /* Sorteio do zero: apaga a distribuição anterior, embaralha os
       participantes elegíveis e reparte em rodízio. A ordem dos times
       também é embaralhada, para que a sobra (quando o total não divide
       por 3) não caia sempre no mesmo time. Resultado: nenhum time fica
       com mais de 1 pessoa de diferença dos outros. */
    @Transactional
    public TimesPainelDTO sortear() {
        List<Time> times = new ArrayList<>(timeRepository.findAllByOrderByIdAsc());
        if (times.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nenhum time cadastrado.");
        }
        List<Pessoa> participantes = new ArrayList<>(pessoaRepository.findAllByRoleAndAtivoTrue(Role.PARTICIPANTE));
        if (participantes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nenhum participante com pagamento confirmado para sortear.");
        }

        Collections.shuffle(participantes, aleatorio);
        Collections.shuffle(times, aleatorio);

        timeIntegranteRepository.apagarTodos();
        timeIntegranteRepository.flush();

        List<TimeIntegrante> novos = new ArrayList<>(participantes.size());
        for (int i = 0; i < participantes.size(); i++) {
            novos.add(new TimeIntegrante(participantes.get(i).getId(), times.get(i % times.size()), null));
        }
        timeIntegranteRepository.saveAll(novos);
        return painel();
    }

    /* Bloco "Seu time" do /participantes: só para quem já abriu /time.
       Nada é gravado — quem ainda não foi revelado (ou nem tem time) recebe
       `revelado` false, e a revelação continua sendo só a do /time. */
    @Transactional(readOnly = true)
    public MeuTimeResumoDTO resumoMeuTime(Integer pessoaId) {
        if (!timesAtivos()) {
            return MeuTimeResumoDTO.naoRevelado();
        }
        Optional<TimeIntegrante> integrante = timeIntegranteRepository.findById(pessoaId)
                .filter(i -> i.getReveladoEm() != null);
        if (integrante.isEmpty()) {
            return MeuTimeResumoDTO.naoRevelado();
        }

        Time time = integrante.get().getTime();
        boolean oculto = rankingOculto();
        Map<Integer, Double> xpMedioPorTime = oculto ? Map.of() : xpMedioPorTime();
        return new MeuTimeResumoDTO(
                true,
                time.getId(),
                time.getNome(),
                time.getCor(),
                time.getMascoteArquivo(),
                oculto ? 0 : posicaoDoTime(time.getId(), xpMedioPorTime),
                oculto ? 0 : (int) timeRepository.count(),
                oculto ? 0 : xpMedioArredondado(time.getId(), xpMedioPorTime),
                oculto
        );
    }

    public Time buscarOuFalhar(Integer id) {
        return timeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time não encontrado."));
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    /* Liberados pelo interruptor do /admin ou, sem ele, a partir do
       horário da revelação programada (hora do servidor). Sem tarefa
       agendada: a primeira requisição depois do horário já vê liberado. */
    private boolean timesAtivos() {
        return configuracaoDoAno()
                .map(config -> Boolean.TRUE.equals(config.getTimesAtivos())
                        || revelacaoProgramadaJaPassou(config.getTimesRevelacaoEm()))
                .orElse(Boolean.FALSE);
    }

    /* Interruptor "Ocultar ranking" do /admin — vale para os times também.
       Ano sem configuração conta como visível. */
    private boolean rankingOculto() {
        return configuracaoDoAno()
                .map(config -> Boolean.TRUE.equals(config.getRankingOculto()))
                .orElse(Boolean.FALSE);
    }

    private static boolean revelacaoProgramadaJaPassou(LocalDateTime revelacaoEm) {
        return revelacaoEm != null && !LocalDateTime.now().isBefore(revelacaoEm);
    }

    private Optional<ConfiguracaoInscricao> configuracaoDoAno() {
        return configuracaoInscricaoRepository.findByAno(LocalDate.now().getYear());
    }

    /* Empate entre os menores é decidido no sorteio. */
    private Time timeComMenosIntegrantes() {
        List<Time> times = new ArrayList<>(timeRepository.findAllByOrderByIdAsc());
        Collections.shuffle(times, aleatorio);
        return times.stream()
                .min(Comparator.comparingLong(t -> timeIntegranteRepository.countByTime_Id(t.getId())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Nenhum time cadastrado."));
    }

    /* XP do time = soma do xp dos integrantes ÷ quantidade de integrantes.
       Média, e não soma, para o time com mais gente não sair na frente só
       por ser maior (quem chega depois do sorteio entra no time menor). */
    private Map<Integer, Double> xpMedioPorTime() {
        Map<Integer, Double> xpMedio = new HashMap<>();
        for (Object[] linha : timeIntegranteRepository.somarXpEContarIntegrantesPorTime()) {
            long soma = ((Number) linha[1]).longValue();
            long integrantes = ((Number) linha[2]).longValue();
            xpMedio.put((Integer) linha[0], integrantes == 0 ? 0.0 : (double) soma / integrantes);
        }
        return xpMedio;
    }

    /* A tela mostra a média arredondada; a posição usa a média exata, para
       não empatar times que só ficam iguais depois de arredondar. */
    private long xpMedioArredondado(Integer timeId, Map<Integer, Double> xpMedioPorTime) {
        return Math.round(xpMedioPorTime.getOrDefault(timeId, 0.0));
    }

    /* Posição pela média de xp: 1 + quantos times têm média maior. Times
       empatados dividem a posição. */
    private int posicaoDoTime(Integer timeId, Map<Integer, Double> xpMedioPorTime) {
        double xpDoTime = xpMedioPorTime.getOrDefault(timeId, 0.0);
        long acima = timeRepository.findAllByOrderByIdAsc().stream()
                .filter(t -> xpMedioPorTime.getOrDefault(t.getId(), 0.0) > xpDoTime)
                .count();
        return (int) acima + 1;
    }

    private TimeAdminDTO paraAdmin(Time time, Map<Integer, Double> xpMedioPorTime) {
        return new TimeAdminDTO(
                time.getId(),
                time.getNome(),
                time.getCor(),
                time.getMascoteArquivo(),
                timeIntegranteRepository.countByTime_Id(time.getId()),
                timeIntegranteRepository.countByTime_IdAndReveladoEmIsNotNull(time.getId()),
                xpMedioArredondado(time.getId(), xpMedioPorTime)
        );
    }
}
