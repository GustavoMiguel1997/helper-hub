function createImageEditor(container, image, mode, report) {
  const lifecycle = new AbortController();
  const events = {signal:lifecycle.signal};
  const field = name => document.getElementById(name);
  const canvas = document.createElement('canvas');
  canvas.className = 'image-edit-canvas';
  canvas.setAttribute('aria-label', 'Prévia da imagem. Os campos abaixo permitem ajustar a edição pelo teclado.');
  const context = canvas.getContext('2d');
  const panel = document.createElement('div');
  panel.className = 'image-edit-panel';
  let loaded = false;
  let start = null;
  let ratio = 0;
  let ratioSelect = null;
  image.hidden = true;
  if(mode === 'crop') {
    const label = document.createElement('label');
    label.textContent = 'Proporção do recorte';
    ratioSelect = document.createElement('select');
    ratioSelect.id = 'crop-ratio';
    for(const [name, value] of [['Livre',0],['Quadrado · 1:1',1],['Paisagem · 16:9',16/9],['Retrato · 9:16',9/16],['Foto · 4:3',4/3]]) {
      const option = document.createElement('option');
      option.textContent = name;
      option.value = value;
      ratioSelect.append(option);
    }
    label.append(ratioSelect);
    panel.append(label);
    ratioSelect.addEventListener('change', () => {
      ratio = Number(ratioSelect.value);
      if(loaded) resetCrop();
    }, events);
  }
  panel.append(canvas);
  const hint = document.createElement('p');
  hint.className = 'field-hint';
  hint.textContent = mode === 'crop'
    ? 'Arraste para desenhar uma seleção. Use os campos para ajustar o recorte em pixels.'
    : 'A prévia acompanha o giro e o espelhamento selecionados.';
  panel.append(hint);
  container.append(panel);

  function selection() {
    return Object.fromEntries(['x','y','width','height'].map(name => [name, Number(field(name).value)]));
  }
  function setSelection(x, y, width, height) {
    for(const [name, value] of Object.entries({x,y,width,height})) field(name).value = Math.round(value);
    draw();
  }
  function resetCrop() {
    let width = image.naturalWidth;
    let height = image.naturalHeight;
    if(ratio) {
      width = Math.max(1, Math.floor(Math.min(width, height * ratio)));
      height = Math.max(1, Math.floor(width / ratio));
    }
    setSelection(Math.floor((image.naturalWidth-width)/2), Math.floor((image.naturalHeight-height)/2), width, height);
  }
  function draw() {
    if(!loaded) return;
    const rotation = mode === 'rotate' ? Number(field('rotation').value) : 0;
    const swap = rotation === 90 || rotation === 270;
    const width = swap ? image.naturalHeight : image.naturalWidth;
    const height = swap ? image.naturalWidth : image.naturalHeight;
    const scale = Math.min(1, 900/width, 600/height);
    canvas.width = Math.max(1, Math.round(width*scale));
    canvas.height = Math.max(1, Math.round(height*scale));
    context.save();
    context.translate(canvas.width/2, canvas.height/2);
    if(mode === 'rotate') {
      context.scale(field('flipHorizontal').checked ? -1 : 1, field('flipVertical').checked ? -1 : 1);
      context.rotate(rotation * Math.PI/180);
    }
    context.drawImage(image, -image.naturalWidth*scale/2, -image.naturalHeight*scale/2, image.naturalWidth*scale, image.naturalHeight*scale);
    context.restore();
    if(mode === 'crop') {
      const crop = selection();
      const x = crop.x*scale, y = crop.y*scale, w = crop.width*scale, h = crop.height*scale;
      context.fillStyle = '#142f2680';
      context.beginPath();
      context.rect(0,0,canvas.width,canvas.height);
      context.rect(x,y,w,h);
      context.fill('evenodd');
      context.strokeStyle = '#e2f7a4';
      context.lineWidth = 2;
      context.strokeRect(x,y,w,h);
      field('x').max = Math.max(0, image.naturalWidth - Math.max(1,crop.width));
      field('y').max = Math.max(0, image.naturalHeight - Math.max(1,crop.height));
      field('width').max = Math.max(1, image.naturalWidth - crop.x);
      field('height').max = Math.max(1, image.naturalHeight - crop.y);
      field('width').setCustomValidity(crop.x + crop.width <= image.naturalWidth ? '' : 'O recorte deve ficar dentro da imagem.');
      field('height').setCustomValidity(crop.y + crop.height <= image.naturalHeight ? '' : 'O recorte deve ficar dentro da imagem.');
    }
  }
  function position(event) {
    const bounds = canvas.getBoundingClientRect();
    return {
      x:Math.round(Math.max(0, Math.min(image.naturalWidth-1,(event.clientX-bounds.left)/bounds.width*image.naturalWidth))),
      y:Math.round(Math.max(0, Math.min(image.naturalHeight-1,(event.clientY-bounds.top)/bounds.height*image.naturalHeight)))
    };
  }
  function drag(event) {
    if(!start || !loaded) return;
    const point = position(event);
    const right = point.x >= start.x;
    const down = point.y >= start.y;
    let width = Math.max(1, Math.abs(point.x-start.x));
    let height = Math.max(1, Math.abs(point.y-start.y));
    if(ratio) {
      const maxWidth = right ? image.naturalWidth-start.x : start.x;
      const maxHeight = down ? image.naturalHeight-start.y : start.y;
      width = Math.max(1, Math.floor(Math.min(width, height*ratio, maxWidth, maxHeight*ratio)));
      height = Math.max(1, Math.floor(width/ratio));
    }
    setSelection(right ? start.x : Math.max(0,start.x-width), down ? start.y : Math.max(0,start.y-height), width, height);
  }
  canvas.addEventListener('pointerdown', event => {
    if(mode !== 'crop' || !loaded || document.getElementById('controls').disabled) return;
    start = position(event);
    canvas.setPointerCapture(event.pointerId);
    event.preventDefault();
  }, events);
  canvas.addEventListener('pointermove', drag, events);
  canvas.addEventListener('pointerup', event => { drag(event); start = null; }, events);
  canvas.addEventListener('pointercancel', () => { start = null; }, events);
  const names = mode === 'crop' ? ['x','y','width','height'] : ['rotation','flipHorizontal','flipVertical'];
  for(const name of names) field(name).addEventListener('input', () => {
    if(mode === 'crop') {
      ratio = 0;
      ratioSelect.value = '0';
    }
    draw();
  }, events);
  function onLoad() {
    loaded = true;
    if(mode === 'crop') resetCrop();
    else draw();
  }
  image.addEventListener('load', onLoad, events);
  image.addEventListener('error', () => report('Não foi possível abrir a prévia. Escolha outra imagem.', true), events);
  if(image.complete && image.naturalWidth) onLoad();
  return {ready:() => loaded, destroy:() => lifecycle.abort()};
}
