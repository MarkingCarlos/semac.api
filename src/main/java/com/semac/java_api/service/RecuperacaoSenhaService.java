package com.semac.java_api.service;

import com.semac.java_api.config.CatalogoVariaveisEmail;
import com.semac.java_api.exception.BloqueioTentativasException;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.model.RecuperacaoSenha;
import com.semac.java_api.repository.PessoaRepository;
import com.semac.java_api.repository.RecuperacaoSenhaRepository;
import com.semac.java_api.service.ModeloEmailService.MensagemPronta;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/* Recuperação de senha em três etapas, todas públicas (a pessoa não
   consegue entrar — é justamente o problema):

     1. solicitar: gera um código de 5 dígitos e manda por e-mail;
     2. verificar: confere o código e devolve um token de troca;
     3. redefinir: com o token, grava a senha nova.

   Regras:
   - o código vale VALIDADE_CODIGO e só pode ser usado uma vez; pedir outro
     invalida o anterior (uma linha por pessoa em recuperacao_senha);
   - MAX_TENTATIVAS erros seguidos bloqueiam por DURACAO_BLOQUEIO. O
     contador é da pessoa, não do código — pedir código novo não o zera,
     senão bastaria pedir outro a cada 2 erros para contornar o limite;
   - código e token ficam só como hash SHA-256 no banco. SHA sem salt
     basta aqui: o que protege os 100 mil códigos possíveis é o limite de
     tentativas, não o custo do hash. */
@Service
public class RecuperacaoSenhaService {

    private static final Duration VALIDADE_CODIGO = Duration.ofMinutes(15);
    private static final Duration VALIDADE_TOKEN_TROCA = Duration.ofMinutes(10);
    private static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(30);
    /* Freia quem usaria a rota para lotar a caixa de e-mail de alguém. */
    private static final Duration INTERVALO_MINIMO_REENVIO = Duration.ofSeconds(60);
    private static final int MAX_TENTATIVAS = 3;

    private final SecureRandom geradorAleatorio = new SecureRandom();

    private final PessoaRepository pessoaRepository;
    private final RecuperacaoSenhaRepository recuperacaoSenhaRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModeloEmailService modeloEmailService;
    private final EmailService emailService;

    public RecuperacaoSenhaService(PessoaRepository pessoaRepository,
                                   RecuperacaoSenhaRepository recuperacaoSenhaRepository,
                                   PasswordEncoder passwordEncoder,
                                   ModeloEmailService modeloEmailService,
                                   EmailService emailService) {
        this.pessoaRepository = pessoaRepository;
        this.recuperacaoSenhaRepository = recuperacaoSenhaRepository;
        this.passwordEncoder = passwordEncoder;
        this.modeloEmailService = modeloEmailService;
        this.emailService = emailService;
    }

    /* Nunca falha de forma visível: e-mail inexistente, pessoa bloqueada
       ou pedido repetido rápido demais terminam em silêncio, com a mesma
       resposta de sucesso no controller. Assim a rota não serve para
       descobrir quem tem conta. */
    @Transactional
    public void solicitarCodigo(String email) {
        Optional<Pessoa> encontrada = pessoaRepository.findByEmail(email.trim());
        if (encontrada.isEmpty()) {
            return;
        }
        Pessoa pessoa = encontrada.get();
        LocalDateTime agora = LocalDateTime.now();

        RecuperacaoSenha recuperacao = recuperacaoSenhaRepository.findByPessoaId(pessoa.getId())
                .orElseGet(() -> novaRecuperacao(pessoa));
        encerrarBloqueioVencido(recuperacao, agora);

        if (estaBloqueada(recuperacao, agora)) {
            return;
        }
        if (recuperacao.getCodigoEnviadoEm() != null
                && recuperacao.getCodigoEnviadoEm().plus(INTERVALO_MINIMO_REENVIO).isAfter(agora)) {
            return;
        }

        String codigo = String.format("%05d", geradorAleatorio.nextInt(100_000));
        recuperacao.setCodigoHash(hash(codigo));
        recuperacao.setCodigoEnviadoEm(agora);
        recuperacao.setCodigoExpiraEm(agora.plus(VALIDADE_CODIGO));
        recuperacao.setTokenTrocaHash(null);
        recuperacao.setTokenTrocaExpiraEm(null);
        recuperacaoSenhaRepository.save(recuperacao);

        Map<String, String> variaveis = new LinkedHashMap<>();
        variaveis.put("nomeParticipante", pessoa.getNome());
        variaveis.put("codigo", codigo);
        variaveis.put("validadeMinutos", String.valueOf(VALIDADE_CODIGO.toMinutes()));
        MensagemPronta mensagem =
                modeloEmailService.montarObrigatoria(CatalogoVariaveisEmail.RECUPERACAO_SENHA, variaveis);
        emailService.enviarHtmlPronto(pessoa.getEmail(), mensagem.assunto(), mensagem.html());
    }

