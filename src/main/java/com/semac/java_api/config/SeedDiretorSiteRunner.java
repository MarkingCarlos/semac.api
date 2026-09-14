package com.semac.java_api.config;

import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.enums.Role;
import com.semac.java_api.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/* Cria o primeiro DIRETOR_SITE no primeiro boot com banco vazio — sem isso,
   ninguém consegue acessar PATCH /api/pessoa/{id}/role (exige um papel admin)
   pra promover o próprio primeiro diretor. Só roda se SEED_DIRETOR_EMAIL e
   SEED_DIRETOR_SENHA estiverem definidas e não existir nenhum DIRETOR_SITE
   ainda; em runs seguintes (diretor já existe) não faz nada. */
@Component
public class SeedDiretorSiteRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDiretorSiteRunner.class);

    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.diretor.email:}")
    private String seedEmail;

    @Value("${seed.diretor.senha:}")
    private String seedSenha;

    @Value("${seed.diretor.nome:Diretor(a) Site}")
    private String seedNome;

    @Value("${seed.diretor.cpf:00000000000}")
    private String seedCpf;

    public SeedDiretorSiteRunner(PessoaRepository pessoaRepository, PasswordEncoder passwordEncoder) {
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (pessoaRepository.countByRole(Role.DIRETOR_SITE) > 0) {
            return;
        }

        if (seedEmail.isBlank() || seedSenha.isBlank()) {
            log.warn("Nenhum DIRETOR_SITE encontrado e SEED_DIRETOR_EMAIL/SEED_DIRETOR_SENHA "
                    + "não foram definidas — pulando seed do primeiro diretor.");
            return;
        }

        if (pessoaRepository.findByEmail(seedEmail).isPresent()) {
            log.warn("SEED_DIRETOR_EMAIL já está em uso por uma pessoa existente — "
                    + "pulando seed do primeiro diretor.");
            return;
        }

        Pessoa diretor = new Pessoa();
        diretor.setNome(seedNome);
        diretor.setEmail(seedEmail);
        diretor.setCpf(seedCpf);
        diretor.setSenha(passwordEncoder.encode(seedSenha));
        diretor.setUuid(UUID.randomUUID().toString());
        diretor.setEhUnesp(false);
        diretor.setAtivo(true);
        diretor.setRole(Role.DIRETOR_SITE);
        diretor.setTentativasCartao(0);

        pessoaRepository.save(diretor);
        log.info("Primeiro DIRETOR_SITE criado via seed: {}", seedEmail);
    }
}
