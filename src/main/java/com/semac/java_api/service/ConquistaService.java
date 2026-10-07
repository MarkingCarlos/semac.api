package com.semac.java_api.service;

import com.semac.java_api.dto.ConcederConquistaRequestDTO;
import com.semac.java_api.dto.ConquistaConcedidaDTO;
import com.semac.java_api.dto.ConquistaDoParticipanteDTO;
import com.semac.java_api.dto.ConquistaParticipanteDTO;
import com.semac.java_api.dto.ConquistaRequestDTO;
import com.semac.java_api.dto.ConquistaResponseDTO;
import com.semac.java_api.config.CatalogoConquistas;
import com.semac.java_api.model.Conquista;
import com.semac.java_api.model.Evento;
import com.semac.java_api.model.EventoParticipante;
import com.semac.java_api.model.ParticipanteConquista;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.CodigoTipoEvento;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.model.enums.TipoValidacaoConquista;
import com.semac.java_api.model.enums.StatusPresenca;
import com.semac.java_api.model.pk.ParticipanteConquistaPK;
import com.semac.java_api.repository.ConquistaRepository;
import com.semac.java_api.repository.EventoParticipanteRepository;
import com.semac.java_api.repository.EventoRepository;
import com.semac.java_api.repository.NivelRepository;
import com.semac.java_api.repository.ParticipanteConquistaRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/* Concessão e revogação de conquistas.

   O catálogo é declarado em CatalogoConquistas e materializado pelo
   ConquistaSeedRunner; aqui ficam as operações sobre ele. As regras das
   conquistas automáticas — cada uma um método próprio, chamado do ponto
   certo, sem motor de regras genérico — entram numa etapa seguinte.

   Xp de conquista segue o mesmo caminho do xp de presença (ver
   InscricaoEventoService.creditarXp): soma em pessoa.xp e recalcula o
   nível. A diferença é que aqui o valor creditado fica gravado no vínculo
   (ParticipanteConquista.xpCreditado), porque conquista pode ser revogada
   e o estorno precisa devolver o que foi dado de fato — não o que o
   catálogo vale hoje. */
@Service
public class ConquistaService {

    private static final Logger log = LoggerFactory.getLogger(ConquistaService.class);

    private final PessoaRepository pessoaRepository;
    private final ConquistaRepository conquistaRepository;
    private final ParticipanteConquistaRepository participanteConquistaRepository;
    private final NivelRepository nivelRepository;
    private final EventoParticipanteRepository eventoParticipanteRepository;
    private final EventoRepository eventoRepository;

    public ConquistaService(PessoaRepository pessoaRepository,
                            ConquistaRepository conquistaRepository,
                            ParticipanteConquistaRepository participanteConquistaRepository,
                            NivelRepository nivelRepository,
                            EventoParticipanteRepository eventoParticipanteRepository,
                            EventoRepository eventoRepository) {
        this.pessoaRepository = pessoaRepository;
        this.conquistaRepository = conquistaRepository;
        this.participanteConquistaRepository = participanteConquistaRepository;
        this.nivelRepository = nivelRepository;
        this.eventoParticipanteRepository = eventoParticipanteRepository;
        this.eventoRepository = eventoRepository;
    }

    /* ── Catálogo (/admin -> Informações SEMAC) ──────────────────── */

    @Transactional(readOnly = true)
    public List<ConquistaResponseDTO> listarCatalogo() {
        return conquistaRepository.findAllByOrderByOrdemAscIdAsc().stream()
                .map(this::paraResposta)
                .toList();
    }

    /* Edita só o que pertence ao /admin. `codigo` e `tipoValidacao` não
       entram: são do código e o seeder os sincroniza no boot. */
    @Transactional
    public ConquistaResponseDTO atualizar(Integer id, ConquistaRequestDTO dto) {
        Conquista conquista = buscarOuFalhar(id);
        conquista.setNome(dto.nome());
        conquista.setDescricao(dto.descricao());
        conquista.setPontosBase(dto.pontosBase());
        conquista.setRaridade(dto.raridade());
        conquista.setOrdem(dto.ordem());
        return paraResposta(conquistaRepository.save(conquista));
    }

