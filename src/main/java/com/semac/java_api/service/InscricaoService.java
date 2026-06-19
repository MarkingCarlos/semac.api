package com.semac.java_api.service;

import com.semac.java_api.dto.InscricaoRequestDTO;
import com.semac.java_api.dto.PessoaResponseDTO;
import com.semac.java_api.exception.RecursoDuplicadoException;
import com.semac.java_api.model.CamisaPedido;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.repository.CamisaPedidoRepository;
import com.semac.java_api.repository.PessoaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InscricaoService {

    private final PessoaRepository pessoaRepository;
    private final CamisaPedidoRepository camisaPedidoRepository;
    private final PasswordEncoder passwordEncoder;

    public InscricaoService(PessoaRepository pessoaRepository,
                            CamisaPedidoRepository camisaPedidoRepository,
                            PasswordEncoder passwordEncoder) {
        this.pessoaRepository = pessoaRepository;
        this.camisaPedidoRepository = camisaPedidoRepository;
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

        Pessoa pessoa = new Pessoa();
        pessoa.setNome(dto.nome());
        pessoa.setCpf(dto.cpf());
        pessoa.setEmail(dto.email());
        pessoa.setSenha(passwordEncoder.encode(dto.senha()));
        pessoa.setUuid(UUID.randomUUID().toString());
        pessoa.setRa(dto.ra());
        pessoa.setAtivo(true);
        pessoa.setRole(null);

        Pessoa salva = pessoaRepository.save(pessoa);

        CamisaPedido camisa = new CamisaPedido();
        camisa.setPessoa(salva);
        camisa.setModelo(dto.modelo());
        camisa.setTamanho(dto.tamanho());
        camisaPedidoRepository.save(camisa);

        return new PessoaResponseDTO(salva.getId(), salva.getUuid(), salva.getNome(), salva.getEmail());
    }
}
