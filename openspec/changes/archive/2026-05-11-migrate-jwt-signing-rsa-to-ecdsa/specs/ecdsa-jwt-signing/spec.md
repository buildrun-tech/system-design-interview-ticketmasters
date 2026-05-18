## ADDED Requirements

### Requirement: JWT emitido com algoritmo ES256
O sistema SHALL emitir tokens JWT assinados com ECDSA P-256 (algoritmo `ES256`), substituindo o algoritmo RSA-2048 (`RS256`) anteriormente utilizado. A chave privada EC SHALL estar disponível no classpath da aplicação.

#### Scenario: Token emitido com grant type password
- **WHEN** um cliente envia credenciais válidas (username + password) para `POST /auth/token` com `grant_type=password`
- **THEN** o sistema retorna um JWT cujo header contém `"alg": "ES256"` e o token é verificável com a chave pública EC correspondente

#### Scenario: Token emitido com grant type client_credentials
- **WHEN** um cliente envia credenciais válidas (clientId + clientSecret) para `POST /auth/token` com `grant_type=client_credentials`
- **THEN** o sistema retorna um JWT cujo header contém `"alg": "ES256"` e o token é verificável com a chave pública EC correspondente

### Requirement: Verificação de JWT com chave pública EC
O sistema SHALL verificar tokens JWT utilizando a chave pública ECDSA P-256 configurada. Tokens assinados com RSA SHALL ser rejeitados após a migração.

#### Scenario: Requisição autenticada com token ES256 válido
- **WHEN** um cliente apresenta um Bearer token ES256 válido (não expirado, assinado com a chave EC correta) em um endpoint protegido
- **THEN** o sistema aceita a requisição e processa normalmente

#### Scenario: Requisição com token RS256 (pré-migração) rejeitada
- **WHEN** um cliente apresenta um Bearer token RS256 (assinado com a antiga chave RSA) após o deploy da migração
- **THEN** o sistema retorna HTTP 401 Unauthorized

### Requirement: Claims do JWT inalterados
O conteúdo dos claims JWT (issuer, subject, groups, email, app_name, expiresIn) SHALL permanecer idêntico ao comportamento anterior. A migração de algoritmo não deve alterar o contrato de payload.

#### Scenario: Claims do token password grant preservados
- **WHEN** um token é emitido via `grant_type=password`
- **THEN** o JWT contém os claims: `iss=ticketmaster`, `upn=<username>`, `sub=<userId>`, `groups=<roles+scopes>`, `email=<email>`, e `exp` com TTL de 300s

#### Scenario: Claims do token client_credentials preservados
- **WHEN** um token é emitido via `grant_type=client_credentials`
- **THEN** o JWT contém os claims: `iss=ticketmaster`, `upn=<clientId>`, `sub=<appId>`, `groups=<scopes>`, `app_name=<name>`, e `exp` com TTL de 300s
