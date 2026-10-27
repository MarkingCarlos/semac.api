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

    /* Quem pode editar e disparar e-mail em nome da SEMAC (aba Mensagens
       do /admin): os papéis do financeiro mais a diretoria de conteúdo.
       Separado do financeiro para que afrouxar um não afrouxe o outro sem
       querer. Espelha os papéis da aba Mensagens em Admin.jsx. */
    private static final String[] PAPEIS_MENSAGENS = { "DIRETOR_SITE", "PRESIDENTE", "DIRETOR_CONTEUDO" };

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

    /* Quem cadastra, edita e exclui doações (tabela `doador`). Espelha
       PAPEIS_DOACAO do frontend (auth/sessao.js) — aba Doações do
       /financeiro. O diretor de patrocínio
       entra aqui, mas patrocinador/cota e a meta de doação seguem só com
       PAPEIS_FINANCEIRO. */
    private static final String[] PAPEIS_DOACAO = { "DIRETOR_SITE", "PRESIDENTE", "DIRETOR_PATROCINIO" };

    /* Quem configura a gamificação: regras de xp, níveis de participante,
       o catálogo de conquistas, os times e o Termo (sub-aba Gamificação de
       Informações SEMAC).
       Espelha PAPEIS_GAMIFICACAO do frontend (auth/sessao.js). O diretor de
       conteúdo entra aqui, mas as configurações financeiras da mesma tela
       (cotas, meta, camiseta, inscrições) seguem só com PAPEIS_FINANCEIRO. */
    private static final String[] PAPEIS_GAMIFICACAO = { "DIRETOR_SITE", "PRESIDENTE", "DIRETOR_CONTEUDO" };

    /* Quem vê a dashboard da seção Início do /admin. Por enquanto só a
       presidência (que vê tudo) e a diretoria de site (quem mantém o
       sistema); a divisão por papel vem depois. Espelha PAPEIS_DASHBOARD
       do frontend (auth/sessao.js). */
    private static final String[] PAPEIS_DASHBOARD = { "PRESIDENTE", "DIRETOR_SITE" };

    /* Card "Vagas restantes em minicursos" da dashboard (com a lista de
       inscritos de cada minicurso): a dashboard mais a diretoria de
       conteúdo, que cuida da programação. Espelha
       PAPEIS_DASHBOARD_MINICURSOS do frontend (auth/sessao.js). */
    private static final String[] PAPEIS_DASHBOARD_MINICURSOS = { "PRESIDENTE", "DIRETOR_SITE", "DIRETOR_CONTEUDO" };

    /* Card "Top 3 do ranking" da dashboard (com o ranking completo, e-mail
       incluído): toda a diretoria e a presidência. MEMBRO fica de fora.
       Espelha PAPEIS_DASHBOARD_RANKING do frontend (auth/sessao.js). */
    private static final String[] PAPEIS_DASHBOARD_RANKING = PAPEIS_ADMIN_SEM_MEMBRO;

    /* Quem vê, cria e altera dados de participante e da comissão: listas,
       comprovante, pagamento, cadastro de balcão, correção de cadastro e
       minicursos de outra pessoa. É o público da aba Pessoas do /admin —
       MEMBRO fica de fora e só vê o nome de quem lê no check-in ou no
       sorteio. Espelha PAPEIS_GESTAO_PARTICIPANTES do frontend
       (auth/sessao.js). Mesmos papéis do financeiro, com nome próprio pelo
       mesmo motivo de PAPEIS_MENSAGENS. */
    private static final String[] PAPEIS_GESTAO_PARTICIPANTES = { "DIRETOR_SITE", "PRESIDENTE" };

    /* Quem entrega kit no credenciamento (sub-aba Kits de Pessoas do
       /admin): toda a comissão, MEMBRO incluído. Hoje igual a
       PAPEIS_ADMIN, com nome próprio pelo mesmo motivo de
       PAPEIS_MENSAGENS. Espelha PAPEIS_ENTREGA_KIT do frontend. */
    private static final String[] PAPEIS_ENTREGA_KIT = PAPEIS_ADMIN;

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
                        // Recuperação de senha: quem usa é justamente quem não consegue entrar
                        .requestMatchers(HttpMethod.POST, "/api/auth/recuperar-senha/**").permitAll()
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
                        // Pesquisa de satisfação: o participante dá e lê as próprias notas;
                        // o resumo (média por evento) é da diretoria, na aba Conteúdo.
                        .requestMatchers(HttpMethod.PUT, "/api/evento/*/avaliacao").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.GET, "/api/evento/avaliacoes/minhas").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.GET, "/api/evento/avaliacoes").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Textos dos e-mails automáticos (/admin -> Mensagens).
                        // Público restrito (financeiro + conteúdo): mudar o texto de uma mensagem
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
                        // Escrita de patrocínio/cota acontece só no financeiro. O GET segue
                        // aberto: cota alimenta a página /cotas, e o PatrocinadorController
                        // esconde os campos financeiros de quem não é do financeiro.
                        .requestMatchers(HttpMethod.POST, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PUT, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.PATCH, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/patrocinador/**", "/api/cota/**").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Doador — aba Doações do /financeiro, que o diretor de patrocínio
                        // também usa. A lista traz o valor de cada doação, então só
                        // /resumo-publico (total + nomes) fica aberto para o site.
                        .requestMatchers(HttpMethod.GET, "/api/doador").hasAnyRole(PAPEIS_DOACAO)
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
                        // Liga/desliga a escolha de minicursos do /participantes —
                        // editado na aba Conteúdo do /admin. O GET é de qualquer
                        // usuário logado: o /participantes mostra ou esconde o botão.
                        .requestMatchers(HttpMethod.GET, "/api/configuracao-inscricao/minicursos").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/configuracao-inscricao/minicursos").hasAnyRole(PAPEIS_CONTEUDO)
                        // Liga/desliga as perguntas para os debates — editado em
                        // Informações SEMAC. GET de qualquer logado: o /participantes
                        // mostra ou esconde o glow e o botão "Mande sua pergunta".
                        .requestMatchers(HttpMethod.GET, "/api/configuracao-inscricao/perguntas-debate").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/configuracao-inscricao/perguntas-debate").hasAnyRole(PAPEIS_CONTEUDO)
                        // Perguntas dos debates: qualquer pessoa envia, com ou sem login
                        // (professores e convidados não têm conta) — página /perguntas e
                        // /participantes. Sem login, o abuso é freado por IP (RateLimitConfig).
                        // Só diretoria e presidência leem (MEMBRO fica de fora).
                        .requestMatchers(HttpMethod.GET, "/api/perguntas/debates").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/perguntas").permitAll()
                        // Excluir pergunta: presidência e diretores, mesmo público da leitura
                        .requestMatchers(HttpMethod.DELETE, "/api/perguntas/*").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers("/api/perguntas/**").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.GET, "/api/evento/*/perguntas").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Incluir pergunta à mão pelo /admin (ex.: entregue em papel):
                        // mesmo público da leitura, e vale com as perguntas fechadas.
                        .requestMatchers(HttpMethod.POST, "/api/evento/*/perguntas").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Estado do telão do debate (painel no notebook ou no celular,
                        // e a guia projetada): mesmo público do painel /debate/<id>.
                        .requestMatchers("/api/evento/*/telao-debate").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Regras de xp (presença, Termo, cortes de atraso) — editadas em
                        // Informações SEMAC. O GET é do card "COMO GANHAR XP" do
                        // /participantes, então vale para qualquer autenticado.
                        .requestMatchers(HttpMethod.GET, "/api/regra-xp").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/regra-xp/**").hasAnyRole(PAPEIS_GAMIFICACAO)
                        // Escrita de níveis de participante — gerenciada em Informações SEMAC (GET segue aberto)
                        .requestMatchers(HttpMethod.POST, "/api/nivel/**").hasAnyRole(PAPEIS_GAMIFICACAO)
                        .requestMatchers(HttpMethod.PUT, "/api/nivel/**").hasAnyRole(PAPEIS_GAMIFICACAO)
                        .requestMatchers(HttpMethod.DELETE, "/api/nivel/**").hasAnyRole(PAPEIS_GAMIFICACAO)
                        // Listas de participantes e comissão (e-mail, RA, telefone, pagamento) —
                        // só a aba Pessoas do /admin. Com PAPEIS_ADMIN um MEMBRO baixava a
                        // lista inteira. GET /api/evento e GET /api/tipo-inscricao seguem
                        // abertos de propósito — alimentam a programação pública e o
                        // cadastro em /inscricoes, respectivamente.
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/participantes", "/api/pessoa/comissao").hasAnyRole(PAPEIS_GESTAO_PARTICIPANTES)
                        // Entrega de kit — toda a comissão; a lista não traz contato, RA nem pagamento
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/kits").hasAnyRole(PAPEIS_ENTREGA_KIT)
                        .requestMatchers(HttpMethod.PATCH, "/api/pessoa/*/kit").hasAnyRole(PAPEIS_ENTREGA_KIT)
                        // Comprovante de pagamento — quem confirma inscrição (aba Pessoas)
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/*/comprovante").hasAnyRole(PAPEIS_GESTAO_PARTICIPANTES)
                        // Reconsulta de status do pagamento no cartão — mesmo público do comprovante acima
                        .requestMatchers(HttpMethod.GET, "/api/pagamento/cartao/*/status").hasAnyRole(PAPEIS_GESTAO_PARTICIPANTES)
                        .requestMatchers(HttpMethod.GET, "/api/relatorio/**").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Card de minicursos — antes da regra geral da dashboard, que tranca o resto
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/minicursos", "/api/dashboard/minicursos/*/inscritos").hasAnyRole(PAPEIS_DASHBOARD_MINICURSOS)
                        // Card de ranking — idem
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/ranking").hasAnyRole(PAPEIS_DASHBOARD_RANKING)
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasAnyRole(PAPEIS_DASHBOARD)
                        // Crachás impressos — devolve o uuid de check-in de todo mundo, então
                        // fica com quem gerencia pessoas (mesmo público da aba Pessoas)
                        .requestMatchers(HttpMethod.GET, "/api/cracha").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Professores convidados (crachá de professor) — cadastrados
                        // na mesma subaba Crachás, então mesmo público
                        .requestMatchers("/api/professor-convidado", "/api/professor-convidado/*").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Certificados de palestrante — mesmo público dos crachás (aba Pessoas)
                        .requestMatchers(HttpMethod.GET, "/api/certificado").hasAnyRole(PAPEIS_FINANCEIRO)
                        // Papel, ativo/suspenso, desconfirmar e excluir — só quem vê a aba
                        // Pessoas do /admin. Com PAPEIS_ADMIN um MEMBRO podia se dar
                        // PRESIDENTE chamando a API direto.
                        .requestMatchers(HttpMethod.PATCH, "/api/pessoa/*/role", "/api/pessoa/*/ativo", "/api/pessoa/*/desconfirmar").hasAnyRole(PAPEIS_GESTAO_PARTICIPANTES)
                        // Correção de cadastro e minicursos de outra pessoa — só diretoria de site e presidência
                        .requestMatchers(HttpMethod.PATCH, "/api/pessoa/*/dados").hasAnyRole(PAPEIS_GESTAO_PARTICIPANTES)
                        .requestMatchers("/api/pessoa/*/minicursos", "/api/pessoa/*/minicursos/*").hasAnyRole(PAPEIS_GESTAO_PARTICIPANTES)
                        // Cadastro manual de participante (inscrição de balcão) — só cria
                        // participante (confirmado ou pendente), nunca comissão. Fica na
                        // aba Pessoas, então MEMBRO não cria participante.
                        .requestMatchers(HttpMethod.POST, "/api/pessoa").hasAnyRole(PAPEIS_GESTAO_PARTICIPANTES)
                        .requestMatchers(HttpMethod.DELETE, "/api/pessoa/*").hasAnyRole(PAPEIS_GESTAO_PARTICIPANTES)
                        // Programação é da aba Conteúdo — MEMBRO não mexe em evento. Toda a
                        // diretoria edita (e cadastra os sorteios do evento); criar e excluir
                        // ficam com site, conteúdo e presidência.
                        .requestMatchers(HttpMethod.POST, "/api/evento").hasAnyRole(PAPEIS_CONTEUDO)
                        .requestMatchers(HttpMethod.PUT, "/api/evento/*").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/evento/*").hasAnyRole(PAPEIS_CONTEUDO)
                        // Marcar presença segue com qualquer papel de comissão: quem opera o /checkin
                        .requestMatchers(HttpMethod.POST, "/api/evento/*/presenca").hasAnyRole(PAPEIS_ADMIN)
                        // "Testar leitura" do /checkin (só leitura) — mesmo público de marcar presença
                        .requestMatchers(HttpMethod.GET, "/api/checkin/teste/*").hasAnyRole(PAPEIS_ADMIN)
                        // "INICIAR EVENTO" do /admin: move o marco do atraso pro início real
                        .requestMatchers(HttpMethod.POST, "/api/evento/*/iniciar").hasAnyRole(PAPEIS_CONTEUDO)
                        // Foto do palestrante (telão do debate): envia quem edita evento;
                        // a leitura é pública, vai direto num <img src>.
                        .requestMatchers(HttpMethod.POST, "/api/palestrante/*/foto").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.GET, "/api/palestrante/*/foto").permitAll()
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
                        // Rede de segurança, mesmo motivo de /api/conquista/**: rota nova
                        // de brinde não nasce pública.
                        .requestMatchers("/api/brinde/**").hasAnyRole(PAPEIS_ADMIN)
                        // Sorteios seguem a mesma regra: MEMBRO lista e realiza (elegíveis +
                        // entrega), só a diretoria cria, edita ou exclui o cadastro.
                        .requestMatchers(HttpMethod.GET, "/api/sorteio/**").hasAnyRole(PAPEIS_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/sorteio/*/entrega").hasAnyRole(PAPEIS_ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/sorteio").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.POST, "/api/sorteio/com-brinde").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.PUT, "/api/sorteio/*").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/sorteio/*").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Rede de segurança: rota nova de sorteio não nasce pública.
                        .requestMatchers("/api/sorteio", "/api/sorteio/**").hasAnyRole(PAPEIS_ADMIN)
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
                        .requestMatchers(HttpMethod.POST, "/api/conquista/reavaliar").hasAnyRole(PAPEIS_GAMIFICACAO)
                        // Conquistas de um participante e revogação — mesmo público
                        // da concessão, já que revogar é desfazer uma concessão.
                        .requestMatchers(HttpMethod.GET, "/api/pessoa/*/conquistas").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        .requestMatchers(HttpMethod.DELETE, "/api/pessoa/*/conquistas/*").hasAnyRole(PAPEIS_ADMIN_SEM_MEMBRO)
                        // Configuração do catálogo (texto, pontos, imagem, ativa) —
                        // mesmo público que edita níveis e regras de xp em Informações SEMAC.
                        .requestMatchers(HttpMethod.PUT, "/api/conquista/*").hasAnyRole(PAPEIS_GAMIFICACAO)
                        .requestMatchers(HttpMethod.PATCH, "/api/conquista/*/ativa").hasAnyRole(PAPEIS_GAMIFICACAO)
                        .requestMatchers(HttpMethod.POST, "/api/conquista/*/imagem").hasAnyRole(PAPEIS_GAMIFICACAO)
                        // Rede de segurança: como a regra final é permitAll, qualquer
                        // rota de conquista que venha a existir e não esteja listada
                        // acima nasceria pública. Aqui ela exige ao menos comissão.
                        .requestMatchers("/api/conquista/**").hasAnyRole(PAPEIS_ADMIN)
                        // ── Brinde da comissão (26/10) ──────────────────────────
                        // Toda a comissão: cada um vê o próprio QR no /admin e
                        // qualquer um entrega lendo no /checkin.
                        .requestMatchers("/api/brinde-comissao/**").hasAnyRole(PAPEIS_ADMIN)
                        // ── Times (/time) ───────────────────────────────────────
                        // Revelação do próprio time (id vem do token). Único lugar
                        // em que o participante descobre o time.
                        .requestMatchers(HttpMethod.GET, "/api/time/meu").hasRole(PAPEL_PARTICIPANTE)
                        // Bloco "Seu time" do /participantes: só leitura, e só
                        // mostra o time de quem já o revelou em /time.
                        .requestMatchers(HttpMethod.GET, "/api/time/meu/resumo").hasRole(PAPEL_PARTICIPANTE)
                        // Mascote vai direto num <img src> em /time e no /admin
                        .requestMatchers(HttpMethod.GET, "/api/time/*/mascote").permitAll()
                        // Bloco Times da sub-aba Gamificação: nomes, mascotes,
                        // sorteio e o interruptor. Também é a rede de segurança
                        // das rotas de time que venham a existir sem regra própria.
                        .requestMatchers("/api/time", "/api/time/**").hasAnyRole(PAPEIS_GAMIFICACAO)
                        // ── Termo (/termo) ──────────────────────────────────────
                        // Jogar é do participante: a vitória credita xp e as
                        // tentativas são contadas por pessoa no banco.
                        .requestMatchers(HttpMethod.GET, "/api/termo/hoje").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.GET, "/api/termo/meus").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.POST, "/api/termo/palpite").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.GET, "/api/termo/validar").hasRole(PAPEL_PARTICIPANTE)
                        // Cadastro dos dias e das palavras — mesmo público que
                        // edita níveis e regras de xp em Informações SEMAC. A
                        // palavra nunca volta na resposta, nem para quem a cadastrou.
                        .requestMatchers(HttpMethod.GET, "/api/termo/dias").hasAnyRole(PAPEIS_GAMIFICACAO)
                        .requestMatchers(HttpMethod.PUT, "/api/termo/dias/*").hasAnyRole(PAPEIS_GAMIFICACAO)
                        .requestMatchers(HttpMethod.POST, "/api/termo/dias/*/palavras").hasAnyRole(PAPEIS_GAMIFICACAO)
                        .requestMatchers(HttpMethod.DELETE, "/api/termo/palavras/*").hasAnyRole(PAPEIS_GAMIFICACAO)
                        // Modo teste do /admin -> Termo: joga contra uma palavra
                        // cadastrada sem gravar nada. Mesmo público do cadastro.
                        .requestMatchers(HttpMethod.GET, "/api/termo/teste/*").hasAnyRole(PAPEIS_GAMIFICACAO)
                        .requestMatchers(HttpMethod.POST, "/api/termo/teste/palpite").hasAnyRole(PAPEIS_GAMIFICACAO)
                        // Rede de segurança, mesmo motivo de /api/conquista/**:
                        // rota nova de termo não nasce pública.
                        .requestMatchers("/api/termo/**").hasAnyRole(PAPEIS_ADMIN)
                        // ── Cifra do Dia (/desafio-cifra) ───────────────────────
                        // Jogar é do participante: o acerto credita xp e as
                        // tentativas são contadas por pessoa no banco.
                        .requestMatchers(HttpMethod.GET, "/api/desafio-cifra/hoje").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.GET, "/api/desafio-cifra/meus").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.POST, "/api/desafio-cifra/tentativa").hasRole(PAPEL_PARTICIPANTE)
                        // Listagem dos desafios no /admin (só leitura: eles entram
                        // por migration). Mesmo público do Termo. É a única rota
                        // que devolve as respostas.
                        .requestMatchers(HttpMethod.GET, "/api/desafio-cifra/admin").hasAnyRole(PAPEIS_GAMIFICACAO)
                        // Modo teste do /admin -> Cifra do Dia: joga um desafio
                        // cadastrado sem gravar nada. Mesmo público do cadastro.
                        .requestMatchers(HttpMethod.GET, "/api/desafio-cifra/teste/*").hasAnyRole(PAPEIS_GAMIFICACAO)
                        .requestMatchers(HttpMethod.POST, "/api/desafio-cifra/teste/tentativa").hasAnyRole(PAPEIS_GAMIFICACAO)
                        // Rede de segurança: rota nova da cifra não nasce pública.
                        .requestMatchers("/api/desafio-cifra/**").hasAnyRole(PAPEIS_ADMIN)
                        // ── Mostra Técnica (/mostra/<codigo>) ───────────────────
                        // Ler o QR do stand e ver o próprio progresso é do
                        // participante: as visitas valem a conquista MOSTRA_COMPLETA.
                        .requestMatchers(HttpMethod.POST, "/api/mostra/visitar/*").hasRole(PAPEL_PARTICIPANTE)
                        .requestMatchers(HttpMethod.GET, "/api/mostra/minhas").hasRole(PAPEL_PARTICIPANTE)
                        // Cadastro dos trabalhos e geração dos QR codes — mesmo
                        // público do Termo. Também é a rede de segurança das rotas
                        // de mostra que venham a existir sem regra própria.
                        .requestMatchers("/api/mostra/**").hasAnyRole(PAPEIS_GAMIFICACAO)
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
