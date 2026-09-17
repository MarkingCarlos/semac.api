package com.semac.java_api.service;

import com.semac.java_api.dto.CamisetaPedidoDTO;
import com.semac.java_api.dto.InscricaoRequestDTO;
import com.semac.java_api.dto.PessoaResponseDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.CamisaPedido;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.TipoInscricao;
import com.semac.java_api.repository.CamisaPedidoRepository;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.TipoInscricaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InscricaoService {

    private final PessoaRepository pessoaRepository;
    private final CamisaPedidoRepository camisaPedidoRepository;
    private final TipoInscricaoRepository tipoInscricaoRepository;
    private final PasswordEncoder passwordEncoder;

    public InscricaoService(PessoaRepository pessoaRepository,
                            CamisaPedidoRepository camisaPedidoRepository,
                            TipoInscricaoRepository tipoInscricaoRepository,
                            PasswordEncoder passwordEncoder) {
        this.pessoaRepository = pessoaRepository;
        this.camisaPedidoRepository = camisaPedidoRepository;
        this.tipoInscricaoRepository = tipoInscricaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PessoaResponseDTO cadastrar(InscricaoRequestDTO dto) {
        // E-mail e CPF são únicos no banco — checa antes de salvar para
        // devolver um aviso claro (409) em vez de estourar erro de banco.
        if (pessoaRepository.findByEmail(dto.email()).isPresent()) {
            throw new RecursoDuplicadoException("Este e-mail já está cadastrado.");
        }
        if (pessoaRepository.findByCpf(dto.cpf()).isPresent()) {
            throw new RecursoDuplicadoException("Este CPF já está cadastrado.");
        }

        validarUnesp(dto);

        TipoInscricao ingresso = ingressoValido(dto.tipoInscricaoId());
        validarCodigoIngresso(ingresso, dto.codigoIngresso());
        validarRestricaoUnesp(ingresso, dto.ehUnesp());
        Integer dias = diasValidos(ingresso, dto.dias());
        List<CamisetaPedidoDTO> camisetas = camisetasValidas(ingresso, dto.camisetas());

        Pessoa pessoa = new Pessoa();
        pessoa.setNome(dto.nome());
        pessoa.setCpf(dto.cpf());
        pessoa.setEmail(dto.email());
        pessoa.setSenha(passwordEncoder.encode(dto.senha()));
        pessoa.setUuid(UUID.randomUUID().toString());
        pessoa.setRa(dto.ra());
        pessoa.setTelefone(dto.telefone());
        pessoa.setEhUnesp(dto.ehUnesp());
        pessoa.setAtivo(true);
        pessoa.setRole(null);
        pessoa.setInscritoEm(LocalDateTime.now());

        /* O ingresso já fica gravado no cadastro — é ele que define quantas
           camisetas a pessoa levou e quanto pagou. O organizador ainda pode
           trocá-lo na confirmação (PessoaService.atribuirRole). */
        pessoa.setTipoInscricao(ingresso);
        pessoa.setDiasInscricao(dias);

        Pessoa salva = pessoaRepository.save(pessoa);

        // Uma linha por camiseta. As `inclusas` primeiras (nessa mesma ordem,
        // que é a ordem em que o cadastro monta a lista — grátis primeiro,
        // avulsas depois) ficam gratuitas; o resto é avulsa.
        int inclusas = ingresso.getCamisetasGratis() == null ? 0 : ingresso.getCamisetasGratis();
        for (int i = 0; i < camisetas.size(); i++) {
            CamisetaPedidoDTO item = camisetas.get(i);
            CamisaPedido camisa = new CamisaPedido();
            camisa.setPessoa(salva);
            camisa.setModelo(item.modelo());
            camisa.setTamanho(item.tamanho());
            camisa.setAvulsa(i >= inclusas);
            camisaPedidoRepository.save(camisa);
        }

        return new PessoaResponseDTO(salva.getId(), salva.getUuid(), salva.getNome(), salva.getEmail());
    }

    /* ── Validações ──────────────────────────────────────────────── */

    /* Marcou "sou da UNESP": RA e e-mail institucional passam a ser
       obrigatórios (o valor do checkbox em si é persistido em
       Pessoa.ehUnesp, ver cadastrar() acima). */
    private void validarUnesp(InscricaoRequestDTO dto) {
        if (!dto.ehUnesp()) {
            return;
        }
        boolean raOk = dto.ra() != null && !dto.ra().isBlank();
        boolean emailOk = dto.email() != null && dto.email().toLowerCase().endsWith("@unesp.br");
        if (!raOk || !emailOk) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Estudantes da UNESP devem informar RA e um e-mail @unesp.br.");
        }
    }

    /* Ingressos com código configurado (ex.: comissão) só aceitam cadastro
       com o código exato — combate gente de fora se auto-inscrevendo
       nesse tipo. A verificação em POST /api/tipo-inscricao/{id}/verificar-codigo
       é só conveniência de UX; esta aqui é a barreira real. */
    private void validarCodigoIngresso(TipoInscricao ingresso, String codigoInformado) {
        String codigoExigido = ingresso.getCodigo();
        if (codigoExigido == null || codigoExigido.isBlank()) {
            return;
        }
        String informado = codigoInformado == null ? null : codigoInformado.trim();
        if (!codigoExigido.equals(informado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Código de acesso inválido para este ingresso.");
        }
    }

    /* Ingressos marcados restritoUnesp (ex.: desconto de permanência,
       comissão) só aceitam cadastro de quem marcou "Sou da UNESP". O
       filtro em BoxInscricao.jsx já esconde esses ingressos da lista pra
       quem não marcou — esta é a barreira real, igual ao código de acesso
       acima, contra quem chamar a API direto. */
    private void validarRestricaoUnesp(TipoInscricao ingresso, boolean ehUnesp) {
        if (Boolean.TRUE.equals(ingresso.getRestritoUnesp()) && !ehUnesp) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este ingresso é exclusivo para estudantes da UNESP.");
        }
    }

    private TipoInscricao ingressoValido(Integer id) {
        TipoInscricao ingresso = tipoInscricaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tipo de ingresso inválido."));
        if (!Boolean.TRUE.equals(ingresso.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este ingresso não está mais disponível.");
        }
        return ingresso;
    }

    /* Ingresso de diária exige uma quantidade dentro do limite; ingresso de
       valor fixo não guarda diária nenhuma, mesmo que o cliente mande uma. */
    private Integer diasValidos(TipoInscricao ingresso, Integer dias) {
        if (!Boolean.TRUE.equals(ingresso.getPorDia())) {
            return null;
        }
        int maximo = ingresso.getMaxDias() == null ? 1 : ingresso.getMaxDias();
        if (dias == null || dias < 1 || dias > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escolha de 1 a " + maximo + " diárias para este ingresso.");
        }
        return dias;
    }

    /* A pessoa precisa escolher exatamente as camisetas que o ingresso
       inclui — nem menos (falta escolha), nem mais. Camiseta avulsa não é
       vendida na inscrição pública; só o /admin pode registrar uma,
       manualmente, na confirmação ou no check-in. */
    private List<CamisetaPedidoDTO> camisetasValidas(TipoInscricao ingresso, List<CamisetaPedidoDTO> camisetas) {
        List<CamisetaPedidoDTO> lista = camisetas == null ? List.of() : camisetas;
        int inclusas = ingresso.getCamisetasGratis() == null ? 0 : ingresso.getCamisetasGratis();
        if (lista.size() < inclusas) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escolha modelagem e tamanho das camisetas inclusas no ingresso.");
        }
        if (lista.size() > inclusas) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este ingresso não permite comprar camiseta avulsa na inscrição.");
        }
        return lista;
    }
}
