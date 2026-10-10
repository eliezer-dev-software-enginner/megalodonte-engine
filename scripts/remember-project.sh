#!/usr/bin/env bash
# Keep command-line exports and the editor's recent-project preference in sync.
set -euo pipefail
if [[ $# -ne 1 ]]; then echo "Uso: $0 /pasta/do/jogo" >&2; exit 1; fi
python3 - "$1" <<'PY'
import json,os,sys,tempfile
from pathlib import Path
project=(Path(sys.argv[1])/'game.json').resolve()
with project.open() as file:
    document=json.load(file)
if not isinstance(document,dict) or not isinstance(document.get('scenes'),list) or not document['scenes']:
    raise SystemExit('Projeto inválido: nenhuma cena; preferência anterior preservada.')
preference=Path(os.environ.get('ENGINE_PREFERENCES_FILE',str(Path.home()/'.config/megalodonte-engine/last-saved-project.txt')))
preference.parent.mkdir(parents=True,exist_ok=True)
fd,temporary=tempfile.mkstemp(prefix='recent-',suffix='.tmp',dir=preference.parent)
try:
    with os.fdopen(fd,'w') as file:file.write(str(project))
    os.replace(temporary,preference)
finally:
    if os.path.exists(temporary):os.unlink(temporary)
print('Último projeto registrado:',project)
PY
