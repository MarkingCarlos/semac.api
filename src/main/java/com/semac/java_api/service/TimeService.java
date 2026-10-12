package com.semac.java_api.service;

import com.semac.java_api.dto.MeuTimeDTO;
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

   O time só é exposto ao participante por GET /api/time/meu, chamado
   pela página /time (link do QR code do cartão). O /participantes não
   mostra o time de propósito. */
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
        Map<Integer, Long> xpPorTime = xpPorTime();
        return new MeuTimeDTO(
                true,
                true,
                jaRevelado,
                time.getId(),
                time.getNome(),
                time.getCor(),
                time.getMascoteArquivo(),
                timeIntegranteRepository.countByTime_Id(time.getId()),
                posicaoDoTime(time.getId(), xpPorTime),
                xpPorTime.getOrDefault(time.getId(), 0L)
        );
    }

    /* ── /admin ──────────────────────────────────────────────────── */

    @Transactional(readOnly = true)
    public TimesPainelDTO painel() {
        Map<Integer, Long> xpPorTime = xpPorTime();
        List<TimeAdminDTO> times = timeRepository.findAllByOrderByIdAsc().stream()
                .map(t -> paraAdmin(t, xpPorTime))
                .toList();
        return new TimesPainelDTO(
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

    public Time buscarOuFalhar(Integer id) {
        return timeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time não encontrado."));
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    private boolean timesAtivos() {
        return configuracaoInscricaoRepository.findByAno(LocalDate.now().getYear())
                .map(ConfiguracaoInscricao::getTimesAtivos)
                .orElse(Boolean.FALSE);
    }

    /* Empate entre os menores é decidido no sorteio. */
    private Time timeComMenosIntegrantes() {
        List<Time> times = new ArrayList<>(timeRepository.findAllByOrderByIdAsc());
        Collections.shuffle(times, aleatorio);
        return times.stream()
                .min(Comparator.comparingLong(t -> timeIntegranteRepository.countByTime_Id(t.getId())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Nenhum time cadastrado."));
    }

    private Map<Integer, Long> xpPorTime() {
        Map<Integer, Long> xp = new HashMap<>();
        for (Object[] linha : timeIntegranteRepository.somarXpPorTime()) {
            xp.put((Integer) linha[0], ((Number) linha[1]).longValue());
        }
        return xp;
    }

    /* Posição pela soma de xp: 1 + quantos times têm soma maior. Times
       empatados dividem a posição. */
    private int posicaoDoTime(Integer timeId, Map<Integer, Long> xpPorTime) {
        long xpDoTime = xpPorTime.getOrDefault(timeId, 0L);
        long acima = timeRepository.findAllByOrderByIdAsc().stream()
                .filter(t -> xpPorTime.getOrDefault(t.getId(), 0L) > xpDoTime)
                .count();
        return (int) acima + 1;
    }

    private TimeAdminDTO paraAdmin(Time time, Map<Integer, Long> xpPorTime) {
        return new TimeAdminDTO(
                time.getId(),
                time.getNome(),
                time.getCor(),
                time.getMascoteArquivo(),
                timeIntegranteRepository.countByTime_Id(time.getId()),
                timeIntegranteRepository.countByTime_IdAndReveladoEmIsNotNull(time.getId()),
                xpPorTime.getOrDefault(time.getId(), 0L)
        );
    }
}
