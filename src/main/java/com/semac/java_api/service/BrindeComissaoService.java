package com.semac.java_api.service;

import com.semac.java_api.dto.BrindeComissaoEntregueDTO;
import com.semac.java_api.dto.MeuBrindeComissaoDTO;
import com.semac.java_api.model.BrindeComissao;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.BrindeComissaoRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/* Brinde (Monster): cada pessoa da comissão mostra o próprio QR no Início
   do /admin e quem entrega lê no /checkin; participante não tem QR
   próprio e retira mostrando o crachá.

   Regras, todas aqui (a tela só exibe):
   - só vale nos dias do brinde (app.brinde-comissao.dias, padrão 26/10 e
     29/10/2026; dá para trocar por BRINDE_COMISSAO_DIAS para testar em
     dev), pela hora do servidor;
   - cada dia é uma rodada: uma retirada por pessoa por dia, e o QR da
     comissão é outro a cada dia (o de um dia não vale no outro);
   - leitura única: a baixa é atômica (ver
     BrindeComissaoRepository.marcarRetirado e registrarRetiradaPorCracha),
     então duas câmeras lendo o mesmo QR ao mesmo tempo não entregam dois
     brindes;
   - comissão retira só pelo QR do /admin (o crachá dela é recusado, senão
     daria para pegar dois) e ninguém entrega o próprio brinde. */
@Service
public class BrindeComissaoService {

    /* Prefixo do texto do QR: separa este QR do crachá (que é só o uuid). */
    static final String PREFIXO_QR_BRINDE_COMISSAO = "SEMAC-BRINDE-COMISSAO:";

    private static final int BYTES_CODIGO_BRINDE = 24;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final SecureRandom aleatorio = new SecureRandom();

    private final BrindeComissaoRepository brindeComissaoRepository;
    private final PessoaRepository pessoaRepository;
    private final List<LocalDate> diasBrinde;

    public BrindeComissaoService(BrindeComissaoRepository brindeComissaoRepository,
                                 PessoaRepository pessoaRepository,
                                 @Value("${app.brinde-comissao.dias}") List<LocalDate> diasBrinde) {
        this.brindeComissaoRepository = brindeComissaoRepository;
        this.pessoaRepository = pessoaRepository;
        this.diasBrinde = List.copyOf(diasBrinde);
    }

    /* QR de quem está logado. Fora dos dias do brinde não devolve nada (o
       cartão do /admin some). O código do dia nasce na primeira vez que a
       pessoa abre o próprio QR naquele dia. */
    @Transactional
    public MeuBrindeComissaoDTO meuBrinde(Integer pessoaId) {
        LocalDate hoje = LocalDate.now();
        if (!diasBrinde.contains(hoje)) {
            return MeuBrindeComissaoDTO.indisponivel();
        }
        Pessoa pessoa = buscarPessoa(pessoaId);
        if (!ehComissao(pessoa)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O QR do brinde é só para a comissão.");
        }

        BrindeComissao brinde = brindeComissaoRepository.findByPessoaIdAndDia(pessoaId, hoje)
                .orElseGet(() -> brindeComissaoRepository.save(
                        new BrindeComissao(null, pessoaId, null, hoje, novoCodigo(), null, null)));

        if (brinde.getRetiradoEm() != null) {
            Pessoa entreguePor = brinde.getEntreguePor();
            return new MeuBrindeComissaoDTO(true, null, brinde.getRetiradoEm(),
                    entreguePor == null ? null : entreguePor.getNome());
        }
        return new MeuBrindeComissaoDTO(true, PREFIXO_QR_BRINDE_COMISSAO + brinde.getCodigo(), null, null);
    }

    /* Leitura no /checkin: QR do /admin (comissão) ou crachá (participante).
       `idQuemEntrega` vem do token. */
    @Transactional
    public BrindeComissaoEntregueDTO retirar(String qrCode, Integer idQuemEntrega) {
        LocalDate hoje = LocalDate.now();
        if (!diasBrinde.contains(hoje)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O Monster só é entregue em " + diasPorExtenso() + ".");
        }
        String texto = qrCode == null ? "" : qrCode.trim();
        if (texto.startsWith(PREFIXO_QR_BRINDE_COMISSAO)) {
            return retirarPeloQrDaComissao(texto.substring(PREFIXO_QR_BRINDE_COMISSAO.length()), hoje, idQuemEntrega);
        }
        return retirarPeloCracha(texto, hoje, idQuemEntrega);
    }

