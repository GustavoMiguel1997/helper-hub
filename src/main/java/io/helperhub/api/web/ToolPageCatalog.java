package io.helperhub.api.web;

import io.helperhub.api.web.ToolPageDefinition.FaqItem;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catálogo central de páginas públicas do HelperHub para roteamento SSR, links internos e sitemap.
 */
public final class ToolPageCatalog {

  public static final String BASE_URL = "https://helperhub.gmslab.cloud";

  private static final ToolPageDefinition HOME = new ToolPageDefinition(
      "/",
      "explore",
      "GERAL",
      "Início",
      "HelperHub — Ferramentas online gratuitas de imagem, áudio e vídeo",
      "Comprima, redimensione, recorte e converta imagens JPG, PNG e WebP, corte e converta áudios MP3/WAV, extraia áudio de vídeos e baixe vídeos online sem cadastro.",
      "MENOS ETAPAS. MAIS POSSIBILIDADES.",
      "Ferramentas online de imagem, áudio e vídeo",
      "Comprima, redimensione e converta imagens, corte áudios, extraia som de vídeos e baixe do YouTube sem cadastro.",
      "Todas as ferramentas em um só lugar",
      List.of(
          "O HelperHub reúne utilitários rápidos para tratar imagens, áudios e vídeos direto no navegador, sem precisar criar conta ou instalar programas.",
          "Os arquivos enviados passam pelo servidor apenas durante o processamento e são descartados logo após a entrega do resultado, com limite de até 50 MB por envio."
      ),
      List.of(
          new FaqItem(
              "Preciso criar conta ou pagar para usar o HelperHub?",
              "Não. Todas as ferramentas disponíveis funcionam gratuitamente e sem cadastro."
          ),
          new FaqItem(
              "Quais formatos são aceitos nas ferramentas?",
              "Para imagens, aceitamos JPG, PNG e WebP. Para áudio e vídeo, aceitamos MP3, WAV, AAC/M4A, OGG, FLAC, OPUS, MP4, WebM, MOV e outros formatos populares."
          ),
          new FaqItem(
              "Os arquivos ficam armazenados no servidor?",
              "Não. Os arquivos são processados em disco temporário e removidos após o envio do resultado."
          )
      ),
      true
  );

