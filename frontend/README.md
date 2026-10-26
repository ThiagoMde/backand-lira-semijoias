# Lira Semijoias — Front end (Angular)

Loja virtual e painel administrativo em Angular 21 (componentes standalone + signals),
conectados a todas as rotas da API Spring Boot deste repositório.

## Como rodar

```bash
# 1. Suba o back end (porta 8080)
cd ..            # raiz do repositório
./mvnw spring-boot:run

# 2. Em outro terminal, rode o front
cd frontend
npm install
npm start        # http://localhost:4200
```

Em desenvolvimento, o `proxy.conf.json` encaminha `/api` → `http://localhost:8080`,
então não é preciso mexer no CORS do back end.

### Produção

```bash
npm run build    # gera dist/frontend/browser
```

Edite `src/environments/environment.ts` (`apiUrl`) com a URL pública da API, por
exemplo `https://api.seudominio.com.br/api`, e inclua o domínio do front na
variável `CORS_ALLOWED_ORIGINS` do back end. Configure o servidor estático para
redirecionar rotas desconhecidas para `index.html` (SPA).

## Telas

### Loja (público)
| Rota | O que faz | Endpoint |
|---|---|---|
| `/` | Vitrine com destaques e categorias | `GET /api/products`, `GET /api/categories` |
| `/produtos?busca=&categoria=` | Catálogo com busca, filtro e ordenação | `GET /api/products?search=&categoryId=` |
| `/produto/:slug` | Detalhe do produto, galeria, quantidade | `GET /api/products/slug/{slug}` |
| `/carrinho` | Sacola (salva no navegador) | — |
| `/checkout` | Dados do cliente e tipo de entrega | `POST /api/orders/checkout` |
| `/pedido-realizado` | Número do pedido + botão para enviar no WhatsApp | — |

### Painel (`/admin`, exige login)
| Rota | Perfil | O que faz | Endpoints |
|---|---|---|---|
| `/admin/login` | — | Login com JWT | `POST /api/auth/login` |
| `/admin` | ADMIN, EMPLOYEE | Indicadores, pedidos pendentes, estoque baixo | `GET /api/admin/dashboard` |
| `/admin/produtos` | ADMIN, EMPLOYEE | Lista, filtros, ativar/desativar (ADMIN) | `GET /api/admin/products`, `PATCH /{id}/status` |
| `/admin/produtos/novo` | ADMIN, EMPLOYEE | **Cadastrar produto pelo front**, com fotos | `POST /api/admin/products`, `POST /api/admin/uploads/images`, `POST /api/admin/products/{id}/images` |
| `/admin/produtos/:id` | ADMIN, EMPLOYEE | Editar, trocar foto principal, remover fotos | `PUT /api/admin/products/{id}`, `DELETE /{id}/images`, `DELETE /api/admin/uploads/images` |
| `/admin/categorias` | ADMIN | Criar, editar, ativar/desativar | `/api/admin/categories` (GET, POST, PUT, PATCH status) |
| `/admin/pedidos` | ADMIN, EMPLOYEE | Lista com filtro de status | `GET /api/admin/orders?status=` |
| `/admin/pedidos/:id` | ADMIN, EMPLOYEE | Confirmar, cancelar, mudar status | `GET /{id}`, `PATCH /{id}/confirm`, `/cancel`, `/status` |
| `/admin/estoque` | ADMIN, EMPLOYEE | Entrada/saída, estoque baixo, histórico | `/api/admin/stock/entry`, `/exit`, `/low`, `/movements`, `/movements/product/{id}` |
| `/admin/usuarios` | ADMIN | Criar usuários e ativar/desativar | `/api/admin/users` (GET, POST, PATCH status) |

## Cadastro de produtos pelo front

Em **Painel → + Novo produto**:

1. Preencha nome, SKU, categoria (administradores podem criar uma categoria ali mesmo),
   descrição, preço, preço promocional, estoque inicial e estoque mínimo.
2. Adicione fotos arrastando arquivos, clicando em "Adicionar fotos" ou colando uma URL.
   Escolha qual é a **principal**.
3. Clique em **Cadastrar produto**. O produto é criado, as fotos são enviadas ao
   Cloudinary e vinculadas ao produto. Marque "Cadastrar outro em seguida" para
   cadastrar vários em sequência.

> O upload de arquivos usa o Cloudinary configurado no back end
> (`CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`).
> Sem essas variáveis, use a opção de colar a URL da imagem.

### Primeiro administrador

Defina as variáveis de ambiente antes de subir o back end:

```bash
ADMIN_NAME="Seu Nome" ADMIN_EMAIL=admin@seudominio.com ADMIN_PASSWORD=senhaforte123 ./mvnw spring-boot:run
```

Na inicialização, o back end (`AdminBootstrap`) cria esse usuário com perfil ADMIN
se o e-mail ainda não existir (senha com no mínimo 8 caracteres). Se já existir,
nada é alterado. Os demais usuários podem ser criados pela tela **Usuários** do painel.

## Estrutura

```
src/app/
  core/
    models.ts               # tipos espelhando DTOs/modelos do back end
    services/api.services.ts# um serviço por controller (todas as rotas)
    services/auth.service.ts# sessão JWT (localStorage)
    services/cart.service.ts# sacola
    interceptors/           # envia Authorization nas rotas /admin; 401 → logout
    guards/                 # authGuard e roleGuard (ADMIN)
  shared/                   # card de produto, toasts
  pages/store/              # loja
  pages/admin/              # painel
```
