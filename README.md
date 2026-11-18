# HelperHub

Aplicação web para comprimir, redimensionar e converter imagens, remover fundos, cortar, converter e extrair áudio de vídeos e baixar vídeos do YouTube. Interface em português e API REST no mesmo processo Spring Boot, sem banco ou cadastro.

## Executar no Windows

Requisitos: **JDK 25**, FFmpeg e FFprobe no `PATH`. Para vídeos: Python 3.10+ e Node.js 22+ no `PATH`, além do yt-dlp. O Gradle Wrapper está incluído.

```powershell
# Se necessário, selecione seu JDK 25:
$env:JAVA_HOME = 'C:\Users\Pichau\.jdks\openjdk-25.0.1'

# Instalação isolada do downloader (não altera o Python global):
python -m venv .tools/yt-dlp
.\.tools\yt-dlp\Scripts\python.exe -m pip install -r requirements-video.txt
$env:HELPERHUB_VIDEO_YT_DLP_COMMAND = (Resolve-Path .tools/yt-dlp/Scripts/yt-dlp.exe).Path

# Opcional: informe a chave sem colocá-la no código.
# $env:HELPERHUB_IMAGE_REMOVE_BG_API_KEY = 'sua-chave'

.\gradlew.bat bootRun
```

Abra <http://localhost:8080>. A documentação interativa local está em <http://localhost:8080/swagger-ui/index.html> e o contrato OpenAPI em <http://localhost:8080/v3/api-docs>.

Em produção, a documentação fica acessível pela rede Tailscale em <https://srv1760039.tail41fec8.ts.net:4003/swagger-ui/index.html>. Conecte o dispositivo ao Tailscale para abrir essa URL. O domínio público bloqueia a interface, o contrato OpenAPI, a configuração do Swagger e os assets da documentação. A regra está em `ops/helperhub-private-docs.yaml`, instalada em `/data/coolify/proxy/dynamic/helperhub-private-docs.yaml`. O Tailscale Serve encaminha a porta 4003 para `127.0.0.1:14003`, a porta de loopback do contêiner configurada no Coolify. A API de processamento continua disponível pelo domínio público.

A chave remove.bg é necessária somente para remover fundos. Essa operação envia a imagem ao serviço externo e usa os créditos da conta configurada. As demais operações continuam disponíveis sem a chave.

No Linux/macOS, use `bash gradlew`, `.tools/yt-dlp/bin/python` e `.tools/yt-dlp/bin/yt-dlp` nos comandos equivalentes. Variáveis usam a sintaxe `export NOME=valor`.

## Testar e empacotar

```powershell
.\gradlew.bat test bootJar
```

Os testes incluem arquivos reais PNG/WebP, corte WAV, conversão WAV→MP3, validação de URLs, limites por IP/concorrência e subprocessos locais para reproduzir timeout, falha e limpeza de downloads. **FFmpeg e FFprobe são necessários para a suíte completa.** Não são usados créditos remove.bg nem downloads externos na suíte automatizada.

