## Purpose

Publica automaticamente a versão verificada na VPS própria, de forma segura e reversível.

## ADDED Requirements

### Requirement: Deploy após CI verde
O deploy DEVE disparar automaticamente apenas em `main` com o CI verde, e manualmente por `workflow_dispatch` com uma tag já publicada (reimplantação ou reversão); execuções de deploy NÃO DEVEM ocorrer em paralelo.

#### Scenario: Push em main com CI verde
- **WHEN** o CI de um push em `main` termina verde
- **THEN** o deploy constrói e publica as imagens e implanta a nova versão

#### Scenario: Reverter
- **WHEN** o deploy manual é disparado com a tag da versão anterior
- **THEN** a VPS volta para essa versão sem reconstruir imagens

### Requirement: Imagens versionadas pelo commit
As imagens da API e do web DEVEM ser publicadas no GHCR com a tag igual ao SHA do commit.

#### Scenario: Rastreabilidade
- **WHEN** uma versão está no ar
- **THEN** o `/actuator/info` informa o SHA e ele corresponde a um commit do repositório

### Requirement: Acesso de publicação mínimo
O workflow NÃO DEVE ter shell na VPS: a chave SSH de publicação DEVE executar apenas `deploy <tag>`, com a tag validada, e a chave do servidor DEVE estar fixada (sem confiança no primeiro uso).

#### Scenario: Comando fora do padrão
- **WHEN** a chave de publicação tenta executar qualquer outro comando
- **THEN** a VPS o recusa

### Requirement: Implantação segura
O script de deploy DEVE trazer as imagens, subir os serviços e conferir o `/actuator/health`; se a API não ficar saudável no prazo, o deploy DEVE falhar com a versão anterior ainda utilizável e o erro registrado.

#### Scenario: Versão com defeito
- **WHEN** a nova API não fica saudável em 90 s
- **THEN** o job falha e a versão anterior é restaurada

### Requirement: Segredos fora do repositório
Chave SSH, chave do servidor, string de conexão e senhas DEVEM existir apenas em secrets do GitHub e no `.env` da VPS, nunca no repositório, no log ou na imagem.

#### Scenario: Log do workflow
- **WHEN** o log do deploy é inspecionado
- **THEN** nenhum segredo aparece

### Requirement: Isolamento na VPS compartilhada
A aplicação DEVE rodar em diretório, containers e rede próprios (prefixo `agro-`), sem publicar portas, atrás do proxy de borda, com limite de memória nos containers.

#### Scenario: Colisão de nomes
- **WHEN** outra aplicação usa nomes genéricos como `api` ou `web` na mesma rede
- **THEN** os containers deste projeto não colidem porque têm o prefixo `agro-`

### Requirement: Proteção do demo público
O proxy DEVE exigir autenticação básica para métodos que alteram dados (POST, PUT, PATCH, DELETE) e PODE deixar a leitura (GET) aberta; a senha NÃO DEVE constar no repositório.

#### Scenario: Escrita sem credencial
- **WHEN** um cliente anônimo faz `POST /api/producers` no demo público
- **THEN** o proxy responde 401

### Requirement: Banco dedicado
O demo DEVE usar um database e um role exclusivos, sem superusuário, com conexão TLS verificada.

#### Scenario: Escopo do role
- **WHEN** o role do demo tenta acessar outro database
- **THEN** o banco nega
