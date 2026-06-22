## Context

A aplicação Quarkus (`ticketmaster`) assina e verifica JWTs com SmallRye JWT, hoje configurado para ler as duas chaves RSA de arquivos no classpath:

```
mp.jwt.verify.publickey.location=classpath:publicKey.pem
smallrye.jwt.sign.key.location=classpath:rsaPrivateKey.pem
```

Os dois `.pem` estão versionados em `app/src/main/resources` e, portanto, embutidos na imagem Docker publicada no ECR — qualquer pull da imagem expõe as chaves.

O repositório já resolve um problema equivalente para a senha do RDS (`terraform/modules/rds/main.tf` + `terraform/modules/ecs/main.tf`): um `aws_secretsmanager_secret` é criado pelo Terraform, a execution role do ECS recebe `secretsmanager:GetSecretValue` escopado a esse ARN, e o valor é injetado como variável de ambiente via o bloco `secrets` da task definition — sem nenhum código Java envolvido. O agente do Fargate resolve o secret antes do container iniciar.

## Goals / Non-Goals

**Goals:**
- Remover `publicKey.pem` e `rsaPrivateKey.pem` do classpath/imagem Docker.
- Entregar o conteúdo das chaves ao processo Java em runtime, exclusivamente via injeção nativa do ECS (sem AWS SDK na aplicação, sem IAM na task role).
- Reaproveitar fielmente o padrão já estabelecido para `db_password_secret_arn`.
- Manter o secret em si fora do controle do Terraform (criado vazio pelo Terraform, populado manualmente na AWS).

**Non-Goals:**
- Rotação automática de chaves em runtime sem restart do container (fora de escopo; troca de chave = atualizar o secret + novo deploy/restart da task).
- Migração do algoritmo de assinatura (RS256 → ES256); isso é coberto por outro change futuro (`ecdsa-jwt-signing`), hoje apenas especificado e não implementado.
- Alterar o fluxo de desenvolvimento local (`application-local.properties` continua com chaves locais fora do classpath versionado, sem mudanças).

## Decisions

### 1. Um único secret JSON com dois campos, não dois secrets
`{"publicKey": "<PEM>", "privateKey": "<PEM>"}` em `aws_secretsmanager_secret.jwt_keys`, nomeado `${name_prefix}-jwt-keys`.
- **Alternativa considerada**: dois secrets (`jwt-public-key`, `jwt-private-key`). Rejeitada por duplicar recursos Terraform e IAM statements sem benefício — as duas chaves têm o mesmo ciclo de vida (sempre trocam juntas) e o mesmo consumidor (a própria task).
- O ECS suporta extrair um campo específico de um secret JSON na referência `valueFrom` usando a sintaxe `<secret-arn>:<json-key>::`, então não perdemos granularidade por usar um único secret.

### 2. Secret criado pelo Terraform, valor populado manualmente (fora do Terraform)
O recurso `aws_secretsmanager_secret.jwt_keys` é gerenciado pelo Terraform (container vazio), mas **não** existe um `aws_secretsmanager_secret_version` gerenciado por ele.
- **Alternativa considerada**: gerar as chaves via Terraform (ex.: `tls_private_key` + `random_password`), como é feito com `random_password.db_password` no módulo RDS. Rejeitada porque o usuário quer popular manualmente — chaves JWT (diferente de senha de banco) podem precisar ser geradas/rotacionadas por um processo externo (ex.: HSM, ferramenta de PKI) e não devem viver em plaintext no `.tfstate`.
- Consequência operacional: após `terraform apply` criar o secret vazio, alguém precisa rodar `aws secretsmanager put-secret-value` (ou via console) antes do primeiro deploy bem-sucedido da task — a task falhará ao iniciar se o secret não tiver um valor JSON válido com os dois campos.

