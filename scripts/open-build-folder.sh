#!/usr/bin/env bash
set -euo pipefail
if [[ "${ENGINE_NO_OPEN:-0}" == 1 ]]; then exit 0; fi
if [[ $# -ne 1 || ! -d "$1" ]]; then echo "Diretório de build não encontrado" >&2; exit 1; fi
case "$(uname -s)" in
  Darwin) open "$1" ;;
  MINGW*|MSYS*|CYGWIN*) explorer "$(cygpath -w "$1")" ;;
  *) if [[ -z "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]]; then echo "Build concluída: $1 (sem sessão gráfica para abrir o explorador)"; else xdg-open "$1" >/dev/null 2>&1 || echo "Não foi possível abrir o explorador: $1" >&2; fi ;;
esac
