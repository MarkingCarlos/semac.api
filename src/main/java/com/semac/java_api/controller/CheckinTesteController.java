package com.semac.java_api.controller;

import com.semac.java_api.dto.LeituraTesteQrDTO;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.PessoaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/* Modo "Testar leitura" do /checkin: confere se o QR de um crachá é lido
   e a quem ele pertence, antes do evento ou quando um crachá dá problema.

   Somente leitura, de propósito separado de InscricaoEventoService e
   ConquistaService: não marca presença, não credita xp, não concede
   conquista e não grava tentativa de check-in. Acesso: qualquer papel de
   comissão (ver SecurityConfig), o mesmo público de marcar presença. */
@RestController
@RequestMapping("/api/checkin/teste")
public class CheckinTesteController {

    private static final Map<Role, String> ROTULO_FUNCAO_COMISSAO = Map.of(
            Role.MEMBRO, "Membro da comissão",
            Role.DIRETOR_SITE, "Diretor(a) de Site",
            Role.DIRETOR_CONTEUDO, "Diretor(a) de Conteúdo",
            Role.DIRETOR_PATROCINIO, "Diretor(a) de Patrocínio",
            Role.DIRETOR_APOIO, "Diretor(a) de Apoio",
            Role.DIRETOR_MARKETING, "Diretor(a) de Marketing",
            Role.PRESIDENTE, "Presidente"
    );

    private final PessoaRepository pessoaRepository;

    public CheckinTesteController(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    @GetMapping("/{uuid}")
    @Transactional(readOnly = true)
    public LeituraTesteQrDTO testarLeitura(@PathVariable String uuid) {
        Pessoa pessoa = pessoaRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR code não reconhecido."));
        return new LeituraTesteQrDTO(pessoa.getNome(), situacao(pessoa));
    }

    private String situacao(Pessoa pessoa) {
        String rotulo;
        if (pessoa.getRole() == null) {
            rotulo = "Aguardando confirmação";
        } else if (pessoa.getRole() == Role.PARTICIPANTE) {
            rotulo = "Participante confirmado";
        } else {
            rotulo = ROTULO_FUNCAO_COMISSAO.getOrDefault(pessoa.getRole(), pessoa.getRole().name());
        }
        return Boolean.FALSE.equals(pessoa.getAtivo()) ? rotulo + " · conta inativa" : rotulo;
    }
}
