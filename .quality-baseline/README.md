# Quality Baseline

Arquivos JSON congelados que servem de piso para o ratchet em `./scripts/quality.sh`.

## Quando atualizar

- Quando uma métrica subiu legitimamente (mais cobertura, menos violações) e o time quer fixar o novo piso.
- **Nunca** para "destravar" um build vermelho. Se o build falhou, a feature deve melhorar a métrica, não relaxar o piso.

## Como atualizar

```bash
./scripts/quality.sh --full --update-baseline
git add .quality-baseline/
git commit -m "chore(quality): ratchet baseline up"
```

Updates de baseline são commits explícitos, revisáveis em PR.
