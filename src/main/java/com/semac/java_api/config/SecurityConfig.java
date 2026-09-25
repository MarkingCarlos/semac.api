package com.semac.java_api.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/* Autenticação por Bearer token (JWT HS256). O login gera o token; cada
   requisição protegida o envia em Authorization: Bearer <token>. O
   resource-server decodifica, lê a claim `role` e a transforma em uma
   autoridade ROLE_<role> usada nas regras abaixo.

   Acesso ao módulo financeiro (apenas DIRETOR_SITE e PRESIDENTE):
   endpoints exclusivos do financeiro são trancados; os GET de
   patrocinador/cota/doador seguem abertos por serem usados pelo site
   público e pela /admin (ainda sem auth). */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /* Público: o PrevisaoItemController também consulta a lista para decidir
       se o resumo sai completo ou sem a composição da arrecadação. */
    public static final String[] PAPEIS_FINANCEIRO = { "DIRETOR_SITE", "PRESIDENTE" };

    /* Quem pode VER a aba Previsão do /financeiro: o financeiro mais os
       diretores de conteúdo, patrocínio, apoio e marketing. Só GET — criar,
       editar, converter e excluir seguem com PAPEIS_FINANCEIRO. */
    private static final String[] PAPEIS_LEITURA_PREVISAO = {
            "DIRETOR_SITE", "PRESIDENTE", "DIRETOR_CONTEUDO", "DIRETOR_PATROCINIO", "DIRETOR_APOIO", "DIRETOR_MARKETING"
    };

    /* Mesmos papéis do financeiro, com nome próprio: quem pode editar e
       disparar e-mail em nome da SEMAC. Separado para que afrouxar um não
       afrouxe o outro sem querer. */
    private static final String[] PAPEIS_MENSAGENS = { "DIRETOR_SITE", "PRESIDENTE" };

    /* Qualquer papel de comissão — espelha PAPEIS_ADMIN do frontend
       (auth/sessao.js). Financeiro (acima) é um subconjunto: quem tem
       acesso financeiro também tem acesso admin. */
    private static final String[] PAPEIS_ADMIN = {
            "MEMBRO", "DIRETOR_CONTEUDO", "DIRETOR_PATROCINIO", "DIRETOR_APOIO", "DIRETOR_MARKETING", "DIRETOR_SITE", "PRESIDENTE"
    };

    /* Papéis de comissão exceto MEMBRO — brindes e relatórios são
       restritos aos diretores/presidente. Derivado de PAPEIS_ADMIN para
       que um novo papel adicionado lá já entre aqui automaticamente. */
    private static final String[] PAPEIS_ADMIN_SEM_MEMBRO =
            Arrays.stream(PAPEIS_ADMIN).filter(p -> !"MEMBRO".equals(p)).toArray(String[]::new);

    /* Quem manda na programação: criar, editar, excluir e marcar o início
       real de um evento. Espelha os papéis da aba Conteúdo do /admin
       (Admin.jsx). MEMBRO fica de fora — a aba já não aparecia pra ele,
       mas as rotas aceitavam qualquer papel de comissão, então dava pra
       mexer na programação chamando a API direto. */
    private static final String[] PAPEIS_CONTEUDO = { "DIRETOR_SITE", "PRESIDENTE", "DIRETOR_CONTEUDO" };

    /* Quem cadastra, edita e exclui doações (tabela `doador`). Espelha os
       papéis da aba Doações do /admin (Admin.jsx). O diretor de patrocínio
       entra aqui, mas patrocinador/cota e a meta de doação seguem só com
       PAPEIS_FINANCEIRO. */
    private static final String[] PAPEIS_DOACAO = { "DIRETOR_SITE", "PRESIDENTE", "DIRETOR_PATROCINIO" };

    private static final String PAPEL_PARTICIPANTE = "PARTICIPANTE";

    private final SecretKey chaveJwt;
    private final List<String> origensCors;

    public SecurityConfig(@Value("${jwt.secret}") String segredo,
                          @Value("${app.cors.origins:http://localhost:5173}") List<String> origensCors) {
        this.chaveJwt = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.origensCors = origensCors;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Públicos / abertos
                        .requestMatchers("/api/auth/login", "/api/inscricao").permitAll()
                        // Cobrança do cartão — chamada logo após o cadastro público acima
                        .requestMatchers(HttpMethod.POST, "/api/pagamento/cartao").permitAll()
                        // Perfil próprio: qualquer usuário autenticado (identificado pelo token)
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/pessoa/me").authenticated()
                        // Agenda, escolha de minicurso e ranking: só o próprio participante confirmado
                        .requestMatchers(HttpMethod.GET, "/api/evento/meus").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/ranking").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/me/dias-ingresso").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.PUT, "/api/pessoa/me/dias-ingresso").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.POST, "/api/evento/*/inscricao").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.DELETE, "/api/evento/*/inscricao").hasRole(PAPEL_PARTICIPANTE)
                        // Textos dos e-mails automáticos (/admin -> Mensagens).
                        // Mesmo público do financeiro: mudar o texto de uma mensagem
                        // que sai para todos os inscritos não é ação de rotina, e o
                        // envio de teste consome cota diária do Gmail.
                        .requestMatchers("/api/admin/modelos-email/**").hasAnyRole(PAPEIS_MENSAGENS)
                        // Comunicados avulsos: mesmo público, e aqui o motivo é ainda
                        // mais forte — é disparo em massa, irreversível.
                        .requestMatchers("/api/comunicados/**").hasAnyRole(PAPEIS_MENSAGENS)
                        // Exclusivos do financeiro
                        .requestMatchers("/api/compra/**", "/api/fornecedor/**", "/api/cotacao/**", "/api/conjunto/**", "/api/variacao/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers("/api/caixa/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Leitura da aba Previsão — antes da regra geral abaixo, que tranca o resto.
                        // O /resumo sai sem entradas e reserva FUNDUNESP para quem não é financeiro.
                        .requestMatchers(HttpMethod.GET, "/api/previsao", "/api/previsao/resumo", "/api/previsao-categoria", "/api/orcamento").hasAnyRole(PAPEIS_LEITURA_PREVISAO)
                        .requestMatchers("/api/previsao/**", "/api/previsao-categoria/**", "/api/orcamento/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/inscricoes").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Editar quantas camisetas uma pessoa tem (grátis/inclusas ou avulsas) —
                        // mesmo acesso do financeiro, tanto para comissão quanto para participantes.
                        .requestMatchers(HttpMethod.PUT, "/api/pessoa/*/camisetas").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Escrita de patrocínio/cota acontece só no financeiro (GET segue aberto)
                        .requestMatchers(HttpMethod.POST, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PUT, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PATCH, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Escrita de doador — aba Doações do /admin, que o diretor de
                        // patrocínio também usa (GET segue aberto)
                        .requestMatchers(HttpMethod.POST, "/api/doador/**").hasAnyRole(PAPEIS_DOACAO)
                        .requestMatchers(HttpMethod.PUT, "/api/doador/**").hasAnyRole(PAPEIS_DOACAO)
                        .requestMatchers(HttpMethod.PATCH, "/api/doador/**").hasAnyRole(PAPEIS_DOACAO)
                        .requestMatchers(HttpMethod.DELETE, "/api/doador/**").hasAnyRole(PAPEIS_DOACAO)
                        // Preço da camiseta avulsa — editado em Informações SEMAC. O GET
                        // segue aberto: o cadastro público precisa do preço.
                        .requestMatchers(HttpMethod.PUT, "/api/camiseta-extra").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Meta de doação — editada em Informações SEMAC. O GET segue
                        // aberto: a página pública de doação precisa da meta.
                        .requestMatchers(HttpMethod.PUT, "/api/meta-doacao").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Liga/desliga o botão "Inscreva-se" da Home — editado em
                        // Informações SEMAC. O GET segue aberto: a Home pública precisa
                        // saber se mostra o botão.
                        .requestMatchers(HttpMethod.PUT, "/api/configuracao-inscricao").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Regras de xp (presença, Termo, cortes de atraso) — editadas em
                        // Informações SEMAC. O GET é do card "COMO GANHAR XP" do
                        // /participantes, então vale para qualquer autenticado.
                        .requestMatchers(HttpMethod.GET, "/api/regra-xp").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/regra-xp/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Escrita de níveis de participante — gerenciada em Informações SEMAC (GET segue aberto)
                        .requestMatchers(HttpMethod.POST, "/api/nivel/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PUT, "/api/nivel/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/nivel/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Exclusivos do /admin (qualquer papel de comissão). GET /api/evento e
                        // GET /api/tipo-inscricao seguem abertos de propósito — alimentam a
                        // programação pública e o cadastro em /inscricoes, respectivamente.
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/participantes", "/api/pessoa/comissao").hasAnyRole(PAPEIS_ADMIN)
                        // Comprovante de pagamento — quem confirma inscrição precisa poder ver
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/*/comprovante").hasAnyRole(PAPEIS_ADMIN)
                        // Reconsulta de status do pagamento no cartão — mesmo público do comprovante acima
                        .requestMatchers(HttpMethod.GET, "/api/pagamento/cartao/*/status").hasAnyRole(PAPEIS_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/relatorio/**").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Crachás impressos — devolve o uuid de check-in de todo mundo, então
                        // fica com quem gerencia pessoas (mesmo público da aba Pessoas)
                        .requestMatchers(HttpMethod.GET, "/api/cracha").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PATCH, "/api/pessoa/*/role", "/api/pessoa/*/ativo", "/api/pessoa/*/desconfirmar").hasAnyRole(PAPEIS_ADMIN)
                        // Cadastro manual de participante (inscrição de balcão) — mesmo público
                        // de quem confirma ou exclui inscrição
                        .requestMatchers(HttpMethod.POST, "/api/pessoa").hasAnyRole(PAPEIS_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/pessoa/*").hasAnyRole(PAPEIS_ADMIN)
                        // Programação é da aba Conteúdo — MEMBRO não cria nem mexe em evento
                        .requestMatchers(HttpMethod.POST, "/api/evento").hasAnyRole(PAPEIS_CONTEUDO)
                        .requestMatchers(HttpMethod.PUT, "/api/evento/*").hasAnyRole(PAPEIS_CONTEUDO)
                        .requestMatchers(HttpMethod.DELETE, "/api/evento/*").hasAnyRole(PAPEIS_CONTEUDO)
                        // Marcar presença segue com qualquer papel de comissão: quem opera o /checkin
                        .requestMatchers(HttpMethod.POST, "/api/evento/*/presenca", "/api/evento/*/presenca/*").hasAnyRole(PAPEIS_ADMIN)
                        // "INICIAR EVENTO" do /admin: move o marco do atraso pro início real
                        .requestMatchers(HttpMethod.POST, "/api/evento/*/iniciar").hasAnyRole(PAPEIS_CONTEUDO)
                        .requestMatchers("/api/tipo-evento/**").hasAnyRole(PAPEIS_ADMIN)
                        // GET /api/trilha segue aberto — alimenta o filtro da programação pública
                        .requestMatchers(HttpMethod.POST, "/api/trilha").hasAnyRole(PAPEIS_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/trilha/*").hasAnyRole(PAPEIS_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/trilha/*").hasAnyRole(PAPEIS_ADMIN)
                        // MEMBRO pode ver e usar brindes no sorteio, mas não gerenciá-los
                        .requestMatchers(HttpMethod.GET, "/api/brinde/**").hasAnyRole(PAPEIS_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/brinde/**").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.PUT, "/api/brinde/**").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/brinde/**").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers("/api/sorteio/**").hasAnyRole(PAPEIS_ADMIN)
                        // Verificação do código de acesso do ingresso — pública de propósito,
                        // usada pelo cadastro em /inscricoes; nunca revela o código real.
                        .requestMatchers(HttpMethod.POST, "/api/tipo-inscricao/*/verificar-codigo").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tipo-inscricao").hasAnyRole(PAPEIS_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/tipo-inscricao/*").hasAnyRole(PAPEIS_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/tipo-inscricao/*").hasAnyRole(PAPEIS_ADMIN)
                        // ── Conquistas ──────────────────────────────────────────
                        // A imagem é pública: vai direto num <img src> na área do
                        // participante e no preview do /admin.
                        .requestMatchers(HttpMethod.GET, "/api/conquista/*/imagem").permitAll()
                        // Vitrine do próprio participante (só as conquistas ativas)
                        .requestMatchers(HttpMethod.GET, "/api/conquista/minhas").hasRole(PAPEL_PARTICIPANTE)
                        // Confirma que a animação de desbloqueio já foi exibida — o
                        // participante só marca as próprias (id vem do token).
                        .requestMatchers(HttpMethod.POST, "/api/conquista/*/vista").hasRole(PAPEL_PARTICIPANTE)
                        // Leitura do catálogo: qualquer papel de comissão
                        .requestMatchers(HttpMethod.GET, "/api/conquista").hasAnyRole(PAPEIS_ADMIN)
                        // Conceder conquista manual (/checkin) — credita pontos que
                        // mexem no ranking, então fica com diretores e presidência.
                        // MEMBRO segue podendo marcar presença, mas não conceder.
                        .requestMatchers(HttpMethod.POST, "/api/conquista/*/conceder").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Reavaliação em massa das regras automáticas (botão do /admin)
                        .requestMatchers(HttpMethod.POST, "/api/conquista/reavaliar").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Conquistas de um participante e revogação — mesmo público
                        // da concessão, já que revogar é desfazer uma concessão.
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/*/conquistas").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/pessoa/*/conquistas/*").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Configuração do catálogo (texto, pontos, imagem, ativa) —
                        // mesmo público que edita níveis e cotas em Informações SEMAC.
                        .requestMatchers(HttpMethod.PUT, "/api/conquista/*").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PATCH, "/api/conquista/*/ativa").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.POST, "/api/conquista/*/imagem").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Rede de segurança: como a regra final é permitAll, qualquer
                        // rota de conquista que venha a existir e não esteja listada
                        // acima nasceria pública. Aqui ela exige ao menos comissão.
                        .requestMatchers("/api/conquista/**").hasAnyRole(PAPEIS_ADMIN)
                        // ── Termo (/termo) ──────────────────────────────────────
                        // Jogar é do participante: a vitória credita xp e as
                        // tentativas são contadas por pessoa no banco.
                        .requestMatchers(HttpMethod.GET, "/api/termo/hoje").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.GET, "/api/termo/meus").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.POST, "/api/termo/palpite").hasRole(PAPEL_PARTICIPANTE)
                        // Cadastro da palavra de cada dia — mesmo público que
                        // edita níveis e cotas em Informações SEMAC. A palavra
                        // nunca volta na resposta, nem para quem a cadastrou.
                        .requestMatchers(HttpMethod.GET, "/api/termo/palavras").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PUT, "/api/termo/palavras/*").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Rede de segurança, mesmo motivo de /api/conquista/**:
                        // rota nova de termo não nasce pública.
                        .requestMatchers("/api/termo/**").hasAnyRole(PAPEIS_ADMIN)
                        // Demais (site público) seguem abertos
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorAutenticacao())));
        return http.build();
    }

    /* Lê a claim `role` do token e a expõe como autoridade ROLE_<role>,
       que casa com hasAnyRole(...). */
    private JwtAuthenticationConverter conversorAutenticacao() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthorityPrefix("ROLE_");
        autoridades.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origensCors);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
