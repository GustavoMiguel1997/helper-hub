# Limites do contêiner HelperHub

O Compose define as mesmas restrições usadas no Coolify em produção.

- Usuário 10001:10001, sem capabilities e com no-new-privileges.
- Filesystem somente leitura e /tmp em tmpfs de 512 MiB, com noexec, nosuid e nodev.
- Memória de 2 GiB, até 2 CPUs e 256 processos/threads.
- Heap Java limitado a 35% da memória, reservando espaço para memória nativa, FFmpeg e tmpfs.

Na aplicação Coolify, configurar `limits_memory=2048m`, `limits_cpus=2` e as opções adicionais Docker abaixo. As opções devem persistir na aplicação para os próximos deploys.

```
--read-only --cap-drop ALL --security-opt no-new-privileges:true --pids-limit 256 --tmpfs /tmp:rw,noexec,nosuid,nodev,size=536870912,mode=1777
```

Confirmar os valores no contêiner criado, além de testar saúde, compressão de vídeo, áudio e imagem. A falta de espaço temporário retorna erro de processamento; os intermediários são removidos. O tmpfs também descarta resíduos quando o contêiner termina. Os limites são por instância e precisam ser calibrados se o uso ou os limites dos arquivos aumentarem.

Swagger continua restrito ao ingresso privado do Tailscale, conforme `helperhub-private-docs.yaml`.
