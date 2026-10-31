# Publicação na Vercel

O projeto usa o **Container Runtime (Beta)** da Vercel, com o `Dockerfile` da raiz. A imagem inclui Java 25, FFmpeg, FFprobe, Python, yt-dlp e Node; interface e API são servidas juntas pelo Spring Boot. Não é necessário converter a aplicação para Next.js nem separar o frontend.

## Importar o repositório

1. Envie este projeto para um repositório Git acessível à sua conta Vercel.
2. Na Vercel, use **Add New → Project**, selecione o repositório e mantenha a raiz do projeto como Root Directory.
3. Confirme o preset **Container**, detectado pelo `Dockerfile`. Deixe os comandos de build e diretório de saída sem overrides; o Dockerfile faz o build Gradle.
4. Opcionalmente, cadastre `HELPERHUB_IMAGE_REMOVE_BG_API_KEY` nas variáveis de ambiente para habilitar remove.bg. Não coloque a chave no repositório.
5. Faça o deploy. Confira `/actuator/health` e teste um arquivo pequeno pela interface.

A imagem escuta na porta 80 por padrão, esperada pela Vercel. Se definir `PORT` nas configurações do projeto, a aplicação e o healthcheck usam esse valor. Localmente, `docker compose up -d --build` continua disponível em `http://localhost:8080`; o Compose define `PORT=8080`. Sem Docker, o padrão também continua sendo 8080.

## Restrição importante: arquivos grandes

**Esta preparação permite publicar o contêiner, mas não resolve o limite de transporte da Vercel.** A documentação informa um limite de 4,5 MB por corpo de requisição/resposta de Functions, também aplicável ao Container Runtime. O formulário multipart inclui metadados além do arquivo. Mesmo uma entrada pequena pode produzir uma saída maior que esse limite, por exemplo ao converter MP3 para WAV.

Os limites locais do HelperHub (uploads de 50 MiB e downloads de vídeo de até 250 MiB) permanecem iguais. Portanto, não considere que todas essas operações funcionarão na Vercel apenas por o deploy estar saudável. Alterar o limite do Spring não aumenta o limite da plataforma. O tempo máximo de cada requisição também depende do plano/configuração da Function; o timeout de 900 segundos do yt-dlp não garante que a plataforma espere esse período.

Para preservar o suporte a arquivos grandes na Vercel, o próximo trabalho é implementar upload direto para armazenamento de objetos (por exemplo, Blob), processamento com identificação de tarefa e download direto do resultado armazenado, incluindo limpeza e controle de acesso. Isso ainda não está implementado. Outra possibilidade é hospedar o contêiner em um serviço cujo limite de requisição comporte o fluxo atual.

Os limites de concorrência e de requisições do HelperHub são por instância, não globais. O rate limit usa o IP da conexão e pode agrupar usuários atrás do proxy. O disco temporário não guarda arquivos de forma persistente. O Compose local não é executado pela Vercel; os limites dele não configuram a infraestrutura remota.

## Referências

- Container Runtime, detecção do Dockerfile e porta: https://vercel.com/docs/functions/runtimes/container
- Limites das Functions: https://vercel.com/docs/functions/limitations
- Upload direto de arquivos grandes: https://vercel.com/kb/guide/how-to-bypass-vercel-body-size-limit-serverless-functions

A configuração deve ser validada na conta de destino após a importação; nenhum deploy remoto foi realizado nesta preparação.