    /* noRollbackFor: o erro de código incorreto é devolvido como exceção,
       mas o incremento do contador (e o bloqueio) precisa ficar gravado —
       com rollback, errar o código não custaria nada. */
    @Transactional(noRollbackFor = {ResponseStatusException.class, BloqueioTentativasException.class})
    public String verificarCodigo(String email, String codigo) {
        Pessoa pessoa = pessoaRepository.findByEmail(email.trim())
                .orElseThrow(RecuperacaoSenhaService::codigoInvalidoOuExpirado);
        RecuperacaoSenha recuperacao = recuperacaoSenhaRepository.findByPessoaId(pessoa.getId())
                .orElseThrow(RecuperacaoSenhaService::codigoInvalidoOuExpirado);
        LocalDateTime agora = LocalDateTime.now();

        encerrarBloqueioVencido(recuperacao, agora);
        if (estaBloqueada(recuperacao, agora)) {
            throw bloqueio(recuperacao, agora);
        }

        if (recuperacao.getCodigoHash() == null || !recuperacao.getCodigoExpiraEm().isAfter(agora)) {
            throw codigoInvalidoOuExpirado();
        }

        if (!mesmoHash(recuperacao.getCodigoHash(), hash(codigo))) {
            int tentativas = recuperacao.getTentativasErradas() + 1;
            if (tentativas >= MAX_TENTATIVAS) {
                /* Bloqueia e queima o código: passado o bloqueio, é preciso
                   pedir outro, em vez de voltar a chutar o mesmo. */
                recuperacao.setTentativasErradas(0);
                recuperacao.setBloqueadoAte(agora.plus(DURACAO_BLOQUEIO));
                recuperacao.setCodigoHash(null);
                recuperacaoSenhaRepository.save(recuperacao);
                throw bloqueio(recuperacao, agora);
            }
            recuperacao.setTentativasErradas(tentativas);
            recuperacaoSenhaRepository.save(recuperacao);
            int restantes = MAX_TENTATIVAS - tentativas;
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Código incorreto. Você tem mais " + restantes
                            + (restantes == 1 ? " tentativa." : " tentativas."));
        }

        String tokenTroca = gerarTokenTroca();
        recuperacao.setCodigoHash(null);
        recuperacao.setTentativasErradas(0);
        recuperacao.setBloqueadoAte(null);
        recuperacao.setTokenTrocaHash(hash(tokenTroca));
        recuperacao.setTokenTrocaExpiraEm(agora.plus(VALIDADE_TOKEN_TROCA));
        recuperacaoSenhaRepository.save(recuperacao);
        return tokenTroca;
    }

    @Transactional
    public void redefinirSenha(String tokenTroca, String novaSenha) {
        LocalDateTime agora = LocalDateTime.now();
        RecuperacaoSenha recuperacao = recuperacaoSenhaRepository.findByTokenTrocaHash(hash(tokenTroca))
                .filter(r -> r.getTokenTrocaExpiraEm() != null && r.getTokenTrocaExpiraEm().isAfter(agora))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O prazo para trocar a senha acabou. Comece a recuperação de novo."));

        Pessoa pessoa = recuperacao.getPessoa();
        pessoa.setSenha(passwordEncoder.encode(novaSenha));
        pessoaRepository.save(pessoa);

        recuperacao.setTokenTrocaHash(null);
        recuperacao.setTokenTrocaExpiraEm(null);
        recuperacaoSenhaRepository.save(recuperacao);
    }

    private RecuperacaoSenha novaRecuperacao(Pessoa pessoa) {
        RecuperacaoSenha recuperacao = new RecuperacaoSenha();
        recuperacao.setPessoa(pessoa);
        recuperacao.setTentativasErradas(0);
        return recuperacao;
    }

    /* Bloqueio cumprido: a pessoa volta a ter as 3 tentativas inteiras. */
    private void encerrarBloqueioVencido(RecuperacaoSenha recuperacao, LocalDateTime agora) {
        if (recuperacao.getBloqueadoAte() != null && !recuperacao.getBloqueadoAte().isAfter(agora)) {
            recuperacao.setBloqueadoAte(null);
            recuperacao.setTentativasErradas(0);
        }
    }

    private boolean estaBloqueada(RecuperacaoSenha recuperacao, LocalDateTime agora) {
        return recuperacao.getBloqueadoAte() != null && recuperacao.getBloqueadoAte().isAfter(agora);
    }

    private BloqueioTentativasException bloqueio(RecuperacaoSenha recuperacao, LocalDateTime agora) {
        long segundos = Math.max(1, Duration.between(agora, recuperacao.getBloqueadoAte()).toSeconds());
        long minutos = (segundos + 59) / 60;
        return new BloqueioTentativasException(
                "Você errou o código " + MAX_TENTATIVAS + " vezes. Tente novamente em "
                        + minutos + (minutos == 1 ? " minuto." : " minutos."),
                segundos);
    }

    private static ResponseStatusException codigoInvalidoOuExpirado() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Código inválido ou expirado. Peça um novo código.");
    }

    private String gerarTokenTroca() {
        byte[] bytes = new byte[32];
        geradorAleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM.", e);
        }
    }

    /* Comparação em tempo constante: não vaza, pelo tempo de resposta,
       quantos caracteres do hash bateram. */
    private static boolean mesmoHash(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
