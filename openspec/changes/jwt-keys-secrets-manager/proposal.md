## Why

As chaves JWT (`publicKey.pem` e `rsaPrivateKey.pem`) hoje são empacotadas dentro do classpath da aplicação, ou seja, viajam dentro da imagem Docker publicada no ECR. Qualquer pessoa ou processo com acesso à imagem (registry, scan de vulnerabilidade, pull acidental) tem acesso às chaves de assinatura e verificação de tokens. Precisamos remover esse material sensível do artefato de build e passar a entregá-lo apenas em runtime, vindo do AWS Secrets Manager — reaproveitando o mesmo padrão de injeção via ECS já usado para a senha do RDS.

## What Changes

- **BREAKING**: Move os arquivos `publicKey.pem` e `rsaPrivateKey.pem` de `app/src/main/resources` para `app/local-keys/` — deixam de existir no classpath/imagem, mas continuam no repositório para uso exclusivo do profile `local`.
- Adiciona um novo segredo no AWS Secrets Manager (um único secret JSON com os campos `publicKey` e `privateKey`), criado pelo Terraform como container vazio — o valor é populado manualmente fora do Terraform (console/CLI AWS), nunca commitado ou armazenado no state.
- Atualiza o módulo Terraform `ecs` para:
  - conceder `secretsmanager:GetSecretValue` à execution role para o novo secret (mesmo padrão já usado para `db_password_secret_arn`);
  - injetar o conteúdo das chaves como variáveis de ambiente do container via `secrets` na task definition, usando a sintaxe de extração de campo JSON do ECS (`arn:...:publicKey::` e `arn:...:privateKey::`).
- Atualiza `application.properties` para ler as chaves via propriedade de valor literal do SmallRye JWT (`mp.jwt.verify.publickey` / `smallrye.jwt.sign.key`) em vez de `.location=classpath:...`, recebendo o conteúdo PEM através das variáveis de ambiente injetadas pelo ECS.
- Nenhum código Java novo é necessário — a aplicação não chama a AWS SDK diretamente; a extração do secret acontece inteiramente na borda do ECS antes do container iniciar.
- Ambiente local (`application-local.properties`) continua usando arquivos `.pem` de desenvolvimento fora do classpath versionado (não cobertos por este change — fluxo local não muda).

## Capabilities

### New Capabilities
- `jwt-key-secrets-manager`: provisionamento e entrega das chaves de assinatura/verificação JWT via AWS Secrets Manager + injeção nativa do ECS, eliminando a presença das chaves no classpath/imagem.

### Modified Capabilities
(nenhuma — não há spec existente documentando o estado atual "chaves no classpath" como requisito formal a ser alterado)

## Impact

- **Código**: `app/src/main/resources/application.properties` (config de localização das chaves), remoção de `publicKey.pem` e `rsaPrivateKey.pem`.
- **Infraestrutura**: `terraform/modules/ecs/main.tf` (IAM policy + `secrets` na task definition), novo recurso `aws_secretsmanager_secret` (sem `_version` gerenciado pelo Terraform).
- **Operacional**: alguém precisa popular manualmente o valor do secret no Secrets Manager após o `terraform apply` que cria o container vazio, para cada ambiente (dev/prod), antes do primeiro deploy bem-sucedido pós-migração.
- **Spec relacionada (atenção futura)**: `openspec/specs/ecdsa-jwt-signing/spec.md` (migração futura RSA→ES256, ainda não implementada) afirma que "a chave privada EC SHALL estar disponível no classpath da aplicação" — esse requisito ficará desatualizado em relação à nova abordagem e deverá ser revisado quando aquela migração for retomada.
