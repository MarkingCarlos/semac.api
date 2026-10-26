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

/* Brinde da comissão: cada pessoa da comissão mostra o próprio QR no
   Início do /admin e quem entrega o brinde lê no /checkin.

   Regras, todas aqui (a tela só exibe):
   - só vale no dia do brinde (app.brinde-comissao.dia, padrão 26/10/2026;
     dá para trocar por BRINDE_COMISSAO_DIA para testar em dev), pela
     hora do servidor;
   - leitura única: o código é baixado de forma atômica (ver
     BrindeComissaoRepository.marcarRetirado), então duas câmeras lendo o
     mesmo QR ao mesmo tempo não entregam dois brindes;
   - ninguém entrega o próprio brinde. */
@Service
public class BrindeComissaoService {

    /* Prefixo do texto do QR: separa este QR do crachá (que é só o uuid),
       para o /checkin dizer "este é o QR do crachá" em vez de "não achei". */
    static final String PREFIXO_QR_BRINDE_COMISSAO = "SEMAC-BRINDE-COMISSAO:";

    private static final int BYTES_CODIGO_BRINDE = 24;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final SecureRandom aleatorio = new SecureRandom();

    private final BrindeComissaoRepository brindeComissaoRepository;
    private final PessoaRepository pessoaRepository;
    private final LocalDate diaBrindeComissao;

    public BrindeComissaoService(BrindeComissaoRepository brindeComissaoRepository,
                                 PessoaRepository pessoaRepository,
                                 @Value("${app.brinde-comissao.dia}") LocalDate diaBrindeComissao) {
        this.brindeComissaoRepository = brindeComissaoRepository;
        this.pessoaRepository = pessoaRepository;
        this.diaBrindeComissao = diaBrindeComissao;
    }

    /* QR de quem está logado. Fora do dia do brinde não devolve nada (o
       cartão do /admin some). O código nasce na primeira vez que a pessoa
       abre o próprio QR. */
    @Transactional
    public MeuBrindeComissaoDTO meuBrinde(Integer pessoaId) {
        if (!hojeEhDiaDoBrinde()) {
            return MeuBrindeComissaoDTO.indisponivel();
        }
        Pessoa pessoa = buscarPessoa(pessoaId);
        exigirComissao(pessoa, "O brinde é só para a comissão.");

        BrindeComissao brinde = brindeComissaoRepository.findById(pessoaId)
                .orElseGet(() -> brindeComissaoRepository.save(
                        new BrindeComissao(pessoaId, null, novoCodigo(), null, null)));

        if (brinde.getRetiradoEm() != null) {
            Pessoa entreguePor = brinde.getEntreguePor();
            return new MeuBrindeComissaoDTO(true, null, brinde.getRetiradoEm(),
                    entreguePor == null ? null : entreguePor.getNome());
        }
        return new MeuBrindeComissaoDTO(true, PREFIXO_QR_BRINDE_COMISSAO + brinde.getCodigo(), null, null);
    }

    /* Leitura no /checkin. `idQuemEntrega` vem do token. */
    @Transactional
    public BrindeComissaoEntregueDTO retirar(String qrCode, Integer idQuemEntrega) {
        if (!hojeEhDiaDoBrinde()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O brinde da comissão só pode ser retirado em "
                            + diaBrindeComissao.format(DateTimeFormatter.ofPattern("dd/MM")) + ".");
        }
        String texto = qrCode == null ? "" : qrCode.trim();
        if (!texto.startsWith(PREFIXO_QR_BRINDE_COMISSAO)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este não é o QR do brinde. Peça o QR do brinde no /admin da pessoa (não o do crachá).");
        }
        String codigo = texto.substring(PREFIXO_QR_BRINDE_COMISSAO.length());

        BrindeComissao brinde = brindeComissaoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "QR do brinde não reconhecido."));
        Pessoa dono = brinde.getPessoa();
        exigirComissao(dono, dono.getNome() + " não está mais na comissão.");

        if (brinde.getPessoaId().equals(idQuemEntrega)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você não pode entregar o seu próprio brinde. Peça para outra pessoa da comissão ler.");
        }
        if (brinde.getRetiradoEm() != null) {
            throw jaRetirado(brinde);
        }

        Pessoa quemEntrega = pessoaRepository.getReferenceById(idQuemEntrega);
        int atualizadas = brindeComissaoRepository.marcarRetirado(brinde.getPessoaId(), LocalDateTime.now(), quemEntrega);
        if (atualizadas == 0) {
            // Outra câmera leu o mesmo QR entre a busca e a baixa.
            throw jaRetirado(brindeComissaoRepository.findByCodigo(codigo).orElse(brinde));
        }
        return new BrindeComissaoEntregueDTO(dono.getNome(), dono.getRole().name());
    }

    private ResponseStatusException jaRetirado(BrindeComissao brinde) {
        String hora = brinde.getRetiradoEm() == null ? "" : " às " + brinde.getRetiradoEm().format(HORA);
        String quem = brinde.getEntreguePor() == null ? "" : " com " + brinde.getEntreguePor().getNome();
        return new ResponseStatusException(HttpStatus.CONFLICT,
                brinde.getPessoa().getNome() + " já retirou o brinde" + hora + quem + ".");
    }

    private boolean hojeEhDiaDoBrinde() {
        return LocalDate.now().equals(diaBrindeComissao);
    }

    private static void exigirComissao(Pessoa pessoa, String mensagem) {
        if (pessoa.getRole() == null || pessoa.getRole() == Role.PARTICIPANTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagem);
        }
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
