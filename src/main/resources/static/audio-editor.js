function createAudioEditor(container, media, selected, startInput, endInput) {
  const root = document.createElement('div');
  root.className = 'audio-editor';
  root.innerHTML = `<div class="audio-editor-heading"><strong>Selecione seu trecho</strong><output class="selection-time"></output></div>
    <div class="audio-track" aria-label="Faixa de áudio"><canvas aria-hidden="true"></canvas><div class="audio-selection"></div><div class="audio-playhead"></div>
      <button type="button" class="audio-handle start-handle" role="slider" aria-label="Início do trecho" aria-orientation="horizontal">〈</button>
      <button type="button" class="audio-handle end-handle" role="slider" aria-label="Fim do trecho" aria-orientation="horizontal">〉</button></div>
    <div class="audio-scale"><span>0:00</span><span class="total-time">—</span></div>
    <div class="audio-actions"><button type="button" class="secondary listen-selection" disabled>▶ Ouvir trecho</button><span class="audio-message" role="status">Carregando áudio…</span></div>`;
  container.append(root);
  media.controls = false; media.hidden = true;
  const query = selector => root.querySelector(selector);
  const track = query('.audio-track'), canvas = query('canvas'), selection = query('.audio-selection');
  const handles = [query('.start-handle'), query('.end-handle')], listen = query('.listen-selection');
  const message = query('.audio-message');
  const abort = new AbortController(), options = {signal:abort.signal};
  let duration = 0, peaks = null, disposed = false, context = null, frame = 0, playing = false;
  const time = seconds => `${Math.floor(seconds / 60)}:${(seconds % 60).toFixed(2).padStart(5, '0')}`;
  const values = () => [Number(startInput.value), Number(endInput.value)];
  const valid = () => { const [s,e] = values(); return startInput.value !== '' && endInput.value !== '' && s >= 0 && e > s && e <= duration && e-s <= 1800; };
  function pause() { playing = false; media.pause(); cancelAnimationFrame(frame); listen.textContent = '▶ Ouvir trecho'; }
  function draw() {
    const width = Math.max(1, track.clientWidth), height = 112, ratio = window.devicePixelRatio || 1;
    canvas.width = width * ratio; canvas.height = height * ratio;
    const ctx = canvas.getContext('2d'); ctx.scale(ratio, ratio); ctx.strokeStyle = '#839b8c'; ctx.lineWidth = 2;
    ctx.beginPath();
    if(peaks) for(let x = 0; x < width; x += 3) { const amplitude = peaks[Math.min(peaks.length-1, Math.floor(x/width*peaks.length))] * 46; ctx.moveTo(x,56-Math.max(1,amplitude)); ctx.lineTo(x,56+Math.max(1,amplitude)); }
    else { ctx.moveTo(0,56); ctx.lineTo(width,56); }
    ctx.stroke();
  }
  function sync() {
    pause();
    const [s,e] = values();
    const left = duration ? Math.max(0,Math.min(100,s/duration*100)) : 0;
    const right = duration ? Math.max(left,Math.min(100,e/duration*100)) : 100;
    selection.style.left = `${left}%`; selection.style.width = `${right-left}%`;
    handles.forEach((handle,index) => {
      handle.style.left = `${index ? right : left}%`; handle.disabled = !duration;
      handle.setAttribute('aria-valuemin', '0'); handle.setAttribute('aria-valuemax', String(duration));
      handle.setAttribute('aria-valuenow', String(index ? e : s)); handle.setAttribute('aria-valuetext', time(index ? e : s));
    });
    query('.selection-time').textContent = `${time(s)} → ${time(e)} · ${time(Math.max(0,e-s))}`;
    listen.disabled = !valid();
    endInput.setCustomValidity(duration && e > duration ? 'O fim deve estar dentro da duração do áudio.' : e <= s ? 'O fim deve ser maior que o início.' : e-s > 1800 ? 'Selecione até 30 minutos.' : '');
  }
  function setBoundary(index, value) {
    const [s,e] = values();
    value = index ? Math.min(duration,Math.max(s+0.001,value)) : Math.max(0,Math.min(e-0.001,value));
    (index ? endInput : startInput).value = value.toFixed(3); sync();
  }
  handles.forEach((handle,index) => {
    handle.addEventListener('pointerdown', event => { if(handle.disabled || handle.matches(':disabled')) return; handle.setPointerCapture(event.pointerId); event.preventDefault(); }, options);
    handle.addEventListener('pointermove', event => {
      if(!handle.hasPointerCapture(event.pointerId) || handle.matches(':disabled')) return;
      const rect = track.getBoundingClientRect(); setBoundary(index,(event.clientX-rect.left)/rect.width*duration);
    }, options);
    handle.addEventListener('keydown', event => {
      const direction = {ArrowLeft:-1,ArrowDown:-1,ArrowRight:1,ArrowUp:1}[event.key];
      if(direction) { event.preventDefault(); setBoundary(index,values()[index]+direction*(event.shiftKey ? 1 : 0.1)); }
      else if(event.key === 'Home' || event.key === 'End') { event.preventDefault(); setBoundary(index,event.key === 'Home' ? 0 : duration); }
    }, options);
  });
  [startInput,endInput].forEach(input => input.addEventListener('input',sync,options));
  function tick() {
    if(disposed || !playing) return;
    query('.audio-playhead').style.left = `${Math.min(100,media.currentTime/duration*100)}%`;
    if(media.currentTime >= values()[1] || media.ended) { pause(); return; }
    frame = requestAnimationFrame(tick);
  }
  listen.addEventListener('click', async () => {
    if(playing) { pause(); return; }
    if(!valid()) return;
    media.currentTime = values()[0]; playing = true; listen.textContent = 'Ⅱ Pausar trecho';
    try { await media.play(); if(playing && !disposed) tick(); }
    catch { pause(); if(!disposed) message.textContent = 'Não foi possível reproduzir este formato. Você ainda pode executar o corte.'; }
  }, options);
  media.addEventListener('timeupdate', () => { if(playing && media.currentTime >= values()[1]) pause(); }, options);
  media.addEventListener('ended',pause,options);
  media.addEventListener('error', () => { pause(); message.textContent = 'Prévia indisponível neste navegador. Informe os tempos abaixo para cortar.'; }, options);
  media.addEventListener('loadedmetadata', async () => {
    if(!Number.isFinite(media.duration) || media.duration <= 0 || disposed) return;
    duration = Math.floor(media.duration*1000)/1000;
    startInput.value = '0'; endInput.value = String(Math.min(duration,1800));
    startInput.max = endInput.max = String(duration);
    query('.total-time').textContent = time(duration); sync();
    message.textContent = 'Arraste as duas pontas para escolher o trecho.';
    // Avoid allocating very large decoded PCM buffers for long compressed recordings.
    if(duration > 600) { message.textContent = 'Áudio longo: use os seletores e a reprodução. Forma de onda disponível até 10 minutos.'; return; }
    try {
      context = new AudioContext();
      const bytes = await selected.arrayBuffer(); if(disposed) return;
      const decoded = await context.decodeAudioData(bytes); if(disposed) return;
      peaks = new Float32Array(1200);
      for(let channel = 0; channel < decoded.numberOfChannels; channel++) {
        const data = decoded.getChannelData(channel), bucket = data.length/peaks.length;
        for(let i=0;i<peaks.length;i++) for(let j=Math.floor(i*bucket);j<Math.min(data.length,(i+1)*bucket);j+=Math.max(1,Math.floor(bucket/100))) peaks[i]=Math.max(peaks[i],Math.abs(data[j]));
      }
      draw();
    } catch { if(!disposed) message.textContent = 'Forma de onda indisponível para este formato. Use os seletores para escolher o trecho.'; }
    finally { if(context && context.state !== 'closed') void context.close(); }
  }, {...options,once:true});
  const observer = new ResizeObserver(draw); observer.observe(track); sync(); draw();
  return {pause, destroy() { disposed = true; pause(); abort.abort(); observer.disconnect(); if(context && context.state !== 'closed') void context.close(); media.removeAttribute('src'); media.load(); startInput.removeAttribute('max'); endInput.removeAttribute('max'); endInput.setCustomValidity(''); root.remove(); }};
}
