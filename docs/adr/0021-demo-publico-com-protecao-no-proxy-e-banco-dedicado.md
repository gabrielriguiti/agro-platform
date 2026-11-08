# 0021. Demo público com proteção no proxy e banco dedicado

- Status: aceito
- Data: 2026-11-07
- Decisores: Gabriel
- Change relacionado: `openspec/changes/delivery`

## Contexto

O demo fica publicado em `agro.gabrielriguiti.com.br` para quem avalia o projeto. A API não tem autenticação por decisão deliberada ([ADR-0019](0019-sem-autenticacao-nesta-entrega.md)), então qualquer pessoa na internet poderia criar, alterar ou apagar dados. A VPS é compartilhada com outros projetos e hospeda o PostgreSQL em container que o demo usa.

## Opções consideradas

1. **Demo totalmente aberto**: simples, mas exposto a vandalismo e a lixo no banco.
2. **Autenticação na aplicação**: é o caminho de produção, mas está fora do escopo desta entrega e do ADR-0019.
3. **Autenticação básica no proxy, só para escrita**: protege o que importa sem mudar a aplicação; a leitura continua aberta para quem só quer ver.

## Decisão

Opção 3. O Caddy exige autenticação básica para `POST`, `PUT`, `PATCH` e `DELETE`; `GET` fica aberto. Usuário e hash da senha vêm de variáveis de ambiente do proxy e nunca do repositório; a senha é enviada aos avaliadores em privado. Os dados são fictícios (profile `demo`). O banco é um database e um role exclusivos (`agro_demo`), sem superusuário, no PostgreSQL em container da própria VPS: a conexão é interna, pela rede Docker, sem TLS (`sslmode=disable`) e com a porta publicada só em `127.0.0.1`. O `CONNECT` é revogado de `PUBLIC` nos demais bancos, para o role não alcançar nada além do `agro_demo`. Se o banco passar a ficar fora da VPS, a conexão deve usar TLS verificado (`verify-full`). A string de conexão existe só no `.env` da VPS. Os containers têm prefixo `agro-`, rede própria (mais a do proxy e a do banco), limite de memória e nenhuma porta publicada.

## Consequências

- Positivas: o demo é seguro o bastante para ficar público sem alterar a aplicação; um role comprometido só alcança o `agro_demo`.
- Negativas e riscos: a proteção está fora da aplicação, então rodar a API sem o proxy a deixa aberta (por isso nenhuma porta é publicada); a autenticação real de produção continua pendente (ADR-0019).
- Documentação: `docs/deploy.md` descreve a criação do banco, do role e do hash da senha.
