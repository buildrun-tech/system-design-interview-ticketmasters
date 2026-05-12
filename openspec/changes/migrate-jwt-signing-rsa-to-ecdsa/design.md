## Context

O serviço utiliza SmallRye JWT (Quarkus) para emissão e verificação de tokens. Atualmente, a assinatura usa RSA-2048 com chaves armazenadas em PEM dentro do classpath da aplicação (`rsaPrivateKey.pem` / `publicKey.pem`). 

Sob carga no endpoint `/auth/token`, a task ECS atinge 99% de CPU. A análise identificou que operações RSA são um contribuinte significativo: RSA-2048 signing exige exponenciação modular com números grandes, enquanto ECDSA P-256 usa aritmética de curva elíptica — operação muito mais leve com segurança equivalente (~128 bits).

Estado atual:
```
smallrye.jwt.sign.key.location=classpath:rsaPrivateKey.pem   (RSA-2048)
mp.jwt.verify.publickey.location=classpath:publicKey.pem     (RSA-2048 public)
```

## Goals / Non-Goals

**Goals:**
- Substituir o algoritmo de assinatura JWT de RS256 (RSA) para ES256 (ECDSA P-256)
- Reduzir CPU gasto em signing/verification por token emitido (~15x)
- Manter contrato externo do endpoint `/auth/token` intacto (mesmo payload, mesmos claims)
- Garantir transição sem downtime com janela de invalidação ≤ 300s (TTL atual dos tokens)

**Non-Goals:**
- Implementar rotação automática de chaves
- Adicionar suporte a múltiplos algoritmos simultaneamente (JWKS dinâmico)
- Alterar a estrutura de claims do JWT
- Cache de tokens (tratado em mudança separada)

## Decisions

### 1. Algoritmo: ES256 (ECDSA com P-256 e SHA-256)

**Escolhido:** ES256  
**Alternativas consideradas:**
- `RS256` (RSA-2048) — atual, CPU-intensivo
- `ES384` / `ES512` — curvas maiores (P-384, P-521), mais seguras mas mais lentas que P-256 e sem benefício prático para tokens de sessão de 300s
- `EdDSA` (Ed25519) — ainda mais rápido que ECDSA, mas suporte no SmallRye JWT requer verificação de compatibilidade com a versão do Quarkus

**Rationale:** ES256 é o padrão da indústria para JWT, suportado nativamente pelo SmallRye JWT/Jose4j, e já é amplamente compatível com clientes que consomem o token.

### 2. Geração das chaves: offline com OpenSSL

As chaves EC serão geradas offline com `openssl` e commitadas no classpath, seguindo o mesmo padrão das chaves RSA atuais. Não há necessidade de geração dinâmica para este contexto.

```bash
# Gerar chave privada EC P-256
openssl ecparam -name prime256v1 -genkey -noout -out ec_private.pem

# Extrair chave pública
openssl ec -in ec_private.pem -pubout -out ec_public.pem
```

### 3. Transição: big-bang com janela de invalidação

**Escolhido:** troca direta (big-bang) aproveitando o TTL curto dos tokens  
**Alternativas consideradas:**
- Suporte dual (RSA + EC por período de transição): complexidade desnecessária dado TTL de 300s
- Endpoint de revogação: over-engineering para este caso

**Rationale:** Tokens têm TTL de 300s. Após o deploy, tokens RSA existentes falharão na verificação (chave pública muda). O impacto máximo é de 300s de requests com tokens inválidos — aceitável em janela de manutenção ou deploy rolling com recarga rápida de token pelos clientes.

## Risks / Trade-offs

| Risco | Mitigação |
|-------|-----------|
| Tokens RSA em voo se tornam inválidos no deploy | Deploy em janela de baixo tráfego; clientes devem re-autenticar (comportamento esperado ao trocar chaves) |
| Consumidores externos com chave pública hardcoded | Identificar consumidores antes do deploy e atualizar a chave pública distribuída |
| Compatibilidade do SmallRye JWT com ES256 | Verificada: SmallRye JWT suporta ES256 via `jose4j`. Sem mudança de dependência necessária. |
| Chave privada EC no classpath | Risco idêntico ao atual (RSA no classpath). Não piora a postura de segurança. |

## Migration Plan

1. Gerar par de chaves EC P-256 localmente
2. Substituir `rsaPrivateKey.pem` → `ecPrivateKey.pem` e `publicKey.pem` → `ecPublicKey.pem` em `app/src/main/resources/`
3. Atualizar `application.properties` com os novos paths e o algoritmo `ES256`
4. Rodar os testes de integração localmente para validar emissão e verificação
5. Fazer deploy em ambiente de staging e validar o fluxo end-to-end de `/auth/token`
6. Deploy em produção em janela de baixo tráfego
7. Monitorar CPU da task ECS nos primeiros 15 minutos pós-deploy

**Rollback:** reverter os arquivos PEM e `application.properties` para os valores RSA e fazer redeploy. Tokens emitidos com EC falharão, clientes re-autenticam.

## Open Questions

- Há consumidores externos (fora do cluster) que validam o JWT localmente com a chave pública RSA atual? Se sim, precisam receber a nova chave pública EC antes do deploy.
