const tools = {
  crop: {category:'IMAGEM', title:'Enquadre o que importa.', description:'Arraste sobre a imagem para selecionar um recorte. Ajuste a posição pelos campos em pixels.', endpoint:'/api/image/edit', action:'Recortar imagem', type:'image', fields:[['x','Posição horizontal (px)','number','0'],['y','Posição vertical (px)','number','0'],['width','Largura do recorte (px)','number',''],['height','Altura do recorte (px)','number',''],['outputFormat','Formato de saída','select','PNG,JPEG,WEBP']]},
  rotate: {category:'IMAGEM', title:'Encontre o ângulo certo.', description:'Gire ou espelhe sua imagem e confira a prévia antes de baixar.', endpoint:'/api/image/edit', action:'Girar e espelhar', type:'image', fields:[['rotation','Giro horário','select','0,90,180,270'],['flipHorizontal','Espelhar horizontalmente','checkbox','true'],['flipVertical','Espelhar verticalmente','checkbox','true'],['outputFormat','Formato de saída','select','PNG,JPEG,WEBP']]},

  compress: {category:'IMAGEM', title:'Menos peso. Mais possibilidades.', description:'Escolha um tamanho máximo ou ajuste a qualidade do JPEG.', endpoint:'/api/image/compress', action:'Comprimir imagem', type:'image', fields:[['targetBytes','Tamanho máximo por imagem','select','manual,500000,1000000,2000000,custom'],['customTarget','Tamanho máximo (KB)','number',''],['allowResize','Permitir reduzir as dimensões para atingir o tamanho-alvo','checkbox','true'],['jpegQuality','Qualidade JPEG (1–100)','range','75']], note:'Saída em JPEG. Áreas transparentes recebem fundo branco.'},
  extract: {category:'VÍDEO', title:'Leve só o som com você.', description:'Envie um vídeo e extraia sua primeira faixa de áudio no formato que preferir.', endpoint:'/api/audio/extract', action:'Extrair áudio', type:'videoUpload', fields:[['targetFormat','Formato de saída','select','MP3,WAV,AAC,OGG,FLAC,OPUS']], note:'O vídeo precisa conter uma faixa de áudio. Upload de até 50 MB.'},
  resize: {category:'IMAGEM', title:'O tamanho certo, sem esforço.', description:'Ajuste as dimensões mantendo a proporção. Imagens menores não são ampliadas.', endpoint:'/api/image/resize', action:'Redimensionar imagem', type:'image', fields:[['maxWidth','Largura máxima (px)','number','1200'],['maxHeight','Altura máxima (px)','number',''],['outputFormat','Formato de saída','select','AUTO,PNG,JPEG,WEBP'],['jpegQuality','Qualidade JPEG / WebP (1–100)','number','85']]},
  imageConvert: {category:'IMAGEM', title:'Um novo formato. A mesma ideia.', description:'Converta sua imagem para PNG, JPEG ou WebP mantendo as dimensões originais.', endpoint:'/api/image/convert', action:'Converter imagem', type:'image', fields:[['outputFormat','Formato de saída','select','PNG,JPEG,WEBP'],['jpegQuality','Qualidade JPEG / WebP (1–100)','number','85']]},
  removeBg: {category:'IMAGEM', title:'Dê espaço ao que importa.', description:'Remova o fundo e receba um PNG com transparência.', endpoint:'/api/image/remove-background', action:'Remover fundo', type:'image', fields:[], note:'Esta operação envia a imagem ao serviço remove.bg e depende da configuração e dos créditos do servidor.'},
  cut: {category:'ÁUDIO', title:'Só o trecho que você precisa.', description:'Arraste os seletores na faixa, ouça o trecho e baixe seu corte. O corte preserva o codec original e pode ter pequena variação de precisão.', endpoint:'/api/audio/cut', action:'Cortar e baixar', type:'audio', fields:[['start','Início (segundos)','number','0'],['end','Fim (segundos)','number','10']]},
  audioConvert: {category:'ÁUDIO', title:'Seu som, em outro formato.', description:'Converta a faixa de áudio principal para o formato que combina com seu próximo passo.', endpoint:'/api/audio/convert', action:'Converter áudio', type:'audio', fields:[['targetFormat','Formato de saída','select','MP3,WAV,AAC,OGG,FLAC,OPUS']]},
  youtube: {category:'VÍDEO', title:'Seu vídeo, pronto para levar.', description:'Cole o link de um vídeo do YouTube e escolha o formato do arquivo.', endpoint:'/api/video/youtube-download', action:'Baixar vídeo', type:'video', fields:[['outputFormat','Formato de saída','select','MP4,WEBM']], note:'Limite de 250 MB de espaço temporário. Vídeos privados, transmissões ao vivo e formatos indisponíveis não são aceitos.'}
};
const $ = id => document.getElementById(id);
const imageExtensions = '.jpg,.jpeg,.png,.webp';
const videoExtensions = '.mp4,.webm,.mkv,.mov,.avi,.wmv,.mpeg,.mpg,.3gp';
const audioExtensions = '.mp3,.m4a,.aac,.wav,.flac,.ogg,.opus,.wma,.aiff,.aif,.caf,' + videoExtensions;
const uploadLimit = 50 * 1024 * 1024;
const batchOperations = {compress:'COMPRESS', resize:'RESIZE', imageConvert:'CONVERT'};
const targetLabels = {manual:'Ajustar qualidade', 500000:'500 KB', 1000000:'1 MB', 2000000:'2 MB', custom:'Personalizado'};
const sourceFiles = {image:null, media:null};
let batchFiles = [];
let audioEditor = null;
let imageEditor = null;
let current = 'resize', file = null, sourceUrl = null, resultUrl = null, comparisonUrl = null, resultFile = null, busy = false;
function extensions(tool) { return tool.type === 'image' ? imageExtensions : tool.type === 'videoUpload' ? videoExtensions : audioExtensions; }
function acceptsFile(tool, candidate) {
  return tool.type !== 'video' && candidate && candidate.size > 0 && candidate.size <= uploadLimit
    && extensions(tool).split(',').includes('.' + candidate.name.split('.').pop().toLowerCase());
}
function family(tool) { return tool.type === 'image' ? 'image' : 'media'; }
function formatSize(bytes) {
  if(bytes < 1024) return `${bytes} B`;
  if(bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
}
function clearResult() {
  $('result').hidden = true; $('result-preview').replaceChildren(); $('comparison-summary').hidden = true;
  if(resultUrl) URL.revokeObjectURL(resultUrl);
  if(comparisonUrl) URL.revokeObjectURL(comparisonUrl);
  resultUrl = null; comparisonUrl = null; resultFile = null;
}
function status(message = '', error = false) { $('status').textContent = message; $('status').classList.toggle('error', error); }
function selectTool(key) {
  if (busy) return;
  audioEditor?.destroy(); audioEditor = null;
  imageEditor?.destroy(); imageEditor = null;
  clearResult();
  current = key; file = null; $('file').value = ''; status();
  batchFiles = [];
  $('batch-mode').checked = false;
  $('batch-option').hidden = !batchOperations[key];
  $('file').multiple = false;
  if(sourceUrl) URL.revokeObjectURL(sourceUrl); sourceUrl = null;
  $('source-preview').replaceChildren(); $('source-preview').hidden = true;
  const tool = tools[key];
  document.querySelectorAll('.tool-category').forEach(group => { group.open = group.dataset.category === tool.category; });
  $('tool-menu-label').textContent = 'Ferramentas · ' + tool.action;
  if(window.matchMedia('(max-width:640px)').matches) $('tool-menu').open = false;
  document.querySelectorAll('[data-tool]').forEach(button => button.setAttribute('aria-pressed', String(button.dataset.tool === key)));
  $('tool-category').textContent = `FERRAMENTA DE ${tool.category}`;
  $('tool-title').textContent = tool.title; $('tool-description').textContent = tool.description;
  $('submit-label').textContent = tool.action; $('service-note').textContent = tool.note || '';
  $('upload-area').hidden = key === 'youtube'; $('url-area').hidden = key !== 'youtube';
  $('url').required = key === 'youtube'; $('url').disabled = key !== 'youtube';
  $('file').required = key !== 'youtube'; $('file').disabled = key === 'youtube';
  $('file').accept = extensions(tool);
  $('file-title').textContent = `Solte ${tool.type === 'image' ? 'sua imagem' : tool.type === 'videoUpload' ? 'seu vídeo' : 'seu áudio'} aqui`;
  $('file-hint').textContent = 'ou clique para escolher um arquivo';
  $('file-types').textContent = tool.type === 'image' ? 'JPG, PNG ou WebP · até 50 MB' : tool.type === 'videoUpload' ? 'MP4, WebM, MOV e outros vídeos · até 50 MB' : 'Áudio ou vídeo com faixa de áudio · até 50 MB';
  $('privacy-note').textContent = key === 'removeBg' ? 'Imagem enviada ao remove.bg para processamento.' : 'Arquivos processados no servidor e descartados após o envio.';
  document.querySelector('.editor-foot>span:first-child').textContent = tool.type === 'image' ? '↗ Proporções preservadas' : '↓ Download do resultado';
  $('options').replaceChildren();
  for(const [name, label, type, value] of tool.fields) {
    const wrapper = document.createElement('div'), caption = document.createElement('label');
    caption.htmlFor = name; caption.textContent = label;
    const control = document.createElement(type === 'select' ? 'select' : 'input'); control.id = name; control.name = name;
    if(type === 'select') for(const item of value.split(',')) { const option = document.createElement('option'); option.value = item; option.textContent = name === 'targetBytes' ? targetLabels[item] : item === 'AUTO' ? 'Automático' : item === 'AAC' ? 'AAC / M4A' : item; control.append(option); }
    else { control.type = type; control.value = value; control.min = name === 'start' ? '0' : '1'; control.step = ['start','end'].includes(name) ? '0.001' : '1'; if(['start','end'].includes(name)) {control.min='0'; control.required = true;} else control.max = name === 'jpegQuality' ? '100' : '8192'; }
    if(['x','y','width','height'].includes(name)) {
      control.min = ['x','y'].includes(name) ? '0' : '1';
      control.max = '25000000';
      control.required = true;
    }
    if(type === 'checkbox') {
      caption.classList.add('check-option');
      caption.prepend(control);
      wrapper.append(caption);
    } else wrapper.append(caption,control);
    if(type === 'range') {
      control.max = '100'; control.min = '1';
      const valueLabel = document.createElement('output'); valueLabel.htmlFor = name; valueLabel.textContent = control.value;
      control.addEventListener('input', () => { valueLabel.textContent = control.value; });
      wrapper.classList.add('quality-control'); wrapper.append(valueLabel);
    }
    $('options').append(wrapper);
  }
  if($('outputFormat')) $('outputFormat').addEventListener('change', updateQuality);
  if($('targetBytes')) {
    $('customTarget').min = '2';
    $('customTarget').max = '10000';
    $('targetBytes').addEventListener('change', updateQuality);
  }
  updateQuality();
  const saved = sourceFiles[family(tool)];
  if(key !== 'youtube' && acceptsFile(tool, saved)) {
    renderSource(saved);
    status('Arquivo mantido. Ajuste as opções e continue quando quiser.');
  }
}
function updateQuality() {
  const mode = $('targetBytes')?.value;
  const target = mode && mode !== 'manual';
  const quality = $('jpegQuality');
  if(quality) quality.disabled = Boolean(target) || $('outputFormat')?.value === 'PNG';
  if(mode) {
    quality.parentElement.hidden = Boolean(target);
    const custom = $('customTarget');
    custom.parentElement.hidden = mode !== 'custom';
    custom.disabled = mode !== 'custom';
    custom.required = mode === 'custom';
    $('allowResize').closest('.check-option').parentElement.hidden = !target;
    $('allowResize').disabled = !target;
  }
}
function updateBatchMode() {
  const batch = $('batch-mode').checked;
  $('file').value = '';
  $('file').multiple = batch;
  $('file').required = true;
  file = null;
  batchFiles = [];
  if(sourceUrl) URL.revokeObjectURL(sourceUrl);
  sourceUrl = null;
  $('source-preview').replaceChildren();
  $('source-preview').hidden = true;
  $('file-title').textContent = batch ? 'Solte suas imagens aqui' : 'Solte sua imagem aqui';
  $('file-hint').textContent = batch ? 'ou clique para escolher imagens' : 'ou clique para escolher um arquivo';
  $('file-types').textContent = batch ? 'JPG, PNG ou WebP · até 5 imagens e 50 MB no total' : 'JPG, PNG ou WebP · até 50 MB';
  $('submit-label').textContent = batch ? 'Processar e gerar ZIP' : tools[current].action;
  clearResult();
  status();
  if(!batch && acceptsFile(tools[current], sourceFiles.image)) renderSource(sourceFiles.image);
}
function setUpload(files) {
  if(busy || files.length === 0) return;
  if(!$('batch-mode').checked) {
    if(files.length === 1) setFile(files[0]);
    else status('Selecione um arquivo ou ative Processar em lote.', true);
    return;
  }
  const selected = Array.from(files);
  if(selected.length > 5 || selected.some(item => !acceptsFile(tools[current], item))
      || selected.reduce((size, item) => size + item.size, 0) > uploadLimit) {
    $('file').value = '';
    status('Selecione até 5 imagens JPG, PNG ou WebP não vazias, com até 50 MB no total.', true);
    return;
  }
  batchFiles = selected;
  $('file').required = false;
  $('file-title').textContent = `${selected.length} ${selected.length === 1 ? 'imagem selecionada' : 'imagens selecionadas'}`;
  $('file-hint').textContent = 'Clique para trocar o lote';
  const list = document.createElement('ul');
  for(const item of selected) {
    const row = document.createElement('li');
    row.textContent = `${item.name} · ${formatSize(item.size)}`;
    list.append(row);
  }
  $('source-preview').replaceChildren(list);
  $('source-preview').hidden = false;
  clearResult();
  status();
}
function renderSource(selected) {
  audioEditor?.destroy(); audioEditor = null;
  imageEditor?.destroy(); imageEditor = null;
  file = selected; $('file').required = false;
  $('file-title').textContent = selected.name;
  $('file-hint').textContent = `${formatSize(selected.size)} · clique para trocar`;
  if(sourceUrl) URL.revokeObjectURL(sourceUrl);
  sourceUrl = URL.createObjectURL(selected); $('source-preview').replaceChildren();
  const kind = tools[current].type === 'image' ? 'img' : tools[current].type === 'videoUpload' ? 'video' : 'audio';
  const preview = document.createElement(kind);
  preview.src = sourceUrl;
  if(kind === 'img') preview.alt = 'Prévia do arquivo selecionado'; else { preview.controls = true; preview.preload = 'metadata'; }
  $('source-preview').append(preview); $('source-preview').hidden = false;
  if(['crop','rotate'].includes(current)) {
    imageEditor = createImageEditor($('source-preview'), preview, current, status);
  }
  if(current === 'cut') audioEditor = createAudioEditor($('source-preview'), preview, selected, $('start'), $('end'));
}
function setFile(selected) {
  if(busy || !selected) return;
  if(!acceptsFile(tools[current], selected)) {
    $('file').value = ''; $('file').required = !file;
    status('Escolha um arquivo não vazio, de um formato aceito e com até 50 MB. O arquivo anterior foi mantido, se houver.', true);
    return;
  }
  clearResult(); status(); sourceFiles[family(tools[current])] = selected; renderSource(selected);
}
function imageFigure(url, title, size) {
  const figure = document.createElement('figure'), caption = document.createElement('figcaption'), image = document.createElement('img');
  caption.textContent = `${title} · ${formatSize(size)}`;
  image.alt = title; image.src = url;
  image.addEventListener('load', () => { caption.textContent = `${title} · ${formatSize(size)} · ${image.naturalWidth} × ${image.naturalHeight} px`; });
  figure.append(caption, image); return figure;
}
function renderResult(blob, filename, original, batch) {
  resultFile = new File([blob], filename, {type:blob.type});
  resultUrl = URL.createObjectURL(resultFile);
  $('download').textContent = batch ? 'Baixar ZIP ↓' : current === 'cut' ? 'Baixar corte ↓' : 'Baixar arquivo ↓';
  $('download').href = resultUrl; $('download').download = filename;
  $('result-meta').textContent = `${filename} · ${formatSize(blob.size)}`;
  $('result').hidden = false;
  $('continue-area').hidden = batch;
  const kind = blob.type.split('/')[0];
  const previewArea = $('result-preview'); previewArea.classList.remove('image-comparison');
  if(kind === 'image' && original) {
    comparisonUrl = URL.createObjectURL(original);
    previewArea.classList.add('image-comparison');
    previewArea.append(imageFigure(comparisonUrl, 'Antes', original.size), imageFigure(resultUrl, 'Depois', blob.size));
    const difference = original.size - blob.size;
    const percentage = (Math.abs(difference) / original.size * 100).toFixed(1);
    $('comparison-summary').textContent = difference > 0
      ? `${percentage}% menor · economia de ${formatSize(difference)}`
      : difference < 0 ? `${percentage}% maior que o original. Você pode ajustar a qualidade ou manter o arquivo original.` : 'Mesmo tamanho do arquivo original.';
    $('comparison-summary').hidden = false;
  } else if(['image','audio','video'].includes(kind)) {
    try {
      const preview = document.createElement(kind === 'image' ? 'img' : kind); preview.src = resultUrl;
      if(kind === 'image') preview.alt = 'Resultado do processamento'; else {preview.controls = true; preview.preload = 'metadata';}
      previewArea.append(preview);
    } catch { /* Download remains available if a media preview is unsupported. */ }
  }
  $('next-tool').replaceChildren();
  for(const [key, tool] of Object.entries(tools)) {
    if(!acceptsFile(tool, resultFile)) continue;
    const option = document.createElement('option'); option.value = key; option.textContent = tool.action;
    $('next-tool').append(option);
  }
  const available = $('next-tool').options.length > 0;
  $('use-result').disabled = !available; $('next-tool').disabled = !available;
  $('continue-note').textContent = available ? 'O resultado vira o próximo arquivo de entrada. Nada é enviado até você iniciar a operação.' : 'Este resultado ultrapassa o limite de 50 MB ou não é compatível com outra ferramenta. O download continua disponível.';
}
$('use-result').addEventListener('click', () => {
  if(busy || !resultFile) return;
  const next = $('next-tool').value, selected = resultFile;
  if(!tools[next] || !acceptsFile(tools[next], selected)) return;
  sourceFiles[family(tools[next])] = selected;
  clearResult(); selectTool(next);
  status('Resultado carregado como entrada. Confira as opções antes de processar.');
  $('tool-title').scrollIntoView({behavior:'smooth', block:'center'});
});
document.querySelectorAll('[data-tool]').forEach(button => button.addEventListener('click', () => selectTool(button.dataset.tool)));
$('batch-mode').addEventListener('change', updateBatchMode);
$('file').addEventListener('change', event => setUpload(event.target.files));
for(const name of ['dragenter','dragover']) $('dropzone').addEventListener(name, event => { event.preventDefault(); if(!busy) $('dropzone').classList.add('dragging'); });
for(const name of ['dragleave','drop']) $('dropzone').addEventListener(name, event => { event.preventDefault(); $('dropzone').classList.remove('dragging'); });
$('dropzone').addEventListener('drop', event => setUpload(event.dataTransfer.files));
$('tool-form').addEventListener('submit', async event => {
  event.preventDefault(); if(busy) return;
  const tool = tools[current], data = new FormData();
  const batch = $('batch-mode').checked;
  if(current === 'youtube') data.set('url', $('url').value.trim());
  else if(batch) {
    if(!batchFiles.length) { status('Escolha as imagens do lote.', true); return; }
    data.set('operation', batchOperations[current]);
    for(const item of batchFiles) data.append('files', item);
  }
  else { if(!acceptsFile(tool, file)) { status('Escolha um arquivo para continuar.', true); return; } data.set('file',file); }
  if(current === 'resize' && !$('maxWidth').value && !$('maxHeight').value) {status('Informe a largura ou a altura máxima.', true); return;}
  for(const [name] of tool.fields) { const control = $(name); if(!control.disabled && control.value !== '' && !['start','end','targetBytes','customTarget','allowResize'].includes(name)) data.set(name, control.type === 'checkbox' ? String(control.checked) : control.value); }
  if(current === 'compress' && $('targetBytes').value !== 'manual') {
    const target = $('targetBytes').value;
    data.set('targetBytes', target === 'custom' ? Number($('customTarget').value) * 1000 : target);
    data.set('allowResize', $('allowResize').checked);
  }
  if(['crop','rotate'].includes(current) && !imageEditor?.ready()) {
    status('Aguarde a prévia da imagem ficar pronta.', true); return;
  }
  if(current === 'cut') {
    const start = Math.round(Number($('start').value)*1000), end = Math.round(Number($('end').value)*1000);
    if(end <= start || end-start > 1800000) {status('O fim deve ser maior que o início e o trecho deve ter até 30 minutos.', true); return;}
    data.set('startMs',start); data.set('endMs',end);
  }
  const original = current === 'youtube' ? null : file;
  audioEditor?.pause();
  clearResult(); busy = true; $('controls').disabled = true;
  document.querySelectorAll('[data-tool]').forEach(button => button.disabled = true);
  $('submit-label').textContent = 'Processando…'; status('Estamos preparando seu arquivo. Mantenha esta página aberta.');
  try {
    const response = await fetch(batch ? '/api/image/batch' : tool.endpoint, {method:'POST', body:data, headers:{Accept:'*/*'}});
    if(!response.ok) {
      let body; try { body = await response.json(); } catch { body = {}; }
      const messages = {REMBG_NOT_CONFIGURED:'A remoção de fundo ainda não está disponível neste servidor.', YTDLP_NOT_AVAILABLE:'O download de vídeos ainda não está disponível neste servidor.'};
      if(messages[body.error]) throw new Error(messages[body.error]);
      throw new Error(body.details?.length ? body.details.join(' · ') : body.message || `Não foi possível processar o arquivo (HTTP ${response.status}).`);
    }
    const blob = await response.blob(); if(!blob.size) throw new Error('O servidor retornou um arquivo vazio.');
    const disposition = response.headers.get('Content-Disposition') || '';
    const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i), plain = disposition.match(/filename="([^"]+)"/i);
    let filename = batch ? 'helperhub-imagens.zip' : 'helperhub-resultado';
    try { filename = encoded ? decodeURIComponent(encoded[1]) : plain ? plain[1] : filename; } catch { /* Keep fallback filename. */ }
    renderResult(blob, filename, original, batch);
    if(current === 'cut') {
      $('download').click();
      status('Corte pronto! Download solicitado. Se não começar, clique em Baixar corte.');
    } else status('Pronto! Seu arquivo está disponível para download.');
  } catch(error) { status(error instanceof TypeError ? 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.' : error?.message || 'Não foi possível concluir a operação. Tente novamente.', true); }
  finally { busy = false; $('controls').disabled = false; document.querySelectorAll('[data-tool]').forEach(button => button.disabled = false); $('submit-label').textContent = batch ? 'Processar e gerar ZIP' : tool.action; }
});
selectTool('resize');

const mobileMenu = window.matchMedia('(max-width:640px)');
$('tool-menu').open = !mobileMenu.matches;
mobileMenu.addEventListener('change', event => { $('tool-menu').open = !event.matches; });
