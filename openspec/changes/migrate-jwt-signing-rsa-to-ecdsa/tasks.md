## 1. Geração das Chaves EC P-256

- [x] 1.1 Gerar chave privada EC P-256 com `openssl ecparam -name prime256v1 -genkey -noout -out ecPrivateKey.pem`
- [x] 1.2 Extrair chave pública EC com `openssl ec -in ecPrivateKey.pem -pubout -out ecPublicKey.pem`
- [x] 1.3 Copiar `ecPrivateKey.pem` e `ecPublicKey.pem` para `app/src/main/resources/`
- [x] 1.4 Remover os arquivos `rsaPrivateKey.pem` e `publicKey.pem` do classpath

## 2. Configuração do Quarkus

- [x] 2.1 Atualizar `application.properties`: trocar `smallrye.jwt.sign.key.location=classpath:rsaPrivateKey.pem` para `classpath:ecPrivateKey.pem`
- [x] 2.2 Atualizar `application.properties`: trocar `mp.jwt.verify.publickey.location=classpath:publicKey.pem` para `classpath:ecPublicKey.pem`
- [x] 2.3 Adicionar `smallrye.jwt.sign.key-id=ec` e `mp.jwt.verify.algorithm=ES256` se necessário para forçar o algoritmo explicitamente

## 3. Validação

- [ ] 3.1 Executar os testes de integração existentes (`CreateUserIT`, `CreateBookingIT`) para garantir que o fluxo de autenticação continua funcional
- [ ] 3.2 Verificar manualmente com `curl` que o token retornado por `/auth/token` possui `"alg": "ES256"` no header (decodificar com `base64`)
- [ ] 3.3 Verificar que um endpoint protegido aceita o token ES256 emitido
- [ ] 3.4 Confirmar que claims (`iss`, `sub`, `upn`, `groups`, `email`/`app_name`, `exp`) estão preservados no token

## 4. Deploy e Monitoramento

- [ ] 4.1 Fazer deploy em ambiente de staging e repetir validação do item 3
- [ ] 4.2 Identificar consumidores externos que validam JWT localmente e atualizar a chave pública EC distribuída para eles
- [ ] 4.3 Fazer deploy em produção em janela de baixo tráfego
- [ ] 4.4 Monitorar CPU da task ECS por 15 minutos após o deploy e registrar a variação percentual