  private static final List<ToolPageDefinition> TOOLS = List.of(
      new ToolPageDefinition(
          "/comprimir-imagem",
          "compress",
          "IMAGEM",
          "Comprimir imagem",
          "Comprimir imagem online grátis (JPG, PNG e WebP) — HelperHub",
          "Reduza o peso de imagens JPG, PNG e WebP ajustando a qualidade ou definindo tamanho máximo (500 KB, 1 MB, 2 MB) e processe até 5 imagens em lote ZIP.",
          "FERRAMENTA DE IMAGEM · COMPRESSÃO",
          "Comprimir imagem online sem complicação",
          "Diminua o tamanho de arquivos JPG, PNG e WebP ajustando a qualidade JPEG ou escolhendo um limite em KB/MB, inclusive em lote ZIP.",
          "Como funciona a compressão de imagens",
          List.of(
              "Escolha entre ajuste manual de qualidade JPEG (1 a 100) ou um tamanho-alvo automático como 500 KB, 1 MB, 2 MB ou um valor personalizado em KB.",
              "Você pode enviar uma imagem de até 50 MB ou ativar o processamento em lote para comprimir até 5 imagens de uma vez e baixar tudo em um arquivo ZIP."
          ),
          List.of(
              new FaqItem(
                  "Qual formato é gerado ao comprimir uma imagem?",
                  "A saída da compressão é gerada em JPEG para alcançar maior redução de tamanho. Áreas transparentes de PNG ou WebP recebem fundo branco."
              ),
              new FaqItem(
                  "Consigo limitar a imagem a um peso exato, como 500 KB ou 1 MB?",
                  "Sim. Selecione 500 KB, 1 MB, 2 MB ou Personalizado no campo Tamanho máximo por imagem."
              ),
              new FaqItem(
                  "Posso comprimir várias imagens de uma vez?",
                  "Sim. Marque a opção Processar em lote (ZIP) para enviar até 5 imagens JPG, PNG ou WebP somando até 50 MB."
              )
          ),
          false
      ),
      new ToolPageDefinition(
          "/redimensionar-imagem",
          "resize",
          "IMAGEM",
          "Redimensionar",
          "Redimensionar imagem online grátis (JPG, PNG e WebP) — HelperHub",
          "Altere largura e altura de imagens JPG, PNG e WebP mantendo a proporção original. Suporta saída em PNG, JPEG ou WebP e processamento em lote ZIP.",
          "FERRAMENTA DE IMAGEM · REDIMENSIONAR",
          "Redimensionar imagem mantendo a proporção",
          "Defina a largura ou altura máxima em pixels para ajustar fotos e gráficos sem distorcer e sem ampliar imagens menores.",
          "Redimensionamento proporcional e em lote",
          List.of(
              "Informe a largura máxima, a altura máxima ou ambas em pixels (até 8192 px por lado). O HelperHub mantém a proporção original e preserva os detalhes.",
              "Também é possível escolher o formato de saída (Automático, PNG, JPEG ou WebP) e processar até 5 imagens simultaneamente em um pacote ZIP."
          ),
          List.of(
              new FaqItem(
                  "A imagem fica esticada ao redimensionar?",
                  "Não. A ferramenta calcula automaticamente a nova dimensão preservando a proporção original."
              ),
              new FaqItem(
                  "Imagens menores que a largura informada são ampliadas?",
                  "Não. Imagens que já são menores que os limites definidos mantêm seu tamanho original para evitar perda de nitidez."
              )
          ),
          false
      ),
      new ToolPageDefinition(
          "/recortar-imagem",
          "crop",
          "IMAGEM",
          "Recortar imagem",
          "Recortar imagem online grátis (JPG, PNG e WebP) — HelperHub",
          "Recorte fotos e capturas de tela visualmente ou informando posição e tamanho exatos em pixels. Exporte em PNG, JPEG ou WebP sem cadastro.",
          "FERRAMENTA DE IMAGEM · RECORTE",
          "Recortar imagem com precisão em pixels",
          "Arraste diretamente sobre a prévia da imagem ou ajuste X, Y, largura e altura em pixels e baixe em PNG, JPEG ou WebP.",
          "Recorte visual e ajuste fino por coordenadas",
          List.of(
              "Ao enviar uma imagem JPG, PNG ou WebP, você seleciona a área desejada diretamente sobre o canvas ou preenche os campos de posição horizontal, vertical, largura e altura em pixels.",
              "O resultado pode ser exportado mantendo transparência em PNG ou WebP, ou em JPEG leve."
          ),
          List.of(
              new FaqItem(
                  "Quais formatos posso usar para recortar e exportar?",
                  "Você pode enviar JPG, PNG ou WebP de até 50 MB e escolher saída em PNG, JPEG ou WebP."
              ),
              new FaqItem(
                  "Posso usar a imagem recortada em outra ferramenta em seguida?",
                  "Sim. Após gerar o recorte, use o bloco Continuar com este arquivo para comprimir, girar ou converter o resultado sem reenviar."
              )
          ),
          false
      ),
      new ToolPageDefinition(
          "/girar-espelhar-imagem",
          "rotate",
          "IMAGEM",
          "Girar e espelhar",
          "Girar e espelhar imagem online grátis — HelperHub",
          "Rotacione imagens em 90°, 180° ou 270° e espelhe horizontal ou verticalmente com prévia instantânea antes do download em PNG, JPEG ou WebP.",
          "FERRAMENTA DE IMAGEM · ORIENTAÇÃO",
          "Girar e espelhar imagem online",
          "Corrija fotos de lado ou de ponta-cabeça girando em 90°, 180° ou 270° e aplique espelhamento horizontal ou vertical com prévia ao vivo.",
          "Correção rápida de orientação e espelhamento",
          List.of(
              "Selecione o ângulo de giro horário (0°, 90°, 180° ou 270°) e combine com espelhamento horizontal ou vertical vendo o resultado na prévia antes de baixar.",
              "Compatível com arquivos JPG, PNG e WebP de até 50 MB e exportação em PNG, JPEG ou WebP."
          ),
          List.of(
              new FaqItem(
                  "Consigo girar e espelhar ao mesmo tempo?",
                  "Sim. Você pode combinar a rotação em graus com espelhamento horizontal e vertical na mesma operação."
              ),
              new FaqItem(
                  "A qualidade original é mantida ao girar em PNG?",
                  "Sim. Ao escolher PNG como formato de saída, a imagem é salva sem compressão com perdas e preservando transparência."
              )
          ),
          false
      ),
      new ToolPageDefinition(
          "/converter-imagem",
          "imageConvert",
          "IMAGEM",
          "Converter imagem",
          "Converter imagem online grátis (PNG, JPG e WebP) — HelperHub",
          "Converta imagens entre PNG, JPEG e WebP online mantendo as dimensões originais. Ajuste a qualidade e converta até 5 arquivos em lote ZIP.",
          "FERRAMENTA DE IMAGEM · CONVERSÃO",
          "Converter imagem entre PNG, JPEG e WebP",
          "Transforme imagens JPG em PNG, PNG em WebP ou WebP em JPEG mantendo a resolução original e com suporte a lote ZIP.",
          "Conversão fiel entre os principais formatos da web",
          List.of(
              "Ideal para converter fotos pesadas para WebP antes de publicar em sites, transformar WebP em JPG/PNG para compatibilidade com editores ou gerar PNG sem perdas.",
              "Quando o formato escolhido for JPEG ou WebP, você pode ajustar a qualidade de 1 a 100. Também funciona em lote ZIP com até 5 imagens."
          ),
          List.of(
              new FaqItem(
                  "Converter PNG para WebP mantém a transparência?",
                  "Sim. Tanto PNG quanto WebP suportam canal alfa (transparência). Já a conversão para JPEG preenche áreas transparentes."
              ),
              new FaqItem(
                  "As dimensões da imagem mudam na conversão?",
                  "Não. A ferramenta de conversão preserva exatamente a largura e a altura originais do arquivo."
              )
          ),
          false
      ),
      new ToolPageDefinition(
          "/remover-fundo-imagem",
          "removeBg",
          "IMAGEM",
          "Remover fundo",
          "Remover fundo de imagem online (PNG transparente) — HelperHub",
          "Remova o fundo de fotos e imagens JPG, PNG ou WebP e baixe o resultado em PNG com fundo transparente.",
          "FERRAMENTA DE IMAGEM · REMOÇÃO DE FUNDO",
          "Remover fundo de imagem automaticamente",
          "Envie sua foto e gere um arquivo PNG com fundo transparente pronto para uso.",
          "Recorte automático de fundo para PNG transparente",
          List.of(
              "Esta operação utiliza integração externa com a API remove.bg quando habilitada nas configurações do servidor."
          ),
          List.of(
              new FaqItem(
                  "Em qual formato a imagem sem fundo é baixada?",
                  "O arquivo final é entregue em PNG para preservar a transparência do recorte."
              )
          ),
          false
      ),
      new ToolPageDefinition(
          "/cortar-audio",
          "cut",
          "ÁUDIO",
          "Cortar áudio",
          "Cortar áudio online grátis (MP3, WAV, AAC e OGG) — HelperHub",
          "Corte trechos de músicas, podcasts ou vídeos pela forma de onda visual, ouça antes de baixar e salve seu corte online sem cadastro.",
          "FERRAMENTA DE ÁUDIO · CORTE",
          "Cortar áudio online com prévia visual",
          "Envie um arquivo de áudio ou vídeo, arraste os marcadores na forma de onda, ouça o trecho selecionado e baixe o corte.",
          "Corte rápido preservando o formato de áudio",
          List.of(
              "Depois de enviar arquivos MP3, WAV, M4A/AAC, OGG, FLAC, OPUS ou até vídeos com faixa de áudio, o editor desenha a onda sonora para você marcar início e fim em segundos.",
              "O corte preserva o codec original sempre que possível para entregar o arquivo rapidamente, com limite de até 30 minutos por trecho e upload de até 50 MB."
          ),
          List.of(
              new FaqItem(
                  "Posso cortar o áudio de um arquivo de vídeo?",
                  "Sim. Você pode enviar um vídeo MP4, WebM ou MOV diretamente e selecionar apenas o trecho de áudio desejado."
              ),
              new FaqItem(
                  "Qual a duração máxima do corte?",
                  "O trecho selecionado pode ter até 30 minutos (1800 segundos), respeitando o limite de 50 MB de upload."
              )
          ),
          false
      ),
      new ToolPageDefinition(
          "/converter-audio",
          "audioConvert",
          "ÁUDIO",
          "Converter áudio",
          "Converter áudio online grátis (MP3, WAV, AAC, OGG, FLAC, OPUS) — HelperHub",
          "Converta áudios ou faixas de vídeo para MP3, WAV, AAC/M4A, OGG, FLAC ou OPUS online com FFmpeg, grátis e sem cadastro.",
          "FERRAMENTA DE ÁUDIO · CONVERSÃO",
          "Converter áudio para MP3, WAV, AAC, OGG, FLAC ou OPUS",
          "Envie qualquer áudio compatível (ou vídeo com trilha de som) e converta para o formato que você precisa com poucos cliques.",
          "Conversão de áudio compatível com os formatos mais usados",
          List.of(
              "Aceita entradas em MP3, M4A, AAC, WAV, FLAC, OGG, OPUS, WMA, AIFF, CAF e também arquivos de vídeo contendo faixa de áudio.",
              "Escolha o formato de saída entre MP3, WAV, AAC/M4A, OGG, FLAC ou OPUS e baixe o novo arquivo assim que a conversão terminar."
          ),
          List.of(
              new FaqItem(
                  "Qual formato escolher entre MP3, WAV e OPUS?",
                  "MP3 e AAC/M4A oferecem alta compatibilidade em celulares e players; WAV e FLAC preservam áudio sem perdas; OPUS e OGG entregam arquivos menores na web."
              ),
              new FaqItem(
                  "Posso usar um arquivo convertido depois para cortar um trecho?",
                  "Sim. Ao final do processamento, use Continuar com este arquivo para enviar o resultado direto para o cortador de áudio."
              )
          ),
          false
      ),
      new ToolPageDefinition(
          "/extrair-audio-video",
          "extract",
          "VÍDEO",
          "Extrair áudio",
          "Extrair áudio de vídeo online grátis (MP4 para MP3, WAV, AAC) — HelperHub",
          "Extraia a faixa de áudio de vídeos MP4, WebM, MOV, MKV ou AVI e salve em MP3, WAV, AAC/M4A, OGG, FLAC ou OPUS grátis e sem cadastro.",
          "FERRAMENTA DE VÍDEO · EXTRAÇÃO DE ÁUDIO",
          "Extrair áudio de vídeo online",
          "Envie um vídeo MP4, WebM, MOV ou MKV de até 50 MB e baixe apenas a faixa de áudio em MP3, WAV, AAC, OGG, FLAC ou OPUS.",
          "Transforme vídeos em arquivos de áudio em segundos",
          List.of(
              "Envie gravações em MP4, WebM, MKV, MOV, AVI, WMV, MPEG ou 3GP. O HelperHub lê a primeira faixa de áudio do vídeo usando FFmpeg e converte diretamente para o formato escolhido.",
              "Útil para separar falas, aulas, entrevistas ou trilhas sonoras sem precisar abrir editores pesados."
          ),
          List.of(
              new FaqItem(
                  "Quais formatos de vídeo são aceitos para extrair áudio?",
                  "Aceitamos MP4, WebM, MKV, MOV, AVI, WMV, MPEG, MPG e 3GP com até 50 MB que possuam ao menos uma faixa de áudio."
              ),
              new FaqItem(
                  "Posso extrair em MP3 e depois cortar um trecho?",
                  "Sim. Após extrair o áudio, clique em Continuar com este arquivo e selecione Cortar áudio."
              )
          ),
          false
      ),
      new ToolPageDefinition(
          "/baixar-video-youtube",
          "youtube",
          "VÍDEO",
          "Baixar do YouTube",
          "Baixar vídeo do YouTube online (MP4 e WebM) — HelperHub",
          "Cole o link de um vídeo do YouTube e baixe em MP4 ou WebM (até 1080p e 30 minutos) direto pelo navegador, sem instalar programas.",
          "FERRAMENTA DE VÍDEO · YOUTUBE",
          "Baixar vídeo do YouTube em MP4 ou WebM",
          "Cole a URL do vídeo do YouTube, escolha entre MP4 ou WebM e faça o download sem instalar extensões.",
          "Download de vídeos em MP4 ou WebM",
          List.of(
              "Basta colar o link público do vídeo do YouTube e escolher o formato de saída MP4 ou WebM. O servidor prepara o arquivo até 1080p respeitando o limite de 30 minutos e 250 MB temporários.",
              "Se você precisar apenas do som após baixar o vídeo (até 50 MB), pode encaminhar o resultado direto para a ferramenta de extrair ou cortar áudio."
          ),
          List.of(
              new FaqItem(
                  "Quais vídeos do YouTube podem ser baixados?",
                  "Vídeos públicos com até 30 minutos de duração e arquivo final de até 250 MB. Vídeos privados, transmissões ao vivo e formatos indisponíveis não são aceitos."
              ),
              new FaqItem(
                  "Qual formato escolher entre MP4 e WebM?",
                  "MP4 tem maior compatibilidade com celulares, TVs e editores; WebM costuma gerar arquivos mais compactos para uso em navegadores."
              )
          ),
          false
      )
  );

