# -*- coding: utf-8 -*-
"""
Builds app/src/main/assets/orot_data.json out of the Wikisource export of ספר אורות.

The source file mirrors the structure of the printed book:

    text -> מדור -> [ פסקה, פסקה, ... ]

where a "פסקה" is the lettered unit of the book (אורות ארץ ישראל א׳, ב׳, ...).
A nested section stores every פסקה as a list of its own display lines; a flat
section stores one string per פסקה.

Each מדור becomes a chapter, each פסקה becomes a Paragraph row whose
paragraphLetter is the letter it carries in the book (א׳, ב׳, ... קז׳).
Replaces the old scrape_full_orot.py, which split raw wikitext on newlines and
therefore invented paragraph boundaries, duplicated the vowelled and unvowelled
copies of the same page, and swept navigation links into the text.
"""

import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.abspath(os.path.join(HERE, '..', '..', '..', '..'))
DEFAULT_SOURCE = os.path.join(HERE, 'orot_wikisource.json')
DEFAULT_OUTPUT = os.path.join(REPO_ROOT, 'app', 'src', 'main', 'assets', 'orot_data.json')

BOOK = {
    'id': 'orot',
    'title': 'ספר אורות',
    'author': 'הראי״ה קוק זצ״ל',
}

# (chapter id, Hebrew chapter title, path into the source "text" object)
CHAPTERS = [
    ('eretz_israel',     'ארץ ישראל',      ['Lights from Darkness', 'Land of Israel']),
    ('milchama',         'המלחמה',         ['Lights from Darkness', 'War']),
    ('israel_techiyato', 'ישראל ותחייתו',  ['Lights from Darkness', 'Israel and its Rebirth']),
    ('orot_techia',      'אורות התחיה',    ['Lights from Darkness', 'Lights of Rebirth']),
    ('kriaa_gdola',      'קריאה גדולה',    ['Lights from Darkness', 'Great Calling']),

    ('ideot_1', 'למהלך האידיאות א׳ - האידיאה האלהית והאידיאה הלאומית באדם',
     ['The Process of Ideals in Israel', 'The Godly and the National Ideal in the Individual']),
    ('ideot_2', 'למהלך האידיאות ב׳ - האידיאה האלהית והאידיאה הלאומית בישראל',
     ['The Process of Ideals in Israel', 'The Godly and the National Ideal in Israel']),
    ('ideot_3', 'למהלך האידיאות ג׳ - מצב הירידה והפרוד בין האידיאות',
     ['The Process of Ideals in Israel', 'Dissolution of Ideals']),
    ('ideot_4', 'למהלך האידיאות ד׳ - המצב בגלות',
     ['The Process of Ideals in Israel', 'The Situation in Exile']),
    ('ideot_5', 'למהלך האידיאות ה׳ - בית ראשון ובית שני. האידיאה הדתית. המצב הישראלי ויחוסו לאנושיות',
     ['The Process of Ideals in Israel', 'The First and Second Temples; Religion']),
    ('ideot_6', 'למהלך האידיאות ו׳ - התאחדות האידיאות בכנסת ישראל בתחיתה בארצה, רשמי דרכיה ופעולותיה',
     ['The Process of Ideals in Israel', 'Unification of Ideals']),

    ('zeraonim_1', 'זרעונים: א. צמאון לאל חי',         ['Seeds', 'Thirst for the Living God']),
    ('zeraonim_2', 'זרעונים: ב. חכם עדיף מנביא',       ['Seeds', 'The Wise is Preferable to Prophet']),
    ('zeraonim_3', 'זרעונים: ג. הנשמות של עולם התוהו', ['Seeds', 'The Souls of the World of Chaos']),
    ('zeraonim_4', 'זרעונים: ד. מעשי יצירה',           ['Seeds', 'Acts of Creation']),
    ('zeraonim_5', 'זרעונים: ה. יסורים ממרקים',        ['Seeds', 'Suffering Cleanses']),
    ('zeraonim_6', 'זרעונים: ו. למלחמת הדעות והאמונות', ['Seeds', 'The War of Ideas']),
    ('zeraonim_7', 'זרעונים: ז. נשמת הלאומיות וגופה',  ['Seeds', 'National Soul and Body']),
    ('zeraonim_8', 'זרעונים: ח. ערך התחיה',            ['Seeds', 'The Value of Rebirth']),

    ('orot_israel_1', 'אורות ישראל א׳', ['Lights of Israel', 'The Essence of Israel']),
    ('orot_israel_2', 'אורות ישראל ב׳', ['Lights of Israel', 'The Individual and the Collective']),
    ('orot_israel_3', 'אורות ישראל ג׳', ['Lights of Israel', 'Connection to the Collective']),
    ('orot_israel_4', 'אורות ישראל ד׳', ['Lights of Israel', 'Love of Israel']),
    ('orot_israel_5', 'אורות ישראל ה׳', ['Lights of Israel', 'Israel and the Nations']),
    ('orot_israel_6', 'אורות ישראל ו׳', ['Lights of Israel', 'Nationhood of Israel']),
    ('orot_israel_7', 'אורות ישראל ז׳', ["Lights of Israel", "Israel's Soul and its Rebirth"]),
    ('orot_israel_8', 'אורות ישראל ח׳', ['Lights of Israel', 'Preciousness of Israel']),
    ('orot_israel_9', 'אורות ישראל ט׳', ['Lights of Israel', 'Holiness of Israel']),
]

