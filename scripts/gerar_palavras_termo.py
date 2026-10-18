"""Gera o dicionário de palpites do Termo (/termo) para palavras de 6 a 10 letras.

Expande o Hunspell pt_BR do VERO/LibreOffice (LGPLv3/MPL): aplica os sufixos
e prefixos (com continuação e cross product) para chegar em plurais e
conjugações, mantém só entradas minúsculas (descarta nomes próprios) e
imprime, sem acento e em maiúsculas, as que ficam só com A-Z e com tamanho
entre MIN e MAX.

As de 5 letras continuam em src/main/resources/termo/palavras-validas.txt,
que é editado à mão (tem SEMAC, CIBER e afins); este script gera só o
arquivo das longas:

    python3 scripts/gerar_palavras_termo.py \
        /usr/share/hunspell/pt_BR.aff /usr/share/hunspell/pt_BR.dic 6 10 \
        | gzip -9 > src/main/resources/termo/palavras-validas-6a10.txt.gz
"""
import re
import sys
import unicodedata

AFF, DIC = sys.argv[1], sys.argv[2]
MIN, MAX = int(sys.argv[3]), int(sys.argv[4])

regras = {}  # flag -> (tipo, cross, [(strip, add, contflags, cond_regex)])
proibida = None
with open(AFF, encoding="utf-8") as f:
    linhas = f.read().splitlines()
i = 0
while i < len(linhas):
    partes = linhas[i].split()
    if partes and partes[0] == "FORBIDDENWORD":
        proibida = partes[1]
    if len(partes) == 4 and partes[0] in ("SFX", "PFX") and partes[2] in ("Y", "N") and partes[3].isdigit():
        tipo, flag, cross, n = partes[0], partes[1], partes[2] == "Y", int(partes[3])
        lista = []
        for j in range(1, n + 1):
            p = linhas[i + j].split()
            strip = "" if p[2] == "0" else p[2]
            add, _, cont = p[3].partition("/")
            add = "" if add == "0" else add
            cond = p[4] if len(p) > 4 else "."
            rx = re.compile((cond + "$") if tipo == "SFX" else ("^" + cond))
            lista.append((strip, add, cont, rx))
        regras.setdefault(flag, (tipo, cross, []))[2].extend(lista)
        i += n
    i += 1


def sufixos(palavra, flags):
    for fl in flags:
        r = regras.get(fl)
        if not r or r[0] != "SFX":
            continue
        for strip, add, cont, rx in r[2]:
            if rx.search(palavra) and palavra.endswith(strip):
                yield palavra[: len(palavra) - len(strip)] + add, r[1], cont


def prefixos(palavra, flags):
    for fl in flags:
        r = regras.get(fl)
        if not r or r[0] != "PFX":
            continue
        for strip, add, cont, rx in r[2]:
            if rx.search(palavra) and palavra.startswith(strip):
                yield add + palavra[len(strip):], r[1], cont


def expandir(base, flags):
    formas = {base}
    for s, cross, cont in sufixos(base, flags):
        formas.add(s)
        for s2, _, _ in sufixos(s, cont):
            formas.add(s2)
        if cross:
            for p, pcross, _ in prefixos(s, flags):
                if pcross:
                    formas.add(p)
    for p, _, _ in prefixos(base, flags):
        formas.add(p)
    return formas


def normalizar(t):
    return unicodedata.normalize("NFD", t).encode("ascii", "ignore").decode().upper()


saida = set()
with open(DIC, encoding="utf-8-sig") as f:
    next(f)
    for linha in f:
        linha = linha.strip()
        if not linha:
            continue
        base, _, flags = linha.partition("/")
        if proibida and proibida in flags:
            continue
        if base != base.lower():
            continue
        for forma in expandir(base, flags):
            n = normalizar(forma)
            if MIN <= len(n) <= MAX and re.fullmatch(r"[A-Z]+", n) and forma == forma.lower():
                saida.add(n)

for p in sorted(saida, key=lambda w: (len(w), w)):
    print(p)