  private static final List<ToolPageDefinition> AUDIO_TOOLS = List.of(
      new ToolPageDefinition(
          "/juntar-audios", "audioJoin", "ÁUDIO", "Juntar áudios",
          "Juntar áudios online com volume e fades — HelperHub",
          "Junte de dois a cinco áudios, organize a ordem e ouça a sequência com ajustes de volume e fades antes de exportar.",
          "FERRAMENTA DE ÁUDIO · JUNÇÃO", "Juntar áudios online",
          "Adicione os arquivos, organize a sequência e prepare uma faixa única em MP3, WAV, AAC, OGG, FLAC ou OPUS.",
          "Como juntar arquivos de áudio",
          List.of(
              "Adicione de dois a cinco arquivos, com até 50 MB no total. Arraste os itens para definir a ordem ou use os botões de mover para cima e para baixo.",
              "Ouça a sequência com os ajustes de volume e fades antes de gerar o arquivo. Os fades são aplicados ao início e ao fim da sequência completa. A prévia depende dos formatos aceitos pelo navegador.",
              "A saída é preparada em estéreo a 48 kHz, com duração total de até 30 minutos. Depois do processamento, o player permite ouvir o resultado antes de baixar."
          ),
          List.of(
              new FaqItem("Posso juntar arquivos de formatos diferentes?",
                  "Sim. Os arquivos são normalizados para uma faixa estéreo e exportados no formato escolhido."),
              new FaqItem("Os fades são aplicados entre todos os arquivos?",
                  "O fade de entrada fica no início da sequência e o de saída no fim. Os arquivos são unidos na ordem escolhida."),
              new FaqItem("Como altero a ordem pelo celular?",
                  "Use as setas ao lado de cada arquivo. Você também pode remover um item e adicionar outro.")
          ), false),
      new ToolPageDefinition(
          "/ajustar-volume-audio", "audioEffects", "ÁUDIO", "Volume e fades",
          "Ajustar volume de áudio e aplicar fades online — HelperHub",
          "Aumente ou diminua o volume de um áudio e aplique fades de entrada e saída, com prévia e exportação em vários formatos.",
          "FERRAMENTA DE ÁUDIO · VOLUME E FADES", "Ajustar volume e fades de áudio",
          "Escolha um volume de 0 a 200% e suavize o início e o fim com fades de até 30 segundos.",
          "Como aplicar volume e fades",
          List.of(
              "Envie um arquivo de áudio ou vídeo com áudio de até 50 MB e 30 minutos. O volume começa em 100%, com os fades desativados.",
              "Use Ouvir com ajustes para conferir a alteração no navegador. Ajuste a duração do fade de entrada e de saída em segundos. Cada fade deve caber na duração do áudio.",
              "Gere o arquivo no formato escolhido e ouça o resultado antes de baixar. A saída usa estéreo a 48 kHz. Volumes altos podem causar distorção."
          ),
          List.of(
              new FaqItem("Qual é o volume original?",
                  "100% mantém a amplitude original. 50% reduz a amplitude pela metade e 200% a duplica."),
              new FaqItem("O que é fade de entrada?",
                  "É um aumento gradual do volume, começando em silêncio e chegando ao volume escolhido."),
              new FaqItem("Posso aplicar apenas o fade de saída?",
                  "Sim. Deixe o fade de entrada em zero e escolha a duração do fade de saída.")
          ), false)
  );

