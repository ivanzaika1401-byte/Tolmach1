#!/usr/bin/env python3
"""Готовит копии исходников для проверочного компилятора Kotlin 1.3:
1) убирает завершающие запятые (синтаксис 1.4+), включая when-ветки;
2) вырезает токен @Composable (1.3 не умеет аннотации на типах функций;
   для типовой проверки аннотация инертна).
Понимает строки, char-литералы и оба вида комментариев."""
import sys


def _walk(src, on_char):
    out = []
    i = 0
    n = len(src)
    S = C = L = B = False
    while i < n:
        c = src[i]
        nx = src[i + 1] if i + 1 < n else ''
        if L:
            out.append(c)
            if c == '\n':
                L = False
            i += 1
            continue
        if B:
            if c == '*' and nx == '/':
                B = False
                out.append('*/')
                i += 2
                continue
            out.append(c)
            i += 1
            continue
        if S:
            if c == '\\':
                out.append(c + nx)
                i += 2
                continue
            if c == '"':
                S = False
            out.append(c)
            i += 1
            continue
        if C:
            if c == '\\':
                out.append(c + nx)
                i += 2
                continue
            if c == "'":
                C = False
            out.append(c)
            i += 1
            continue
        if c == '/' and nx == '/':
            L = True
            out.append('//')
            i += 2
            continue
        if c == '/' and nx == '*':
            B = True
            out.append('/*')
            i += 2
            continue
        if c == '"':
            S = True
            out.append(c)
            i += 1
            continue
        if c == "'":
            C = True
            out.append(c)
            i += 1
            continue
        step = on_char(src, i, out)
        if step:
            i += step
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def strip(src):
    def commas(s, i, out):
        if s[i] != ',':
            return 0
        j = i + 1
        n = len(s)
        while j < n:
            cj = s[j]
            if cj in ' \t\n\r':
                j += 1
                continue
            if cj == '/' and j + 1 < n and s[j + 1] == '/':
                while j < n and s[j] != '\n':
                    j += 1
                continue
            if cj == '/' and j + 1 < n and s[j + 1] == '*':
                k = s.find('*/', j + 2)
                j = n if k < 0 else k + 2
                continue
            break
        if j < n and (s[j] in ')]}' or s[j:j + 2] == '->'):
            return 1
        return 0

    def composable(s, i, out):
        token = '@Composable'
        if s[i] == '@' and s.startswith(token, i):
            after = s[i + len(token)] if i + len(token) < len(s) else ' '
            if not (after.isalnum() or after == '_'):
                return len(token)
        return 0

    return _walk(_walk(src, composable), commas)


if __name__ == '__main__':
    sys.stdout.write(strip(open(sys.argv[1], encoding='utf-8').read()))
