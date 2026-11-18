function createAudioMixer(container, files, onChange) {
  const lifecycle = new AbortController();
  const events = {signal:lifecycle.signal};
  let tracks = files.map(file => ({file, url:URL.createObjectURL(file), duration:null, failed:false}));
  let index = 0;
  let sequence = false;
  let frame = 0;
  let context = null;
  let gain = null;
  const panel = document.createElement('div');
  panel.className = 'audio-mixer';
  const list = document.createElement('ol');
  list.className = 'audio-file-list';
  const player = document.createElement('audio');
  player.controls = true;
  player.preload = 'metadata';
  const actions = document.createElement('div');
  actions.className = 'audio-actions';
  const listen = document.createElement('button');
  listen.type = 'button';
  listen.className = 'secondary';
  listen.textContent = 'Ouvir com ajustes';
  const stop = document.createElement('button');
  stop.type = 'button';
  stop.className = 'secondary';
  stop.textContent = 'Parar prévia';
  const message = document.createElement('p');
  message.className = 'field-hint';
  message.setAttribute('role','status');
  actions.append(listen,stop);
  if(onChange) panel.append(list);
  panel.append(player,actions,message);
  container.append(panel);

  function canEdit() { return !document.getElementById('controls').disabled; }
  function pause() {
    sequence = false;
    player.pause();
    cancelAnimationFrame(frame);
  }
  function envelope() {
    if(!gain || !tracks.length) return;
    const volume = Math.max(0, Math.min(2, Number(document.getElementById('volumePercent').value)/100));
    const fadeIn = Number(document.getElementById('fadeIn').value);
    const fadeOut = Number(document.getElementById('fadeOut').value);
    const total = tracks.reduce((sum,track) => sum+(track.duration || 0),0);
    const position = tracks.slice(0,index).reduce((sum,track) => sum+(track.duration || 0),0)+player.currentTime;
    const start = fadeIn > 0 ? Math.min(1, position/fadeIn) : 1;
    const end = fadeOut > 0 ? Math.min(1, Math.max(0,total-position)/fadeOut) : 1;
    gain.gain.setValueAtTime(volume*start*end,context.currentTime);
  }
  function tick() {
    envelope();
    if(!player.paused) frame = requestAnimationFrame(tick);
  }
  async function activate() {
    if(!context) {
      context = new AudioContext();
      gain = context.createGain();
      context.createMediaElementSource(player).connect(gain).connect(context.destination);
    }
    await context.resume();
    envelope();
  }
  function updateMessage() {
    const failed = tracks.some(track => track.failed);
    const ready = tracks.length > 0 && tracks.every(track => track.duration !== null && !track.failed);
    listen.disabled = !ready;
    if(failed) message.textContent = 'A prévia deste formato não está disponível no navegador. Gere o resultado para ouvir o arquivo processado.';
    else if(!tracks.length) message.textContent = 'Adicione arquivos para preparar sua sequência.';
    else if(!ready) message.textContent = 'Carregando a duração para preparar a prévia…';
    else {
      const total = tracks.reduce((sum,track) => sum+track.duration,0);
      const fadeIn = Number(document.getElementById('fadeIn').value);
      const fadeOut = Number(document.getElementById('fadeOut').value);
      if(fadeIn > total || fadeOut > total) {
        listen.disabled = true;
        message.textContent = 'Cada fade deve caber na duração total do áudio.';
        return;
      }
      const order = onChange ? ' Arraste os arquivos ou use as setas para ordenar.' : '';
      message.textContent = tracks.length+(tracks.length === 1 ? ' arquivo · ' : ' arquivos · ')+total.toFixed(1)+' s.'+order
          +' Os fades são aplicados ao início e ao fim da faixa completa.';
    }
  }
  function selectFirst() {
    index = 0;
    if(tracks.length) player.src = tracks[0].url;
    else { player.removeAttribute('src'); player.load(); }
  }
  function changed() {
    pause();
    selectFirst();
    renderList();
    updateMessage();
    onChange?.(tracks.map(track => track.file));
  }
  function move(track, target) {
    if(!canEdit() || target < 0 || target >= tracks.length) return;
    const previous = tracks.indexOf(track);
    tracks.splice(previous,1);
    tracks.splice(target,0,track);
    changed();
    list.querySelectorAll('li')[target]?.querySelector('button:not(:disabled)')?.focus();
  }
  function renderList() {
    list.replaceChildren();
    tracks.forEach((track, position) => {
      const row = document.createElement('li');
      row.draggable = true;
      const name = document.createElement('span');
      name.className = 'audio-file-name';
      name.textContent = (position+1)+'. '+track.file.name;
      const buttons = document.createElement('div');
      buttons.className = 'audio-file-buttons';
      for(const [label, symbol, target] of [['Mover para cima','↑',position-1],['Mover para baixo','↓',position+1]]) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'secondary';
        button.textContent = symbol;
        button.setAttribute('aria-label',label+': '+track.file.name);
        button.disabled = target < 0 || target >= tracks.length;
        button.addEventListener('click',()=>move(track,target));
        buttons.append(button);
      }
      const remove = document.createElement('button');
      remove.type = 'button';
      remove.className = 'secondary';
      remove.textContent = 'Remover';
      remove.setAttribute('aria-label','Remover '+track.file.name);
      remove.addEventListener('click',()=>{
        if(!canEdit()) return;
        pause();
        track.metadata.removeAttribute('src');
        track.metadata.load();
        URL.revokeObjectURL(track.url);
        tracks = tracks.filter(item => item !== track);
        changed();
      });
      buttons.append(remove);
      row.append(name,buttons);
      row.addEventListener('dragstart',event=>{
        if(!canEdit()) { event.preventDefault(); return; }
        event.dataTransfer.setData('application/x-helperhub-audio-order',String(position));
        event.dataTransfer.effectAllowed = 'move';
      });
      row.addEventListener('dragover',event=>{ if(canEdit()) event.preventDefault(); });
      row.addEventListener('drop',event=>{
        event.preventDefault();
        const value = event.dataTransfer.getData('application/x-helperhub-audio-order');
        if(value === '') return;
        const source = Number(value);
        if(Number.isInteger(source) && tracks[source]) move(tracks[source],position);
      });
      list.append(row);
    });
  }
  tracks.forEach(track=>{
    const metadata = new Audio();
    track.metadata = metadata;
    metadata.preload = 'metadata';
    metadata.addEventListener('loadedmetadata',()=>{
      track.duration = Number.isFinite(metadata.duration) && metadata.duration > 0 ? metadata.duration : null;
      track.failed = track.duration === null;
      updateMessage();
    },events);
    metadata.addEventListener('error',()=>{track.failed=true;updateMessage();},events);
    metadata.src = track.url;
  });
  listen.addEventListener('click',async()=>{
    if(!canEdit()) return;
    pause();
    selectFirst();
    try {
      await activate();
      sequence = true;
      await player.play();
    } catch {
      sequence = false;
      message.textContent = 'A prévia com ajustes não pôde ser reproduzida. Gere o resultado para ouvir o áudio processado.';
    }
  },events);
  stop.addEventListener('click',pause,events);
  player.addEventListener('play',async()=>{
    if(!canEdit()) { pause(); return; }
    try { await activate(); cancelAnimationFrame(frame); tick(); }
    catch { pause(); message.textContent='Use Ouvir com ajustes para iniciar a prévia.'; }
  },events);
  player.addEventListener('pause',()=>cancelAnimationFrame(frame),events);
  player.addEventListener('seeked',envelope,events);
  player.addEventListener('ended',async()=>{
    if(sequence && index+1 < tracks.length) {
      index++;
      player.src = tracks[index].url;
      try { await player.play(); }
      catch { pause(); }
    } else pause();
  },events);
  for(const name of ['volumePercent','fadeIn','fadeOut']) {
    document.getElementById(name).addEventListener('input',()=>{envelope();updateMessage();},events);
  }
  selectFirst();
  renderList();
  updateMessage();
  return {
    pause,
    destroy() {
      pause();
      lifecycle.abort();
      player.removeAttribute('src');
      player.load();
      tracks.forEach(track=>{
        track.metadata.removeAttribute('src');
        track.metadata.load();
        URL.revokeObjectURL(track.url);
      });
      context?.close().catch(()=>{});
    }
  };
}