  private static final List<ToolPageDefinition> PRESETS = List.of(
      new ToolPageDefinition(
          "/comprimir-imagem-500kb", "compress", "IMAGEM", "Comprimir imagem para 500 KB",
          "Comprimir imagem para até 500 KB online — HelperHub",
          "Reduza JPG, PNG ou WebP para um JPEG de até 500 KB. O limite já vem selecionado, com prévia e opção de reduzir as dimensões.",
          "IMAGEM · ATÉ 500 KB", "Comprimir imagem para até 500 KB",
          "Envie sua imagem e gere um JPEG de até 500.000 bytes. Confira o resultado antes de baixar.",
          "Como reduzir uma imagem para 500 KB",
          List.of(
              "O limite de 500 KB já está selecionado. Envie JPG, PNG ou WebP de até 50 MB e inicie a compressão. O resultado é um JPEG, com áreas transparentes preenchidas de branco.",
              "Primeiro ajustamos a qualidade mantendo as dimensões. Quando isso for insuficiente, você pode permitir reduzir as dimensões e tentar novamente. A comparação mostra o peso e as dimensões finais.",
              "Neste preset, 500 KB correspondem a 500.000 bytes. É um teto para o arquivo final. Você também pode ativar o lote para aplicar o mesmo limite a cada uma de até cinco imagens."
          ),
          List.of(
              new FaqItem("A imagem fica exatamente com 500 KB?",
                  "O JPEG final fica com até 500.000 bytes. Ele pode ficar menor, dependendo da imagem."),
              new FaqItem("O que faço se a imagem não chegar ao limite?",
                  "Ative Permitir reduzir as dimensões para atingir o tamanho-alvo e processe novamente. A redução das dimensões começa após sua autorização."),
              new FaqItem("A transparência do PNG é mantida?",
                  "A saída deste preset é JPEG. As áreas transparentes recebem fundo branco.")
          ), false),
      new ToolPageDefinition(
          "/converter-png-para-jpg", "imageConvert", "IMAGEM", "Converter PNG para JPG",
          "Converter PNG para JPG online grátis — HelperHub",
          "Transforme PNG em JPG com saída JPEG já selecionada. Ajuste a qualidade, compare os arquivos e converta até cinco imagens em lote.",
          "IMAGEM · PNG PARA JPG", "Converter PNG para JPG",
          "Envie seu PNG, ajuste a qualidade do JPEG e confira a comparação antes de baixar.",
          "Como transformar PNG em JPG",
          List.of(
              "A saída JPEG já vem selecionada com qualidade 85. Envie o PNG e clique em Converter imagem. JPG e JPEG identificam o mesmo formato de imagem.",
              "A conversão mantém as dimensões e preenche áreas transparentes com branco. Fotos podem ocupar menos espaço em JPEG, enquanto textos e gráficos podem apresentar perda de nitidez. A comparação mostra os tamanhos reais.",
              "Para chegar a um limite de upload, use Comprimir imagem para 500 KB. Se precisar preservar a transparência, escolha PNG ou WebP na ferramenta de conversão. O lote permite converter até cinco arquivos com as mesmas opções."
          ),
          List.of(
              new FaqItem("JPG e JPEG são formatos diferentes?", "São duas extensões usadas para o mesmo formato. O arquivo gerado aqui usa a extensão .jpg."),
              new FaqItem("O fundo transparente fica de que cor?", "As áreas transparentes recebem fundo branco na saída JPEG."),
              new FaqItem("Posso ajustar a qualidade antes de converter?", "Sim. O preset começa em qualidade 85, e você pode escolher um valor de 1 a 100.")
          ), false)
  );

