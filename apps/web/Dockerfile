# Imagem do web: build do Vite e nginx servindo os estáticos, com repasse de /api para a API.
# Contexto de build: raiz do monorepo (precisa do packages/api-client).
FROM node:22-alpine AS build
RUN corepack enable
WORKDIR /repo
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/web/package.json apps/web/
COPY packages/api-client/package.json packages/api-client/
RUN pnpm install --frozen-lockfile --filter web... 
COPY packages/api-client packages/api-client
COPY apps/web apps/web
RUN pnpm --filter @agro/api-client build && pnpm --filter web exec vite build

FROM nginx:1.27-alpine
COPY apps/web/nginx/default.conf.template /etc/nginx/templates/default.conf.template
ENV API_HOST=api
ENV NGINX_ENVSUBST_FILTER=API_HOST
COPY --from=build /repo/apps/web/dist /usr/share/nginx/html
EXPOSE 80