### 3. Injeção via ECS `secrets` (env vars), não leitura ativa via SDK na aplicação
A task definition referencia dois campos do mesmo secret como duas env vars distintas:
```
secrets = [
  { name = "MP_JWT_VERIFY_PUBLICKEY", valueFrom = "${aws_secretsmanager_secret.jwt_keys.arn}:publicKey::" },
  { name = "SMALLRYE_JWT_SIGN_KEY",   valueFrom = "${aws_secretsmanager_secret.jwt_keys.arn}:privateKey::" }
]
```
- **Alternativa considerada** (a ideia original do usuário): a aplicação chama o SDK da AWS no startup, busca o secret, faz parse do JSON e injeta as chaves na configuração do SmallRye JWT programaticamente. Rejeitada porque exige: nova dependência (SDK Secrets Manager), permissão IAM na task role (superfície maior que a execution role, que só age no boot), e um `ConfigSourceFactory` customizado para injetar valores antes do MicroProfile Config resolver `mp.jwt.*` — complexidade não justificada quando o ECS já resolve isso nativamente, no mesmo padrão usado para a senha do banco.
- `mp.jwt.verify.publickey` e `smallrye.jwt.sign.key` (propriedades de **valor literal**, diferentes de `.location`) aceitam o conteúdo PEM diretamente, então a env var injetada pelo ECS é lida pelo SmallRye JWT sem nenhum código adicional.

### 4. IAM: reaproveitar a policy existente, escopada ao novo ARN
A policy `aws_iam_role_policy.ecs_secrets_policy` (já existente, presa à execution role) passa a incluir o ARN do novo secret na lista `Resource`, junto ao `db_password_secret_arn`. Nenhuma mudança na task role.

## Risks / Trade-offs

- **[Risco]** Task falha no boot se o secret estiver vazio ou com JSON malformado (chave `publicKey`/`privateKey` ausente) → **[Mitigação]** documentar o passo manual de popular o secret como pré-requisito de deploy em cada ambiente (dev/prod), antes do primeiro `terraform apply` que afeta a task definition; adicionar verificação no checklist de deploy.
- **[Risco]** PEM multi-linha em variável de ambiente pode ter problemas de parsing dependendo de como o SmallRye JWT lê valores literais vs. localização de arquivo → **[Mitigação]** validar localmente (ex.: `docker run` simulando a env var) antes de promover para dev/prod; SmallRye JWT aceita PEM com quebras de linha em propriedade de valor literal, comportamento documentado e usado por outros projetos Quarkus.
- **[Trade-off]** Sem rotação automática: troca de chave exige atualizar o secret manualmente E forçar um novo deploy/restart da task (ECS só resolve `secrets` no boot do container, não em runtime). Aceitável dado que é o mesmo comportamento já aceito para a senha do RDS.
- **[Risco]** Ambiente dev e prod precisam de secrets/chaves distintos (não reusar a mesma chave privada entre ambientes) → **[Mitigação]** o secret é nomeado com `name_prefix` (que já inclui o ambiente), garantindo secrets separados por ambiente automaticamente.

## Migration Plan

1. Terraform: criar `aws_secretsmanager_secret.jwt_keys` (sem version) no módulo `ecs` (ou `rds`-like local module), expor `jwt_keys_secret_arn` como output se necessário.
2. Terraform: estender `aws_iam_role_policy.ecs_secrets_policy` para incluir o novo ARN.
3. Terraform: adicionar o bloco `secrets` à `aws_ecs_task_definition.app` com as duas env vars.
4. `terraform apply` em **dev** primeiro — isso cria o secret vazio, mas a task ainda não deve ser atualizada até o passo 5.
5. Popular manualmente o secret em **dev** com `{"publicKey": "...", "privateKey": "..."}` (chaves atuais ou novas, a definir por quem executa).
6. Atualizar `application.properties` (`mp.jwt.verify.publickey` / `smallrye.jwt.sign.key` no lugar de `.location`), remover os arquivos `.pem` do classpath.
7. Deploy da aplicação em dev, validar emissão e verificação de token (`POST /auth/token`, endpoint autenticado).
8. Repetir 4–7 para **prod**.

**Rollback**: reverter o commit que remove os `.pem` do classpath e a mudança de `application.properties`; o secret/IAM/task definition no Terraform podem permanecer (não atrapalham se não usados) ou ser revertidos junto via `terraform apply` do estado anterior.

## Open Questions

- As chaves atuais (`publicKey.pem` / `rsaPrivateKey.pem`) serão reaproveitadas (copiadas para o secret) ou serão geradas novas chaves no momento da migração? Isso é decidido por quem popula o secret manualmente, fora deste change.
- Quem é responsável por popular o secret em cada ambiente antes do deploy (necessário antes de aplicar/concluir as tasks deste change)?