O JAR executável fica em `build/libs/api-0.0.1-SNAPSHOT.jar`. Execute com Java 25:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -jar build/libs/api-0.0.1-SNAPSHOT.jar
```

O relatório fica em `build/reports/tests/test/index.html`. O workflow `.github/workflows/ci.yml` instala FFmpeg, executa testes e publica o JAR como artefato quando este diretório estiver em um repositório GitHub.

## API

Todas as operações são `POST`. Envie campos como `multipart/form-data`; o download do YouTube também aceita formulário URL-encoded. As respostas bem-sucedidas são arquivos com `Content-Disposition: attachment`.

| Endpoint | Campos |
| --- | --- |
| `/api/image/compress` | `file`; opcionais `jpegQuality=1..100` (75 por padrão), `outputFormat=JPEG/PNG` (JPEG por padrão). Em JPEG, `targetBytes=1024..10000000` ativa a meta por arquivo e substitui a qualidade manual. Com `allowResize=true`, reduz as dimensões se ajustar a qualidade for insuficiente. |
| `/api/image/batch` | 1 a 5 campos `files` e `operation=COMPRESS/CONVERT/RESIZE`, com as opções da operação correspondente. Retorna um ZIP. |
| `/api/image/edit` | `file`; opcionais `x`, `y`, `width`, `height` (recorte em pixels, os quatro juntos), `rotation=0/90/180/270` (giro horário), `flipHorizontal`, `flipVertical`, `outputFormat=PNG/JPEG/WEBP` (PNG por padrão) e `jpegQuality=1..100`. Aplica recorte, giro e espelhamento, nessa ordem. |
| `/api/image/resize` | `file`, `maxWidth` e/ou `maxHeight`; opcionais `outputFormat=AUTO/PNG/JPEG/WEBP`, `jpegQuality=1..100` |
| `/api/image/convert` | `file`, `outputFormat=PNG/JPEG/WEBP`; opcional `jpegQuality` |
| `/api/image/remove-background` | `file` (JPG, PNG ou WebP); retorna PNG |
| `/api/audio/extract` | `file` de vídeo com áudio, `targetFormat=MP3/WAV/AAC/OGG/FLAC/OPUS`. Extrai a primeira faixa de áudio. |
| `/api/audio/cut` | `file`, `startMs`, `endMs` (milissegundos) |
| `/api/audio/convert` | `file`, `targetFormat=MP3/WAV/AAC/OGG/FLAC/OPUS` (AAC gera M4A) |
| `/api/video/youtube-download` | `url` HTTPS de `youtube.com/watch`, `youtube.com/shorts` ou `youtu.be`; opcional `outputFormat=MP4/WEBM` |

```powershell
curl.exe -f -F "file=@imagem.webp" -F "maxWidth=800" http://localhost:8080/api/image/resize -o imagem.png
curl.exe -f -F "file=@audio.wav" -F "targetFormat=MP3" http://localhost:8080/api/audio/convert -o audio.mp3
```

Erros incluem `error`, `message` e, quando disponíveis, `details`. Códigos principais: 400 (campos inválidos), 413 (upload excedido), 422 (processamento recusado/falhou), 429 (limite por IP), 503 (servidor ocupado ou remove.bg não configurado). Solicite `Accept: */*` ou JSON para receber o corpo de erro do endpoint de vídeo.

## Limites e configuração

As propriedades estão em `src/main/resources/application.properties`. O Spring aceita sobrescrevê-las por argumentos `--helperhub...=valor` ou variáveis de ambiente: por exemplo, `helperhub.video.yt-dlp-command` corresponde a `HELPERHUB_VIDEO_YT_DLP_COMMAND`.

| Propriedade | Padrão |
| --- | --- |
| `helperhub.processing.max-concurrent` | 4 requisições de processamento por instância |
| `helperhub.audio.max-requests-per-minute` | 60 por IP |
| `helperhub.image.max-requests-per-minute` | 30 por IP |
| `helperhub.video.max-requests-per-minute` | 20 por IP |
| `helperhub.audio.max-cut-duration-ms` | 1.800.000 (30 minutos) |
| `helperhub.audio.max-processing-seconds` | 120 por execução FFmpeg; análise FFprobe limitada a 30 s |
| `helperhub.image.max-input-pixels` | 25.000.000, verificados antes da decodificação |
| `helperhub.image.max-resize-output-side` | 8192 px |
| `helperhub.video.max-download-duration-seconds` | 900 s de execução |
| `helperhub.video.max-video-duration-seconds` | 1800 s de duração do vídeo |
| `helperhub.video.max-download-bytes` | 262.144.000 bytes (250 MiB), incluindo arquivos intermediários durante o download |

Upload: 50 MiB por arquivo e 55 MiB por requisição. A interface usa esses limites padrão. Se alterar limites do servidor, ajuste também as mensagens e validações da interface. Vídeos limitados a 1080p e aos codecs disponíveis no formato escolhido; não há transcodificação para forçar formatos ausentes. Áudio usa cópia de codec no corte: a precisão depende do formato.

O limite por IP usa o endereço da conexão e janelas fixas de um minuto. `X-Forwarded-For` enviado pelo cliente é ignorado. Atrás de um proxy, todas as conexões podem compartilhar o IP do proxy: configure identificação de clientes apenas no proxy/container confiável, bloqueie acesso direto à aplicação e valide os cabeçalhos antes de mudar `server.forward-headers-strategy`. Os contadores são locais à instância; múltiplas réplicas precisam de limitação no gateway ou armazenamento compartilhado.

Os downloads usam um diretório temporário exclusivo e removem os intermediários em sucesso, falha e timeout. O arquivo final é removido quando a resposta fecha. Um encerramento abrupto do sistema pode deixar resíduos: use armazenamento temporário efêmero. O limite de disco do downloader é verificado periodicamente, não é uma quota rígida do sistema operacional.

## Preparar deploy

```powershell
Copy-Item .env.example .env
# Preencha a chave remove.bg se desejar habilitar a operação.
docker compose up --build -d
docker compose logs -f
```

O Dockerfile inclui Java 25, FFmpeg/FFprobe, Node.js 22 e yt-dlp. O Compose inicia uma instância sem privilégios, com filesystem somente leitura, temporários em memória, limites de memória/CPU e porta publicada apenas em `127.0.0.1:8080`. O `.env` é lido pelo Compose; o `bootRun` usa variáveis do shell e não carrega esse arquivo automaticamente.

Para hospedagem pública, configure domínio/TLS e proxy no destino escolhido. A rota `/actuator/health` permite verificar se a aplicação está ativa; ela não garante acesso ao YouTube ou disponibilidade de créditos remove.bg. A preparação local não publica o projeto em nenhuma hospedagem.

## Próximos passos de operação

Validação local em 31/10/2026: 43 testes passaram; o JAR e a imagem Docker foram construídos. No contêiner com as restrições do Compose, `/actuator/health` retornou `UP`, WebP→PNG e WAV→MP3 retornaram arquivos válidos e um vídeo público de 19 segundos foi baixado como MP4. A interface foi verificada no navegador com imagem, áudio e vídeo e em largura de celular. As instâncias usadas para teste foram encerradas. A integração real com remove.bg e a execução do workflow no GitHub ainda dependem de configuração externa.

- Validar a imagem Docker no ambiente de destino e calibrar memória, concorrência e tamanho dos temporários com carga real.
- Configurar a chave remove.bg e validar uma operação real dessa integração.
- Definir hospedagem, domínio/TLS e política de acesso/limitação no gateway.
- Para maior volume, avaliar jobs assíncronos com status/cancelamento, observabilidade externa e limites compartilhados entre réplicas.

Referências: [yt-dlp](https://github.com/yt-dlp/yt-dlp), [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys), [Jaffree](https://github.com/kokorin/Jaffree).

## Fluxo entre ferramentas

O menu agrupa as ferramentas em acordeões de Imagem, Áudio e Vídeo. A categoria da ferramenta selecionada abre automaticamente. No celular, as categorias e seus totais de ferramentas ficam visíveis, com expansão individual. A página inicial oferece Explore com dois atalhos por categoria.

**Recortar imagem** oferece seleção por arraste, campos em pixels e proporções livre, 1:1, 16:9, 9:16 e 4:3. Ajustar os campos manualmente volta à proporção livre. **Girar e espelhar** mostra uma prévia dos giros de 90°, 180° e 270° e do espelhamento horizontal/vertical. As fotos com orientação EXIF são normalizadas antes do processamento para acompanhar a prévia do navegador.

Conversão e redimensionamento também aceitam saída WEBP, inclusive em lote. O campo `jpegQuality` ajusta a qualidade de JPEG e WebP. WebP preserva transparência e usa FFmpeg/libwebp, com timeout de 60 segundos por codificação ou decodificação.

A interface mantém o último arquivo de imagem e de mídia em memória durante a sessão da página. Ao trocar de ferramenta, o arquivo é reaproveitado se for compatível; não é enviado automaticamente. Cada resultado oferece **Continuar com este arquivo**, que o transforma na entrada da próxima operação sem baixar e selecionar novamente. Recarregar a página descarta esses arquivos da interface.

Resultados acima de 50 MB continuam disponíveis para download, mas não podem ser reutilizados em endpoints de upload. Imagens exibem comparação lado a lado, dimensões, tamanhos e redução (ou aumento) real. JPEG substitui transparência por branco.

Em **Comprimir imagem**, escolha 500 KB, 1 MB, 2 MB ou informe um teto em KB (1 KB = 1000 bytes). O resultado JPEG respeita o teto ou a API retorna um erro. A comparação mostra as dimensões finais antes do download. A opção de reduzir as dimensões vem desmarcada.

Em **Comprimir**, **Redimensionar** e **Converter**, selecione **Processar em lote (ZIP)** para aplicar as mesmas opções a até 5 imagens (50 MiB de entrada e 64 MiB de resultados no total). O processamento é sequencial. O ZIP numera os arquivos para preservar resultados de mesmo nome. Caso uma imagem falhe, a resposta identifica o arquivo com problema e o lote pode ser reenviado após a correção.

Validação do pacote de compressão/extração em 31/10/2026: **49 testes passaram**. No navegador, foram verificados os fluxos imagem → compressão → redimensionamento e vídeo → extração MP3 → corte, além da preservação do arquivo ao trocar de ferramenta e do layout em tela pequena. O contêiner foi reconstruído e atualizado em `http://localhost:8080`.

### Corte de áudio interativo

Ao selecionar um arquivo em **Cortar áudio**, a interface apresenta a faixa com dois seletores arrastáveis de início e fim. Os campos em segundos ficam sincronizados; as setas do teclado ajustam o seletor em 0,1 segundo (Shift + seta: 1 segundo). **Ouvir trecho** reproduz a seleção e para ao chegar ao fim. O botão **Cortar e baixar** processa o trecho e inicia o download automaticamente. **Baixar corte** permite salvar novamente o resultado, que também pode ser ouvido ou reutilizado em outra ferramenta.

A forma de onda é calculada localmente no navegador para arquivos de até 10 minutos, evitando grandes alocações de memória em gravações longas. Acima disso, a linha do tempo e os seletores continuam disponíveis. A reprodução e a forma de onda dependem dos formatos suportados pelo navegador; quando a prévia não está disponível, os campos de tempo permitem solicitar o corte ao servidor. O trecho permanece limitado a 30 minutos e o corte preserva o codec, com precisão dependente do formato.

Validação em 31/10/2026: suíte Gradle aprovada; seleção por arraste e teclado, sincronização dos campos, reprodução e download verificados no navegador. Um WAV de 8 segundos foi recortado de 1,916 a 5 segundos, baixado pela interface e validado com FFprobe (3,111 segundos, devido à cópia de codec). Contêiner atualizado e mantido em execução.

### Publicar na Vercel

Use o preset **Container** para rodar o Dockerfile na Vercel. O [guia de deploy](DEPLOY-VERCEL.md) mostra como configurar o projeto e testar um Preview. Para o primeiro teste, use arquivos pequenos. A Vercel limita o corpo de uploads a 4,5 MB.

### Marca e favicon

O símbolo usa um h creme sobre verde-floresta com detalhe em lima. O logo de 512 px está em `src/main/resources/static/logo.png`. O site usa o mesmo símbolo no cabeçalho, rodapé, `favicon.ico` (16, 32 e 48 px), `favicon-32.png` e `apple-touch-icon.png` (180 px).

A marca foi criada com o ImageGen integrado. Prompt final usado:

> Use case: logo-brand. Final app icon for Helper Hub, a useful web toolkit. Square opaque canvas completely filled edge-to-edge with perfectly flat forest green #254d3b. In the center, one bold, clean geometric lowercase h in warm white #f8f8f2, with thick rounded ends and one small rounded-square lime #c5d88b accent floating above its right shoulder. Minimal legible geometric design suitable for 16px favicon. Symbol occupies 72 percent of canvas. Crisp solid colors. Only these three solid colors. No words, no border, no texture, no gradients, no shadows, no mockup, no transparency.

### Páginas públicas por ferramenta

A branch principal deste repositório é master. As páginas por ferramenta são renderizadas no servidor com título, descrição, canonical, conteúdo e dados estruturados próprios. O sitemap em /sitemap.xml é gerado a partir do catálogo em ToolPageCatalog. A interface atualiza a URL e os metadados ao navegar, preservando o arquivo selecionado quando compatível.

O código no repositório e a versão em produção devem ser conferidos separadamente. Depois de publicar um commit, acompanhe o deploy da aplicação helperhub-app no Coolify até ficar saudável e confira a página inicial, uma rota direta como /comprimir-imagem e /sitemap.xml pelo domínio público.