    private BrindeComissaoEntregueDTO retirarPeloQrDaComissao(String codigo, LocalDate hoje, Integer idQuemEntrega) {
        BrindeComissao brinde = brindeComissaoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "QR do brinde não reconhecido."));
        Pessoa dono = brinde.getPessoa();
        if (!brinde.getDia().equals(hoje)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este QR era do dia " + brinde.getDia().format(DIA_MES)
                            + ". Peça para " + dono.getNome() + " abrir o /admin de novo para gerar o QR de hoje.");
        }
        if (!ehComissao(dono)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, dono.getNome() + " não está mais na comissão.");
        }
        exigirOutraPessoa(dono, idQuemEntrega);
        if (brinde.getRetiradoEm() != null) {
            throw jaRetirado(brinde);
        }

        Pessoa quemEntrega = pessoaRepository.getReferenceById(idQuemEntrega);
        int atualizadas = brindeComissaoRepository.marcarRetirado(brinde.getId(), LocalDateTime.now(), quemEntrega);
        if (atualizadas == 0) {
            // Outra câmera leu o mesmo QR entre a busca e a baixa.
            throw jaRetirado(brindeComissaoRepository.findByCodigo(codigo).orElse(brinde));
        }
        return new BrindeComissaoEntregueDTO(dono.getNome(), dono.getRole().name());
    }

    private BrindeComissaoEntregueDTO retirarPeloCracha(String uuid, LocalDate hoje, Integer idQuemEntrega) {
        Pessoa dono = pessoaRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "QR não reconhecido. Leia o crachá do participante ou o QR do /admin da comissão."));
        if (ehComissao(dono)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    dono.getNome() + " é da comissão: o Monster sai pelo QR do /admin, não pelo crachá.");
        }
        if (dono.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A inscrição de " + dono.getNome() + " ainda não foi confirmada.");
        }
        if (Boolean.FALSE.equals(dono.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A conta de " + dono.getNome() + " está inativa.");
        }
        exigirOutraPessoa(dono, idQuemEntrega);

        int inseridas = brindeComissaoRepository.registrarRetiradaPorCracha(
                dono.getId(), hoje, LocalDateTime.now(), idQuemEntrega);
        if (inseridas == 0) {
            throw brindeComissaoRepository.findByPessoaIdAndDia(dono.getId(), hoje)
                    .map(this::jaRetirado)
                    .orElseGet(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            dono.getNome() + " já retirou o brinde hoje."));
        }
        return new BrindeComissaoEntregueDTO(dono.getNome(), dono.getRole().name());
    }

    private ResponseStatusException jaRetirado(BrindeComissao brinde) {
        String hora = brinde.getRetiradoEm() == null ? "" : " às " + brinde.getRetiradoEm().format(HORA);
        String quem = brinde.getEntreguePor() == null ? "" : " com " + brinde.getEntreguePor().getNome();
        return new ResponseStatusException(HttpStatus.CONFLICT,
                brinde.getPessoa().getNome() + " já retirou o brinde hoje" + hora + quem + ".");
    }

    private String diasPorExtenso() {
        return diasBrinde.stream().map(dia -> dia.format(DIA_MES)).collect(Collectors.joining(" e "));
    }

    private static void exigirOutraPessoa(Pessoa dono, Integer idQuemEntrega) {
        if (dono.getId().equals(idQuemEntrega)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você não pode entregar o seu próprio brinde. Peça para outra pessoa da comissão ler.");
        }
    }

    private static boolean ehComissao(Pessoa pessoa) {
        return pessoa.getRole() != null && pessoa.getRole() != Role.PARTICIPANTE;
    }

    private Pessoa buscarPessoa(Integer pessoaId) {
        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }

    private String novoCodigo() {
        byte[] bytes = new byte[BYTES_CODIGO_BRINDE];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