  private static final Map<String, ToolPageDefinition> BY_PATH = buildByPath();
  private static final Map<String, ToolPageDefinition> BY_TOOL_KEY = tools().stream()
      .collect(Collectors.toUnmodifiableMap(ToolPageDefinition::toolKey, Function.identity()));

  private ToolPageCatalog() {}

  private static Map<String, ToolPageDefinition> buildByPath() {
    Map<String, ToolPageDefinition> map = pages().stream()
        .collect(Collectors.toMap(ToolPageDefinition::path, Function.identity()));
    map.put(HOME.path(), HOME);
    return Map.copyOf(map);
  }

  public static ToolPageDefinition home() {
    return HOME;
  }

  public static List<ToolPageDefinition> tools() {
    return java.util.stream.Stream.concat(TOOLS.stream(), AUDIO_TOOLS.stream()).toList();
  }

  public static List<ToolPageDefinition> pages() {
    return java.util.stream.Stream.concat(tools().stream(), PRESETS.stream()).toList();
  }

  public static Map<String, String> presetValues(String path) {
    return switch (path) {
      case "/comprimir-imagem-500kb" -> Map.of("targetBytes", "500000");
      case "/converter-png-para-jpg" -> Map.of("outputFormat", "JPEG", "jpegQuality", "85");
      default -> Map.of();
    };
  }

  public static Optional<ToolPageDefinition> findByPath(String path) {
    return Optional.ofNullable(BY_PATH.get(path));
  }

  public static Optional<ToolPageDefinition> findByToolKey(String toolKey) {
    return Optional.ofNullable(BY_TOOL_KEY.get(toolKey));
  }
}
