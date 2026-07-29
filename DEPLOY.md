# Guia de produção — java_api (SEMAC)

Passo a passo para subir o backend em produção com segurança, com foco
nos **segredos** e nas configurações que hoje apontam para `localhost`.

> Regra de ouro: **nenhum segredo no código/repositório**. Em produção
> tudo entra por **variável de ambiente** no servidor.

---

## 1. Variáveis que produção precisa

O backend lê estas configs. Em dev há valores padrão; em produção você
**define todas** no ambiente do servidor.

| Variável | Para que serve | Exemplo |
|---|---|---|
| `JWT_SECRET` | Segredo que assina/valida o token (HS256). **≥ 32 caracteres.** | `R8f2...` (aleatório) |
| `DB_USERNAME` | Usuário do PostgreSQL | `semac_app` |
| `DB_PASSWORD` | Senha do PostgreSQL | `（senha forte）` |
| `SPRING_DATASOURCE_URL` | URL do banco em produção (sobrepõe o `localhost` do `application.properties`) | `jdbc:postgresql://db.interno:5432/semac2026` |

> O Spring Boot permite sobrepor **qualquer** propriedade por variável de
> ambiente (relaxed binding): `spring.datasource.url` →
> `SPRING_DATASOURCE_URL`. É assim que trocamos o banco sem mexer no código.

---

## 2. Gerar um `JWT_SECRET` forte

Gere **uma vez**, guarde em local seguro (cofre de senhas / secret manager)
e **reutilize sempre o mesmo** — se mudar, todos os tokens emitidos param de
valer e os usuários precisam relogar.

**Linux/macOS:**
```bash
openssl rand -base64 48
```

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 }))
```

Copie o resultado — será o valor de `JWT_SECRET`.

---

## 3. Definir as variáveis no servidor

Escolha o método conforme onde o backend roda.

### Opção A — Linux com systemd (VPS/servidor próprio)
Crie um arquivo de ambiente protegido (só leitura para o root/serviço):

```bash
sudo install -m 600 /dev/null /etc/semac/java_api.env
sudo nano /etc/semac/java_api.env
```

Conteúdo:
```env
JWT_SECRET=cole_aqui_o_segredo_gerado
DB_USERNAME=semac_app
DB_PASSWORD=senha_forte_do_banco
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/semac2026
```

No service unit (`/etc/systemd/system/semac-api.service`):
```ini
[Service]
EnvironmentFile=/etc/semac/java_api.env
ExecStart=/usr/bin/java -jar /opt/semac/java_api.jar
Restart=always
```
```bash
sudo systemctl daemon-reload
sudo systemctl restart semac-api
```

### Opção B — Docker
```bash
docker run -d --name semac-api \
  -e JWT_SECRET="cole_aqui_o_segredo" \
  -e DB_USERNAME="semac_app" \
  -e DB_PASSWORD="senha_forte" \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://db:5432/semac2026" \
  -p 8080:8080 \
  semac/java_api:latest
