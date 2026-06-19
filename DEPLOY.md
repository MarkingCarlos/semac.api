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

### 5.3 Banco — `ddl-auto`
Em produção evite `spring.jpa.hibernate.ddl-auto=update` mexendo no schema
sozinho. Use `validate` (ou migrações via Flyway/Liquibase) e aplique o DDL
manualmente. Defina por env:
```env
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

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
- [ ] `ddl-auto=validate` e schema aplicado (item 5.3)
- [ ] Tudo sob HTTPS (item 5.4)
- [ ] Nenhum segredo commitado no git

---

## 7. O que NÃO fazer

- ❌ Commitar `JWT_SECRET`, senha do banco ou `.env` de produção no git.
- ❌ Reaproveitar o segredo de dev (`dev-semac-2026-...`) em produção.
- ❌ Trocar o `JWT_SECRET` sem necessidade (invalida todas as sessões).
- ❌ Deixar o CORS em `localhost` achando que "vai funcionar" — não vai.