    /* O interruptor da presidência, com as duas travas combinadas:

       - ativar sem imagem é barrado porque o card do participante vive da
         imagem (colorida quando conquistada, preto e branco quando não) —
         sem ela a vitrine sai quebrada;

       - desativar com gente vinculada é barrado porque os pontos já foram
         creditados em pessoa.xp e mexem no ranking. Para destravar,
         revoga-se de quem tem (ver revogar) e então desativa. */
    @Transactional
    public ConquistaResponseDTO alterarAtiva(Integer id, boolean ativa) {
        Conquista conquista = buscarOuFalhar(id);

        if (ativa && !temImagem(conquista)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Envie uma imagem antes de ativar esta conquista.");
        }

        if (!ativa) {
            long jaConquistaram = participanteConquistaRepository.countByPk_ConquistaId(id);
            if (jaConquistaram > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        jaConquistaram == 1
                                ? "1 participante já tem esta conquista. Revogue antes de desativar."
                                : jaConquistaram + " participantes já têm esta conquista. Revogue antes de desativar.");
            }
        }

        conquista.setAtiva(ativa);
        return paraResposta(conquistaRepository.save(conquista));
    }

    @Transactional
    public ConquistaResponseDTO definirImagem(Integer id, String nomeArquivo) {
        Conquista conquista = buscarOuFalhar(id);
        conquista.setImagemUrl(nomeArquivo);
        return paraResposta(conquistaRepository.save(conquista));
    }

    @Transactional(readOnly = true)
    public Conquista buscarOuFalhar(Integer id) {
        return conquistaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conquista não encontrada."));
    }

    /* ── Vitrine do participante ─────────────────────────────────── */

    /* As conquistas ativas que fazem sentido para esta pessoa, marcando
       quais ela já tem. As bloqueadas vêm junto de propósito: é o card em
       preto e branco, com a descrição servindo de meta.

       A exceção é "Minicurso Concluído" para quem não está inscrito em
       minicurso nenhum: a meta é inalcançável para essa pessoa, então o
       card sai da vitrine. Quem já tem a conquista continua vendo, mesmo
       que tenha saído das inscrições depois — o xp dela já foi creditado
       e o card é o que explica de onde ele veio. */
    @Transactional(readOnly = true)
    public List<ConquistaParticipanteDTO> listarDoParticipante(Integer participanteId) {
        Map<Integer, ParticipanteConquista> obtidas = new HashMap<>();
        for (ParticipanteConquista vinculo : participanteConquistaRepository.findByPk_ParticipanteId(participanteId)) {
            obtidas.put(vinculo.getPk().getConquistaId(), vinculo);
        }

        boolean inscritoEmMinicurso = eventoParticipanteRepository.buscarComEventoPorParticipante(participanteId)
                .stream()
                .anyMatch(vinculo -> exigeInscricao(vinculo.getEvento()));

        return conquistaRepository.findByAtivaTrueOrderByOrdemAscIdAsc().stream()
                .filter(conquista -> inscritoEmMinicurso
                        || obtidas.containsKey(conquista.getId())
                        || !CatalogoConquistas.CODIGO_MINICURSO_CONCLUIDO.equals(conquista.getCodigo()))
                .map(conquista -> {
                    ParticipanteConquista vinculo = obtidas.get(conquista.getId());
                    return new ConquistaParticipanteDTO(
                            conquista.getId(),
                            conquista.getNome(),
                            conquista.getDescricao(),
                            conquista.getPontosBase(),
                            conquista.getRaridade(),
                            conquista.getOrdem(),
                            conquista.getImagemUrl(),
                            vinculo != null,
                            vinculo == null ? null : vinculo.getObtidaEm(),
                            vinculo != null && vinculo.getVistaEm() == null);
                })
                .toList();
    }

    /* Registra que a animação desta conquista já foi exibida — é o que
       impede que ela apareça de novo na próxima abertura.

       Marcada uma a uma, quando a animação daquela conquista termina, e
       não no fim da fila: se a pessoa fechar o app no meio, as já
       celebradas não voltam e as que faltam ainda aparecem.

       Idempotente e silenciosa: marcar algo que já está marcado, ou que a
       pessoa nem tem, não é erro. O front chama isto no fim de uma
       animação e não tem o que fazer com uma falha. */
    @Transactional
    public void marcarComoVista(Integer participanteId, Integer conquistaId) {
        participanteConquistaRepository
                .findById(new ParticipanteConquistaPK(participanteId, conquistaId))
                .filter(vinculo -> vinculo.getVistaEm() == null)
                .ifPresent(vinculo -> {
                    vinculo.setVistaEm(LocalDateTime.now());
                    participanteConquistaRepository.save(vinculo);
                });
    }

    /* ── Regras automáticas ──────────────────────────────────────── */

    /* O conceito de falta não existe no banco: `StatusPresenca.AUSENTE`
       nunca é gravado por ninguém (não há job de fim de dia, apesar do que
       dizem alguns comentários antigos). Então INSCRITO significa ao mesmo
       tempo "ainda vai acontecer" e "faltou".

       Todas as regras abaixo resolvem isso pelo relógio: só olham eventos
       cujo `dataHoraFim` já passou. Num evento encerrado, PRESENTE é
       presença e qualquer outro status é falta. Assim as regras funcionam
       sem depender de rotina noturna nenhuma. */

    /* Tipos de evento abertos cuja presença conta para Presença Total e
       Dia Cheio. Ficam de fora o coffee-break e o credenciamento (onde a
       pessoa ainda está pegando o kit). Minicursos entram à parte, pelos
       vínculos: só contam os que a própria pessoa escolheu. */
    private static final Set<CodigoTipoEvento> TIPOS_QUE_CONTAM_PRESENCA = EnumSet.of(
            CodigoTipoEvento.ABERTURA,
            CodigoTipoEvento.ENCERRAMENTO,
            CodigoTipoEvento.PALESTRA,
            CodigoTipoEvento.MESA_REDONDA,
            CodigoTipoEvento.DEBATE,
            CodigoTipoEvento.MOSTRA_TECNICA,
            CodigoTipoEvento.ATIVIDADES_NOTURNAS);

    /* Reavalia as conquistas automáticas de um participante. Chamada a cada
       check-in (o momento em que o quadro dele muda), no boot e pelo botão
       "Reavaliar" do /admin. Idempotente: conceder() ignora quem já tem,
       então rodar de novo não duplica nem recredita. */
    @Transactional
    public void reavaliarAutomaticas(Pessoa participante) {
        if (participante == null || participante.getRole() != Role.PARTICIPANTE) {
            return;
        }

        List<EventoParticipante> vinculos =
                eventoParticipanteRepository.buscarComEventoPorParticipante(participante.getId());
        List<Evento> catalogoQueConta = eventoRepository.buscarComTipoPorCodigos(TIPOS_QUE_CONTAM_PRESENCA).stream()
                .filter(evento -> !exigeInscricao(evento))
                .toList();
        LocalDateTime agora = LocalDateTime.now();

        avaliarPresencaTotal(participante, vinculos, catalogoQueConta, agora);
        avaliarDiaCompleto(participante, vinculos, catalogoQueConta, agora);
        avaliarMinicursoConcluido(participante, vinculos, agora);
        avaliarAtividadesNoturnas(participante, vinculos, catalogoQueConta, agora);
        avaliarAbertura(participante, vinculos, catalogoQueConta);
        avaliarCoffeeBreaks(participante, vinculos, agora);
        avaliarDebates(participante, vinculos, catalogoQueConta, agora);
    }

    /* Passa por todo participante confirmado. Usada no boot e pelo botão
       do /admin — é o que faz uma conquista recém-ativada alcançar quem já
       tinha cumprido a regra antes de ela existir. Devolve quantos foram
       avaliados. */
    @Transactional
    public int reavaliarAutomaticasDeTodos() {
        List<Pessoa> participantes = pessoaRepository.findAllByRole(Role.PARTICIPANTE);
        for (Pessoa participante : participantes) {
            reavaliarAutomaticas(participante);
        }
        log.info("Conquistas automáticas reavaliadas para {} participante(s).", participantes.size());
        return participantes.size();
    }

    /* "Presença Total": presente em TODO evento dos TIPOS_QUE_CONTAM_PRESENCA
       da semana, mais todos os minicursos que a pessoa escolheu. Só é
       concedida quando todos eles já terminaram — antes disso "todos" ainda
       não aconteceu, e quem foi a tudo no primeiro dia não pode levar.

       O denominador dos eventos abertos é o catálogo, não a lista de
       vínculos da pessoa. A diferença importa: se alguém não tem linha em
       `evento_participante` para uma palestra — por pré-inscrição que não
       rodou, vínculo removido à mão, seja o que for — contar só os
       vínculos daria a conquista a quem foi a dois minicursos e a nenhuma
       palestra. Faltar e não constar têm que pesar igual.

       Quem confirmou a inscrição no meio da semana não leva: as palestras
       anteriores continuam no denominador. É o critério combinado — vale
       "compareceu a tudo", e não "a tudo desde que chegou".

       O guard de lista vazia importa: sem ele, "todos" seria verdade por
       vacuidade e a base inteira ganharia a conquista. */
    private void avaliarPresencaTotal(Pessoa participante, List<EventoParticipante> vinculos,
                                      List<Evento> catalogoQueConta, LocalDateTime agora) {
        List<Evento> exigidos = new ArrayList<>(catalogoQueConta);
        exigidos.addAll(minicursosEscolhidos(vinculos));

        if (exigidos.isEmpty() || !todosEncerrados(exigidos, agora)) {
            return;
        }
        if (compareceuATodos(exigidos, presencas(vinculos))) {
            conceder(participante, CatalogoConquistas.CODIGO_PRESENCA_TOTAL);
        }
    }

    /* "Dia Cheio": existe pelo menos um dia, já inteiramente encerrado, em
       que a pessoa compareceu a tudo o que contava naquele dia — os eventos
       dos TIPOS_QUE_CONTAM_PRESENCA mais os minicursos que escolheu. Basta
       um dia assim. Mesmo denominador por catálogo da Presença Total. */
    private void avaliarDiaCompleto(Pessoa participante, List<EventoParticipante> vinculos,
                                    List<Evento> catalogoQueConta, LocalDateTime agora) {
        List<Evento> exigidos = new ArrayList<>(catalogoQueConta);
        exigidos.addAll(minicursosEscolhidos(vinculos));

        Map<LocalDate, List<Evento>> porDia = new LinkedHashMap<>();
        for (Evento evento : exigidos) {
            porDia.computeIfAbsent(evento.getDataHoraInicio().toLocalDate(), d -> new ArrayList<>()).add(evento);
        }

        Set<Integer> presencas = presencas(vinculos);
        boolean temDiaCheio = porDia.values().stream()
                .anyMatch(doDia -> todosEncerrados(doDia, agora) && compareceuATodos(doDia, presencas));
        if (temDiaCheio) {
            conceder(participante, CatalogoConquistas.CODIGO_DIA_COMPLETO);
        }
    }

    /* "Minicurso Concluído": compareceu a todos os encontros de algum
       minicurso em que se inscreveu.

       Heurística consciente: não existe no banco nada que ligue os
       encontros de um mesmo minicurso — cada encontro é um `evento`
       próprio e o participante se inscreve em cada um (ver
       InscricaoEventoService). O nome é o único vínculo disponível, então
       é por ele que se agrupa. Minicurso de um encontro só cai no mesmo
       caminho, com um grupo de tamanho 1. Se algum dia surgir um campo de
       série/turma, é aqui que se troca. */
    private void avaliarMinicursoConcluido(Pessoa participante, List<EventoParticipante> vinculos, LocalDateTime agora) {
        Map<String, List<EventoParticipante>> porMinicurso = new LinkedHashMap<>();
        for (EventoParticipante vinculo : vinculos) {
            if (!exigeInscricao(vinculo.getEvento())) {
                continue;
            }
            porMinicurso.computeIfAbsent(vinculo.getEvento().getNome(), n -> new ArrayList<>()).add(vinculo);
        }

        boolean concluiuAlgum = porMinicurso.values().stream()
                .anyMatch(encontros ->
                        encontros.stream().allMatch(v -> jaEncerrou(v.getEvento(), agora))
                                && encontros.stream().allMatch(this::compareceu));
        if (concluiuAlgum) {
            conceder(participante, CatalogoConquistas.CODIGO_MINICURSO_CONCLUIDO);
        }
    }

    /* "Criatura das Trevas": presente em todas as atividades noturnas da
       SEMAC. Mesmo critério da Presença Total, restrito a esse tipo:
       denominador pelo catálogo, concedida só quando a última já terminou,
       e ninguém leva se não houver nenhuma cadastrada. */
    private void avaliarAtividadesNoturnas(Pessoa participante, List<EventoParticipante> vinculos,
                                           List<Evento> catalogoQueConta, LocalDateTime agora) {
        List<Evento> noturnas = catalogoQueConta.stream()
                .filter(evento -> evento.getTipoEvento().getCodigo() == CodigoTipoEvento.ATIVIDADES_NOTURNAS)
                .toList();

        if (noturnas.isEmpty() || !todosEncerrados(noturnas, agora)) {
            return;
        }
        if (compareceuATodos(noturnas, presencas(vinculos))) {
            conceder(participante, CatalogoConquistas.CODIGO_ATIVIDADES_NOTURNAS);
        }
    }

    /* "E lá vamos nós": presente na abertura da SEMAC. Exceção à regra do
       relógio das demais: aqui não há falta a descobrir, só presença a
       confirmar — então sai no próprio check-in, sem esperar a abertura
       terminar. Havendo mais de uma abertura cadastrada, vale estar em
       todas. Nenhuma cadastrada, ninguém leva. */
    private void avaliarAbertura(Pessoa participante, List<EventoParticipante> vinculos,
                                 List<Evento> catalogoQueConta) {
        List<Evento> aberturas = catalogoQueConta.stream()
                .filter(evento -> evento.getTipoEvento().getCodigo() == CodigoTipoEvento.ABERTURA)
                .toList();

        if (!aberturas.isEmpty() && compareceuATodos(aberturas, presencas(vinculos))) {
            conceder(participante, CatalogoConquistas.CODIGO_ABERTURA_PRESENTE);
        }
    }

    /* "Café com Java": presente em todos os coffee breaks da SEMAC. Mesmo
       critério da Criatura das Trevas — denominador pelo catálogo,
       concedida só quando o último já terminou, ninguém leva se não houver
       nenhum cadastrado. Coffee-break não está em TIPOS_QUE_CONTAM_PRESENCA
       (não pesa na Presença Total nem no Dia Cheio), por isso a consulta
       própria. */
    private void avaliarCoffeeBreaks(Pessoa participante, List<EventoParticipante> vinculos, LocalDateTime agora) {
        List<Evento> coffeeBreaks = eventoRepository.buscarComTipoPorCodigos(EnumSet.of(CodigoTipoEvento.COFFEE_BREAK));

        if (coffeeBreaks.isEmpty() || !todosEncerrados(coffeeBreaks, agora)) {
            return;
        }
        if (compareceuATodos(coffeeBreaks, presencas(vinculos))) {
            conceder(participante, CatalogoConquistas.CODIGO_COFFEE_BREAK_COMPLETO);
        }
    }

    /* "Treta Saudável": presente em todos os debates da SEMAC. Mesmo
       critério da Criatura das Trevas — denominador pelo catálogo,
       concedida só quando o último já terminou, ninguém leva se não houver
       nenhum cadastrado. */
    private void avaliarDebates(Pessoa participante, List<EventoParticipante> vinculos,
                                List<Evento> catalogoQueConta, LocalDateTime agora) {
        List<Evento> debates = catalogoQueConta.stream()
                .filter(evento -> evento.getTipoEvento().getCodigo() == CodigoTipoEvento.DEBATE)
                .toList();

        if (debates.isEmpty() || !todosEncerrados(debates, agora)) {
            return;
        }
        if (compareceuATodos(debates, presencas(vinculos))) {
            conceder(participante, CatalogoConquistas.CODIGO_DEBATES_COMPLETO);
        }
    }

    private List<Evento> minicursosEscolhidos(List<EventoParticipante> vinculos) {
        return vinculos.stream()
                .map(EventoParticipante::getEvento)
                .filter(this::exigeInscricao)
                .toList();
    }

    private Set<Integer> presencas(List<EventoParticipante> vinculos) {
        return vinculos.stream()
                .filter(this::compareceu)
                .map(vinculo -> vinculo.getEvento().getId())
                .collect(Collectors.toSet());
    }

    private boolean todosEncerrados(List<Evento> eventos, LocalDateTime agora) {
        return eventos.stream().allMatch(evento -> jaEncerrou(evento, agora));
    }

    private boolean compareceuATodos(List<Evento> eventos, Set<Integer> presencas) {
        return eventos.stream().allMatch(evento -> presencas.contains(evento.getId()));
    }

    private boolean jaEncerrou(Evento evento, LocalDateTime agora) {
        return evento.getDataHoraFim() != null && evento.getDataHoraFim().isBefore(agora);
    }

    /* Check-in atrasado credita 0 xp mas registra PRESENTE (ver
       InscricaoEventoService.marcarPresente) — e para conquista conta como
       presença: a pessoa esteve lá. */
    private boolean compareceu(EventoParticipante vinculo) {
        return vinculo.getStatus() == StatusPresenca.PRESENTE;
    }

    private boolean exigeInscricao(Evento evento) {
        return evento.getTipoEvento() != null
                && Boolean.TRUE.equals(evento.getTipoEvento().getExigeInscricao());
    }

    /* ── Concessão e revogação ───────────────────────────────────── */

    /* Concede pelo código do catálogo — é por aqui que as regras
       automáticas vão conceder. Idempotente e tolerante: se a conquista
       ainda não existe no banco ou está inativa, não faz nada. Devolve se
       a conquista foi de fato concedida agora. */
    @Transactional
    public boolean conceder(Pessoa pessoa, String codigo) {
        Conquista conquista = conquistaRepository.findByCodigo(codigo).orElse(null);
        if (conquista == null) {
            log.warn("Conquista de código '{}' não encontrada no catálogo — pulando concessão.", codigo);
            return false;
        }
        return conceder(pessoa, conquista, null);
    }

    /* Vincula a conquista à pessoa (se ainda não tiver) e credita os
       pontos, recalculando o nível. `concedidaPor` identifica quem da
       diretoria concedeu nas conquistas manuais; vem null quando quem
       concedeu foi o sistema.

       Conquista inativa nunca é concedida: `ativa` é o interruptor da
       presidência, e vale tanto para a vitrine quanto para a concessão. */
    @Transactional
    public boolean conceder(Pessoa pessoa, Conquista conquista, Pessoa concedidaPor) {
        if (!Boolean.TRUE.equals(conquista.getAtiva())) {
            return false;
        }
        if (participanteConquistaRepository
                .existsByPk_ParticipanteIdAndPk_ConquistaId(pessoa.getId(), conquista.getId())) {
            return false;
        }

        int pontos = conquista.getPontosBase() == null ? 0 : conquista.getPontosBase();

        ParticipanteConquista vinculo = new ParticipanteConquista();
        vinculo.setPk(new ParticipanteConquistaPK(pessoa.getId(), conquista.getId()));
        vinculo.setParticipante(pessoa);
        vinculo.setConquista(conquista);
        vinculo.setObtidaEm(LocalDateTime.now());
        vinculo.setXpCreditado(pontos);
        vinculo.setConcedidaPor(concedidaPor);
        participanteConquistaRepository.save(vinculo);

        somarXp(pessoa, pontos);
        log.info("Conquista '{}' concedida a {} (id {}), {} pontos.",
                conquista.getCodigo(), pessoa.getNome(), pessoa.getId(), pontos);
        return true;
    }

    /* Desfaz uma concessão: remove o vínculo e estorna exatamente o xp que
       ela creditou. Vínculo antigo sem xpCreditado gravado (não deve
       existir depois da V35) estorna zero — melhor do que chutar o valor
       atual do catálogo, que pode ter mudado. */
    @Transactional
    public void revogar(Pessoa pessoa, Conquista conquista) {
        ParticipanteConquista vinculo = participanteConquistaRepository
                .findById(new ParticipanteConquistaPK(pessoa.getId(), conquista.getId()))
                .orElse(null);
        if (vinculo == null) {
            return;
        }

        int estorno = vinculo.getXpCreditado() == null ? 0 : vinculo.getXpCreditado();
        participanteConquistaRepository.delete(vinculo);
        somarXp(pessoa, -estorno);

        log.info("Conquista '{}' revogada de {} (id {}), {} pontos estornados.",
                conquista.getCodigo(), pessoa.getNome(), pessoa.getId(), estorno);
    }

    /* ── Concessão manual (/checkin) e revogação (/admin) ────────── */

    /* Concede uma conquista MANUAL a partir do uuid do crachá (QR code) ou
       do id (busca manual). Quem concede fica gravado no vínculo.

       Conquista automática não passa por aqui de propósito: ela tem regra
       própria, e conceder à mão criaria um caminho paralelo capaz de dar a
       conquista a quem não cumpriu o critério. */
    @Transactional
    public ConquistaConcedidaDTO concederManualmente(Integer conquistaId,
                                                     ConcederConquistaRequestDTO dto,
                                                     Integer idDeQuemConcede) {
        Conquista conquista = buscarOuFalhar(conquistaId);

        if (conquista.getTipoValidacao() != TipoValidacaoConquista.MANUAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta conquista é concedida automaticamente pelo sistema.");
        }
        if (!Boolean.TRUE.equals(conquista.getAtiva())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta conquista ainda não está ativa.");
        }

        Pessoa participante = localizarParticipante(dto);
        if (participante.getRole() != Role.PARTICIPANTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só participantes confirmados recebem conquistas.");
        }

        Pessoa concedidaPor = pessoaRepository.findById(idDeQuemConcede)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));

        if (!conceder(participante, conquista, concedidaPor)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    participante.getNome() + " já tem esta conquista.");
        }

        return new ConquistaConcedidaDTO(
                participante.getNome(),
                conquista.getNome(),
                conquista.getPontosBase(),
                participante.getNivel() == null ? null : participante.getNivel().getNome(),
                participante.getXp());
    }

    /* "Não cadastrado" cobre uuid inexistente e id inexistente com a mesma
       mensagem — para quem opera a fila os dois casos pedem a mesma ação,
       mesmo critério de InscricaoEventoService.registrarPresencaPorUuid. */
    private Pessoa localizarParticipante(ConcederConquistaRequestDTO dto) {
        if (dto != null && dto.uuid() != null && !dto.uuid().isBlank()) {
            return pessoaRepository.findByUuid(dto.uuid())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Este participante não está cadastrado."));
        }
        if (dto != null && dto.participanteId() != null) {
            return pessoaRepository.findById(dto.participanteId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Este participante não está cadastrado."));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o QR code ou o participante.");
    }

    /* Conquistas que um participante tem — alimenta o painel de revogação
       do /admin. */
    @Transactional(readOnly = true)
    public List<ConquistaDoParticipanteDTO> listarDoParticipanteParaAdmin(Integer participanteId) {
        return participanteConquistaRepository.findByPk_ParticipanteId(participanteId).stream()
                .map(vinculo -> new ConquistaDoParticipanteDTO(
                        vinculo.getConquista().getId(),
                        vinculo.getConquista().getNome(),
                        vinculo.getConquista().getTipoValidacao() == null
                                ? null
                                : vinculo.getConquista().getTipoValidacao().name(),
                        vinculo.getXpCreditado(),
                        vinculo.getObtidaEm(),
                        vinculo.getConcedidaPor() == null ? null : vinculo.getConcedidaPor().getNome()))
                .toList();
    }

    /* Revogação pedida pelo /admin. Diferente de revogar(), falha se o
       vínculo não existe — quem clicou espera uma confirmação, e um
       silêncio pareceria sucesso. */
    @Transactional
    public void revogarDoParticipante(Integer participanteId, Integer conquistaId) {
        Pessoa participante = pessoaRepository.findById(participanteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante não encontrado."));
        Conquista conquista = buscarOuFalhar(conquistaId);

        if (!participanteConquistaRepository
                .existsByPk_ParticipanteIdAndPk_ConquistaId(participanteId, conquistaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    participante.getNome() + " não tem esta conquista.");
        }

        revogar(participante, conquista);
    }

    /* ── Auxiliares ──────────────────────────────────────────────── */

    private boolean temImagem(Conquista conquista) {
        return conquista.getImagemUrl() != null && !conquista.getImagemUrl().isBlank();
    }

    private ConquistaResponseDTO paraResposta(Conquista conquista) {
        return new ConquistaResponseDTO(
                conquista.getId(),
                conquista.getCodigo(),
                conquista.getNome(),
                conquista.getDescricao(),
                conquista.getPontosBase(),
                conquista.getRaridade(),
                conquista.getOrdem(),
                conquista.getAtiva(),
                conquista.getTipoValidacao() == null ? null : conquista.getTipoValidacao().name(),
                conquista.getImagemUrl(),
                participanteConquistaRepository.countByPk_ConquistaId(conquista.getId()));
    }

    /* Mesmo critério de InscricaoEventoService.creditarXp, aceitando
       delta negativo (estorno). Nunca deixa o xp ficar abaixo de zero. */
    private void somarXp(Pessoa pessoa, int delta) {
        int xpAtual = pessoa.getXp() == null ? 0 : pessoa.getXp();
        int novoXp = Math.max(0, xpAtual + delta);
        pessoa.setXp(novoXp);
        nivelRepository.findTopByXpMinimoLessThanEqualOrderByXpMinimoDesc(novoXp)
                .ifPresent(pessoa::setNivel);
        pessoaRepository.save(pessoa);
    }
}
