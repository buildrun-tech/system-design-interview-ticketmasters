## ADDED Requirements

### Requirement: Chaves JWT fora do classpath
O sistema SHALL obter as chaves de assinatura (privada) e verificação (pública) de JWT exclusivamente em runtime, a partir de variáveis de ambiente injetadas pelo ECS no boot do container. As chaves NÃO SHALL estar presentes como arquivos no classpath ou na imagem Docker publicada.

#### Scenario: Build da imagem sem material de chave
- **WHEN** a imagem Docker da aplicação é construída a partir do código-fonte
- **THEN** a imagem resultante não contém nenhum arquivo `.pem` ou conteúdo de chave privada/pública embutido

#### Scenario: Container recebe as chaves via variável de ambiente
- **WHEN** o ECS inicia uma nova task da aplicação
- **THEN** o container recebe `MP_JWT_VERIFY_PUBLICKEY` e `SMALLRYE_JWT_SIGN_KEY` como variáveis de ambiente já resolvidas a partir do AWS Secrets Manager, antes do processo Java iniciar

### Requirement: Secret único no AWS Secrets Manager com os dois campos de chave
O sistema SHALL armazenar as chaves pública e privada em um único secret do AWS Secrets Manager, em formato JSON com os campos `publicKey` e `privateKey`, nomeado de forma a incluir o prefixo do ambiente (dev/prod).

#### Scenario: Secret nomeado por ambiente
- **WHEN** a infraestrutura é provisionada para o ambiente `dev` ou `prod`
- **THEN** existe um secret distinto no Secrets Manager para cada ambiente, cujo nome inclui o prefixo do respectivo ambiente

### Requirement: Permissão de leitura do secret restrita à execution role do ECS
O sistema SHALL conceder permissão `secretsmanager:GetSecretValue` sobre o secret de chaves JWT apenas à execution role do ECS (responsável por resolver `secrets` no boot da task), e NÃO SHALL conceder essa permissão à task role (usada pela aplicação em runtime).

#### Scenario: Task role sem acesso direto ao secret
- **WHEN** o código da aplicação tenta assumir permissões da task role para chamar `secretsmanager:GetSecretValue` no secret de chaves JWT
- **THEN** a chamada é negada por falta de permissão IAM, pois a aplicação não deve precisar acessar o Secrets Manager diretamente

### Requirement: Falha rápida quando o secret está vazio ou incompleto
O sistema SHALL falhar a inicialização da task ECS (não iniciar o container da aplicação) quando o secret referenciado não existir, estiver vazio, ou não contiver os campos `publicKey` e `privateKey` esperados.

#### Scenario: Secret sem valor populado
- **WHEN** a task definition referencia um secret que ainda não teve nenhum valor colocado manualmente no Secrets Manager
- **THEN** a task ECS falha ao iniciar o container, sem expor a aplicação com chaves ausentes ou inválidas
