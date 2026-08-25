package com.semac.java_api.controller;

import com.semac.java_api.dto.ErroRespostaDTO;
import com.semac.java_api.dto.LoginRequestDTO;
import com.semac.java_api.dto.LoginResponseDTO;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.repository.PessoaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Duration VALIDADE_TOKEN = Duration.ofHours(8);

    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthController(PessoaRepository pessoaRepository,
                          PasswordEncoder passwordEncoder,
                          JwtEncoder jwtEncoder) {
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    /* Valida e-mail + senha. A senha é comparada com o hash BCrypt via
       passwordEncoder.matches — o hash nunca é "descriptografado".
       Por segurança, credenciais inválidas e usuário inexistente
       devolvem a mesma resposta 401 (não revela se o e-mail existe).
       Senha correta em conta que ainda não pode entrar (inscrição
       pendente ou acesso suspenso) devolve 403 com a mensagem explicando
       o motivo. Em caso de sucesso, devolve um Bearer token (JWT) com a
       claim `role`, usada para autorizar as rotas protegidas. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO dto) {
        Optional<Pessoa> encontrada = pessoaRepository.findByEmail(dto.email());

        if (encontrada.isEmpty() || !passwordEncoder.matches(dto.senha(), encontrada.get().getSenha())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErroRespostaDTO("E-mail ou senha inválidos."));
        }

        Pessoa pessoa = encontrada.get();

        /* Senha certa, mas a conta ainda não pode entrar. Os dois casos abaixo
           só são avaliados depois do matches: quem erra a senha continua
           recebendo o mesmo 401 e não descobre que o e-mail existe.

           role nula = inscrição aguardando a confirmação de um organizador;
           sem papel não há área nenhuma para acessar. */
        if (pessoa.getRole() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErroRespostaDTO("Estamos validando sua inscrição. Volte mais tarde."));
        }

        /* ativo = false: acesso suspenso pelo /admin. O registro é preservado
           (histórico), mas a pessoa não entra mais. */
        if (Boolean.FALSE.equals(pessoa.getAtivo())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErroRespostaDTO("Seu acesso está suspenso. Procure a organização da SEMAC."));
        }

        String role = pessoa.getRole().name();
        String token = gerarToken(pessoa, role);

        return ResponseEntity.ok(
                new LoginResponseDTO(token, pessoa.getId(), pessoa.getNome(), pessoa.getEmail(), role, pessoa.getUuid())
        );
    }

    private String gerarToken(Pessoa pessoa, String role) {
        Instant agora = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("semac")
                .issuedAt(agora)
                .expiresAt(agora.plus(VALIDADE_TOKEN))
                .subject(pessoa.getEmail())
                .claim("id", pessoa.getId())
                .claim("nome", pessoa.getNome());

        // O login barra quem não tem papel, então a claim sempre existe.
        claims.claim("role", role);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
