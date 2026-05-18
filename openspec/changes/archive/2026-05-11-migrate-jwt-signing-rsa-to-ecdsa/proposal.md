## Why

O endpoint `/auth/token` está com CPU da task ECS chegando a 99% sob carga. A análise identificou que a assinatura JWT com RSA é um dos gargalos CPU-bound — operações RSA-2048 são intrinsecamente custosas. Migrar para ECDSA P-256 oferece segurança equivalente com aproximadamente 15x menos CPU por operação de assinatura, sem mudanças na interface pública da API.

## What Changes

- Geração de novo par de chaves EC P-256 (substituindo RSA-2048)
- Substituição dos arquivos `rsaPrivateKey.pem` e `publicKey.pem` por equivalentes ECDSA
- Atualização das configurações do Quarkus SmallRye JWT para apontar para as novas chaves
- Invalidação de todos os tokens RSA em circulação (tokens expiram em 300s, transição natural)

## Capabilities

### New Capabilities

- `ecdsa-jwt-signing`: Emissão e verificação de JWT utilizando ECDSA P-256 no lugar de RSA-2048

### Modified Capabilities

<!-- Nenhuma mudança de contrato de API — a interface de `/auth/token` permanece idêntica -->

## Impact

- **Arquivos de chave**: `app/src/main/resources/rsaPrivateKey.pem` e `publicKey.pem` serão substituídos
- **Configuração**: `application.properties` — propriedades `smallrye.jwt.sign.key.location` e `mp.jwt.verify.publickey.location`
- **Tokens em circulação**: tokens RSA existentes se tornam inválidos ao trocar a chave pública de verificação — janela de impacto máxima de 300s (TTL atual dos tokens)
- **Consumidores do JWT**: qualquer sistema que valide o JWT localmente (sem chamar o servidor) precisa atualizar a chave pública; sistemas que dependem apenas do endpoint de autenticação não são afetados
- **Sem mudança de contrato**: payload do JWT, claims e endpoint `/auth/token` permanecem idênticos
