## 1. Terraform — Secret no AWS Secrets Manager

- [x] 1.1 Criar `aws_secretsmanager_secret.jwt_keys` (sem `aws_secretsmanager_secret_version` gerenciado) no módulo `ecs`, nomeado `${var.name_prefix}-jwt-keys`
- [x] 1.2 Expor o ARN do secret como output do módulo `ecs` (ex.: `jwt_keys_secret_arn`), se necessário para referência fora do módulo

## 2. Terraform — IAM

- [x] 2.1 Adicionar o ARN do novo secret à lista `Resource` da policy `aws_iam_role_policy.ecs_secrets_policy` (execution role), junto ao `db_password_secret_arn`
- [x] 2.2 Confirmar que a task role (`aws_iam_role.ecs_task_role`) NÃO recebe nenhuma permissão sobre o novo secret

## 3. Terraform — Task Definition

- [x] 3.1 Adicionar ao bloco `secrets` da `aws_ecs_task_definition.app`:
  - `MP_JWT_VERIFY_PUBLICKEY` → `${aws_secretsmanager_secret.jwt_keys.arn}:publicKey::`
  - `SMALLRYE_JWT_SIGN_KEY` → `${aws_secretsmanager_secret.jwt_keys.arn}:privateKey::`
- [x] 3.2 Validar sintaxe/config com `terraform validate` (passou). `terraform plan` real contra AWS não foi executado nesta sessão — requer credenciais/backend do ambiente dev; pendente para quem rodar o rollout.

## 4. Aplicação — Configuração

> **Ajuste de escopo** (a pedido do usuário): em vez de remover os `.pem` do repositório, eles foram movidos para `app/local-keys/` (fora de `src/main/resources`, fora do classpath/imagem) e continuam sendo usados **apenas** pelo profile `local`.

- [x] 4.1 Atualizar `app/src/main/resources/application.properties`: chaves de dev/prod agora vêm de `%dev.mp.jwt.verify.publickey=${MP_JWT_VERIFY_PUBLICKEY}` / `%prod.mp.jwt.verify.publickey=${MP_JWT_VERIFY_PUBLICKEY}` (propriedade de valor literal, escopada por profile para não colidir com o profile `local`)
- [x] 4.2 Idem para `smallrye.jwt.sign.key` (`%dev.`/`%prod.` com `${SMALLRYE_JWT_SIGN_KEY}`)
- [x] 4.3 Movidos `publicKey.pem` e `rsaPrivateKey.pem` de `app/src/main/resources` para `app/local-keys/` (fora do classpath/imagem, mantidos no repo para uso local)
- [x] 4.4 `application-local.properties` (profile `local`) aponta `mp.jwt.verify.publickey.location` / `smallrye.jwt.sign.key.location` para `file:./local-keys/*.pem`; não depende do Secrets Manager

## 5. Rollout — Dev

- [ ] 5.1 Aplicar Terraform em dev (cria o secret vazio, IAM e task definition apontando para ele)
- [ ] 5.2 Popular manualmente o secret em dev com `{"publicKey": "<PEM>", "privateKey": "<PEM>"}`
- [ ] 5.3 Fazer deploy da aplicação em dev e validar emissão de token (`POST /auth/token`, ambos grant types) e acesso a endpoint autenticado
- [ ] 5.4 Validar que a task falha de forma clara se o secret estiver vazio/incompleto (teste antes de popular, se possível em ambiente isolado)

## 6. Rollout — Prod

- [ ] 6.1 Aplicar Terraform em prod (cria o secret vazio, IAM e task definition apontando para ele)
- [ ] 6.2 Popular manualmente o secret em prod com `{"publicKey": "<PEM>", "privateKey": "<PEM>"}`
- [ ] 6.3 Fazer deploy da aplicação em prod e validar emissão/verificação de token

## 7. Documentação

- [x] 7.1 README raiz atualizado: seção "Quick start", "Configuration" (JWT) e "Security note" agora documentam `QUARKUS_PROFILE=local`, `app/local-keys/` e a origem das chaves em dev/prod via Secrets Manager/ECS
- [x] 7.2 Nota adicionada em `openspec/specs/ecdsa-jwt-signing/spec.md` (comentário não-normativo) sinalizando que o requisito de classpath ficará desatualizado quando aquela migração for retomada
