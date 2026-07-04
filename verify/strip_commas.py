#!/usr/bin/env python3
"""Убирает завершающие запятые (Kotlin 1.4+) для проверки старым компилятором.
Понимает строки, char-литералы, комментарии и when-ветки (запятая перед ->)."""
import sys

def strip(src):
    out = []; i = 0; n = len(src)
    S = C = L = B = False
    while i < n:
        c = src[i]; nx = src[i+1] if i+1 < n else ''
        if L:
            out.append(c)
            if c == '\n': L = False
            i += 1; continue
        if B:
            if c == '*' and nx == '/': B = False; out.append('*/'); i += 2; continue
            out.append(c); i += 1; continue
        if S:
            if c == '\\': out.append(c + nx); i += 2; continue
            if c == '"': S = False
            out.append(c); i += 1; continue
        if C:
            if c == '\\': out.append(c + nx); i += 2; continue
            if c == "'": C = False
            out.append(c); i += 1; continue
        if c == '/' and nx == '/': L = True; out.append('//'); i += 2; continue
        if c == '/' and nx == '*': B = True; out.append('/*'); i += 2; continue
        if c == '"': S = True; out.append(c); i += 1; continue
        if c == "'": C = True; out.append(c); i += 1; continue
        if c == ',':
            j = i + 1
            while j < n:
                cj = src[j]
                if cj in ' \t\n\r': j += 1; continue
                if cj == '/' and j+1 < n and src[j+1] == '/':
                    while j < n and src[j] != '\n': j += 1
                    continue
                if cj == '/' and j+1 < n and src[j+1] == '*':
                    k = src.find('*/', j+2); j = n if k < 0 else k + 2; continue
                break
            if j < n and (src[j] in ')]}' or src[j:j+2] == '->'):
                i += 1; continue
        out.append(c); i += 1
    return ''.join(out)

if __name__ == '__main__':
    sys.stdout.write(strip(open(sys.argv[1], encoding='utf-8').read()))