# Wikisource stores the lines of a quoted piyyut as separate strings, which would
# otherwise turn a single quotation into three lettered paragraphs. Each entry is
# (chapter id, first index, last index, expected opening) over the 1-based source
# strings; the opening is asserted so a reworded source fails loudly.
MERGES = [
    ('ideot_5', 13, 15, 'ישנה ויחלף כליל'),
]

_ONES = ['', 'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט']
_TENS = ['', 'י', 'כ', 'ל', 'מ', 'נ', 'ס', 'ע', 'פ', 'צ']
_HUNDREDS = ['', 'ק', 'ר', 'ש']


def gematria(n):
    """1 -> 'א', 15 -> 'טו', 107 -> 'קז'. Spells 15/16 as טו/טז, not as the Name."""
    if n < 1:
        raise ValueError('gematria is defined for positive numbers only')
    out = []
    while n >= 400:
        out.append('ת')
        n -= 400
    if n >= 100:
        out.append(_HUNDREDS[n // 100])
        n %= 100
    if n in (15, 16):
        out.append('טו' if n == 15 else 'טז')
        n = 0
    if n >= 10:
        out.append(_TENS[n // 10])
        n %= 10
    if n > 0:
        out.append(_ONES[n])
    return ''.join(out)


def paragraph_letter(n):
    return gematria(n) + '׳'


def clean(text):
    """Collapse runs of whitespace; the source carries no markup of its own."""
    return re.sub(r'\s+', ' ', text).strip()


def piskaot(node, path):
    """Return one string per lettered פסקה, keeping its internal line breaks."""
    if not isinstance(node, list) or not node:
        raise ValueError('expected a non-empty list at %s' % ' / '.join(path))
    if isinstance(node[0], str):
        # Flat section: one string per פסקה.
        return [s for s in (clean(s) for s in node) if s]
    # Nested section: each פסקה is a list of its display lines.
    result = []
    for sub in node:
        lines = [s for s in (clean(s) for s in sub) if s]
        if lines:
            result.append('\n\n'.join(lines))
    return result


def apply_merges(chapter_id, units):
    for cid, first, last, opening in MERGES:
        if cid != chapter_id:
            continue
        if len(units) < last:
            raise ValueError('%s has only %d paragraphs, cannot merge %d-%d'
                             % (cid, len(units), first, last))
        head = units[first - 1].lstrip('"״“')
        if not head.startswith(opening):
            raise ValueError('%s paragraph %d no longer starts with %r'
                             % (cid, first, opening))
        units = units[:first - 1] + ['\n'.join(units[first - 1:last])] + units[last:]
    return units


def resolve(text, path):
    node = text
    for key in path:
        node = node[key]
    return node


def build(source_path):
    with open(source_path, encoding='utf-8') as f:
        text = json.load(f)['text']

    chapters = []
    paragraphs = []
    for order_index, (chapter_id, title, path) in enumerate(CHAPTERS):
        units = apply_merges(chapter_id, piskaot(resolve(text, path), path))
        if not units:
            raise ValueError('no paragraphs for %s' % chapter_id)
        chapters.append({
            'id': chapter_id,
            'bookId': BOOK['id'],
            'title': title,
            'orderIndex': order_index,
        })
        for i, body in enumerate(units):
            paragraphs.append({
                'id': '%s_p_%d' % (chapter_id, i + 1),
                'chapterId': chapter_id,
                'orderIndex': i,
                'paragraphLetter': paragraph_letter(i + 1),
                'textContent': body,
            })

    return {'book': BOOK, 'chapters': chapters, 'paragraphs': paragraphs}


def main():
    source = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SOURCE
    output = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_OUTPUT

    data = build(source)
    with open(output, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print('wrote %s' % output)
    print('chapters:   %d' % len(data['chapters']))
    print('paragraphs: %d' % len(data['paragraphs']))
    for c in data['chapters']:
        n = sum(1 for p in data['paragraphs'] if p['chapterId'] == c['id'])
        print('  %-18s %3d  (א׳..%s)' % (c['id'], n, paragraph_letter(n)))


if __name__ == '__main__':
    main()