```
Ou, com `docker-compose.yml`, use a seção `environment:` (de preferência
lendo de um arquivo `.env` que **não** vai para o git).

### Opção C — Plataformas PaaS (Railway, Render, Fly.io, Heroku…)
No painel do serviço há uma aba **Environment / Variables**. Adicione
`JWT_SECRET`, `DB_USERNAME`, `DB_PASSWORD` e `SPRING_DATASOURCE_URL` lá.
A plataforma injeta como variáveis de ambiente automaticamente.

### Opção D — Windows Server
```powershell
[Environment]::SetEnvironmentVariable("JWT_SECRET","cole_aqui_o_segredo","Machine")
[Environment]::SetEnvironmentVariable("DB_USERNAME","semac_app","Machine")
[Environment]::SetEnvironmentVariable("DB_PASSWORD","senha_forte","Machine")
[Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_URL","jdbc:postgresql://localhost:5432/semac2026","Machine")
```
Reinicie o serviço/terminal para as variáveis valerem.

---

## 4. Gerar o artefato e rodar

```bash
# na pasta java_api
./mvnw clean package -DskipTests
# gera target/java_api-<versão>.jar
java -jar target/java_api-*.jar
```
Em produção, prefira rodar via systemd/Docker (passos acima) para o
processo reiniciar sozinho e ler o EnvironmentFile.

> Confirme no log de inicialização que **não** apareceu o segredo de dev —
> se `JWT_SECRET` foi lido do ambiente, ele sobrepõe o fallback.

---

## 5. Ajustes obrigatórios de produção (não esquecer!)

### 5.1 CORS — domínio do frontend  ⚠️ exige ajuste de código
Hoje o CORS está **fixo** em `http://localhost:5173`
(`config/SecurityConfig.java`, bean `corsConfigurationSource`). Em produção
o frontend estará num domínio real (ex.: `https://semac.com.br`), então as
chamadas serão **bloqueadas** até trocar isso.

Recomendação: tornar a origem configurável por variável de ambiente. Ex.:
```java
// SecurityConfig
@Value("${app.cors.origins}") String[] origens;   // ...
config.setAllowedOrigins(List.of(origens));
```
e definir `APP_CORS_ORIGINS=https://semac.com.br` no servidor.
*(Posso implementar isso quando você for para produção — hoje está como
localhost de propósito.)*

### 5.2 Frontend — URL da API no build
O frontend chama `import.meta.env.VITE_API_URL || 'http://localhost:8080'`.
No build de produção defina:
```bash
# web_site/.env.production  (ou variável no CI/host de build)
VITE_API_URL=https://api.semac.com.br
```
e rode `npm run build`. Sem isso, o site tentaria falar com `localhost:8080`.

### 5.3 Banco — Flyway (✅ resolvido)
O projeto usa **Flyway** para controlar o schema — o Hibernate não altera
mais o banco sozinho (`spring.jpa.hibernate.ddl-auto=validate`).

- Migrations ficam em `src/main/resources/db/migration/`, nomeadas
  `V<versão>__descricao.sql` (ex.: `V2__adiciona_coluna_x.sql`).
- Ao subir, o Flyway roda automaticamente **antes** do Hibernate: aplica
  qualquer migration pendente e só então o Hibernate valida se as
  entidades batem com o schema resultante.
- `spring.flyway.baseline-on-migrate=true` + `spring.flyway.baseline-version=1`
  cobrem tanto um banco novo (roda `V1` do zero) quanto um banco que já
  tinha o schema criado pelo Hibernate antes do Flyway existir (marca esse
  schema existente como "já na versão 1", sem tentar recriar nada).

**No dia do deploy em produção:**
1. Garanta que o banco de produção **não tenha nenhuma migration aplicada
   ainda** (é esperado — ele nunca rodou Flyway). Se o schema já existir lá
   (por ex. você já rodou a aplicação em produção antes com `ddl-auto=update`),
   o baseline cuida disso sozinho no primeiro start — não precisa rodar nada
   manualmente.
2. Se for um banco de produção **totalmente vazio** (primeira vez), o Flyway
   vai criar o schema inteiro a partir da `V1__schema_inicial.sql` no primeiro
   start da aplicação — também automático.
3. Depois do primeiro deploy, confirme que a tabela `flyway_schema_history`
   foi criada e tem uma linha de sucesso:
   ```sql
   SELECT version, description, success FROM flyway_schema_history;
   ```
4. **Dali em diante**, toda mudança de schema vira uma nova migration
   (`V2__...sql`, `V3__...sql`) commitada no repositório — nunca edite
   entidades esperando que o Hibernate crie a coluna/tabela sozinho.

### 5.4 HTTPS
O token Bearer trafega no header `Authorization`. **Sirva tudo sob HTTPS**
(via proxy reverso — Nginx/Caddy/Cloudflare) para o token não trafegar em
texto puro.

---

## 6. Checklist final

- [ ] `JWT_SECRET` gerado (≥32 chars), guardado em cofre e definido no servidor
- [ ] `DB_USERNAME` / `DB_PASSWORD` / `SPRING_DATASOURCE_URL` definidos
- [ ] CORS apontando para o domínio real do frontend (item 5.1)
- [ ] `VITE_API_URL` definido no build do frontend (item 5.2)
- [ ] Flyway rodou no primeiro start e `flyway_schema_history` tem a `V1` registrada (item 5.3)
- [ ] Tudo sob HTTPS (item 5.4)
- [ ] Nenhum segredo commitado no git

---

## 7. O que NÃO fazer

- ❌ Commitar `JWT_SECRET`, senha do banco ou `.env` de produção no git.
- ❌ Reaproveitar o segredo de dev (`dev-semac-2026-...`) em produção.
- ❌ Trocar o `JWT_SECRET` sem necessidade (invalida todas as sessões).
- ❌ Deixar o CORS em `localhost` achando que "vai funcionar" — não vai.
