import urllib.request
import json
import urllib.parse
import re
import time
from html.parser import HTMLParser

headers = {'User-Agent': 'OrotReaderApp/1.0 (contact: support@orotapp.org)'}

class HTMLTextExtractor(HTMLParser):
    def __init__(self):
        super().__init__()
        self.paragraphs = []
        self.current_p = []
        self.in_p = False
        self.in_ignored = False
        
    def handle_starttag(self, tag, attrs):
        attrs_dict = dict(attrs)
        cls = attrs_dict.get('class', '')
        if tag in ['p']:
            self.in_p = True
            self.current_p = []
        elif 'navbox' in cls or 'navigation' in cls or 'catlinks' in cls or 'mw-jump-link' in cls:
            self.in_ignored = True
            
    def handle_endtag(self, tag):
        if tag in ['p']:
            self.in_p = False
            text = ''.join(self.current_p).strip()
            # clean internal spaces
            text = re.sub(r'\s+', ' ', text)
            if text and len(text) > 3 and not text.startswith('חזרה לתוכן'):
                self.paragraphs.append(text)
            self.current_p = []
        elif tag in ['div', 'table']:
            self.in_ignored = False
            
    def handle_data(self, data):
        if self.in_p and not self.in_ignored:
            self.current_p.append(data)

def fetch_parsed_html_paragraphs(page_title):
    url = f'https://he.wikisource.org/w/api.php?action=parse&page={urllib.parse.quote(page_title)}&format=json&prop=text'
    req = urllib.request.Request(url, headers=headers)
    time.sleep(0.3)
    try:
        resp = urllib.request.urlopen(req)
        data = json.loads(resp.read().decode('utf-8'))
        html = data.get('parse', {}).get('text', {}).get('*', '')
        parser = HTMLTextExtractor()
        parser.feed(html)
        return parser.paragraphs
    except Exception as e:
        print(f"Error fetching parse for '{page_title}': {e}")
        return []

def fetch_batch_wikitext(titles):
    encoded_titles = urllib.parse.quote('|'.join(titles))
    url = f'https://he.wikisource.org/w/api.php?action=query&prop=revisions&rvprop=content&rvslots=main&format=json&titles={encoded_titles}'
    req = urllib.request.Request(url, headers=headers)
    resp = urllib.request.urlopen(req)
    data = json.loads(resp.read().decode('utf-8'))
    results = {}
    for pid, pinfo in data.get('query', {}).get('pages', {}).items():
        title = pinfo.get('title')
        revs = pinfo.get('revisions', [])
        if revs:
            content = revs[0].get('slots', {}).get('main', {}).get('*', '')
            results[title] = content
        else:
            results[title] = ''
    return results

def clean_wikitext_to_paragraphs(text):
    # Remove ref tags and inner content
    text = re.sub(r'<ref[^>]*>.*?</ref>', '', text, flags=re.DOTALL)
    text = re.sub(r'<ref[^>]*/>', '', text)
    # Remove comments
    text = re.sub(r'<!--.*?-->', '', text, flags=re.DOTALL)
    # Remove general HTML tags
    text = re.sub(r'<[^>]+>', '', text)
    # Remove templates
    for _ in range(4):
        text = re.sub(r'\{\{[^\{\}]*\}\}', '', text)
    # Remove wiki link formatting [[target|text]] -> text, [[target]] -> target
    text = re.sub(r'\[\[(?:[^|\]]*\|)?([^\]]+)\]\]', r'\1', text)
    # Remove bold / italics
    text = re.sub(r"'{2,5}", '', text)
    # Remove heading markers
    text = re.sub(r'==+[^=]+==+', '', text)
    # Remove category markers
    text = re.sub(r'קטגוריה:[^\n]+', '', text)

    lines = text.split('\n')
    paragraphs = []
    nav_ignore = [
        'חזרה לתוכן של ספר אורות',
        'חזרה לתוכן העניינים',
        'חזרה לדף הראשי',
        'חזרה לתוכן',
        'ניווט:'
    ]
    for line in lines:
        cleaned = line.strip()
        if not cleaned:
            continue
        if any(nav in cleaned for nav in nav_ignore):
            continue
        if cleaned.startswith('{{') or cleaned.startswith('}}'):
            continue
        if cleaned.startswith('__') and cleaned.endswith('__'):
            continue
        if cleaned.startswith('[[') and cleaned.endswith(']]'):
            continue
        if re.match(r'^-{3,}$', cleaned):
            continue
        paragraphs.append(cleaned)
    return paragraphs

hebrew_numerals = [
    (1, 'א׳'), (2, 'ב׳'), (3, 'ג׳'), (4, 'ד׳'), (5, 'ה׳'), (6, 'ו׳'), (7, 'ז׳'), (8, 'ח׳'), (9, 'ט׳'), (10, 'י׳'),
    (11, 'יא׳'), (12, 'יב׳'), (13, 'יג׳'), (14, 'יד׳'), (15, 'טו׳'), (16, 'טז׳'), (17, 'יז׳'), (18, 'יח׳'), (19, 'יט׳'), (20, 'כ׳'),
    (21, 'כא׳'), (22, 'כב׳'), (23, 'כג׳'), (24, 'כד׳'), (25, 'כה׳'), (26, 'כו׳'), (27, 'כז׳'), (28, 'כח׳'), (29, 'כט׳'), (30, 'ל׳'),
    (31, 'לא׳'), (32, 'לב׳'), (33, 'לג׳'), (34, 'לד׳'), (35, 'לה׳'), (36, 'לו׳'), (37, 'לז׳'), (38, 'לח׳'), (39, 'לט׳'), (40, 'מ׳'),
    (41, 'מא׳'), (42, 'מב׳'), (43, 'מג׳'), (44, 'מד׳'), (45, 'מה׳'), (46, 'מו׳'), (47, 'מז׳'), (48, 'מח׳'), (49, 'מט׳'), (50, 'נ׳'),
    (51, 'נא׳'), (52, 'נב׳'), (53, 'נג׳'), (54, 'נד׳'), (55, 'נה׳'), (56, 'נו׳'), (57, 'נז׳'), (58, 'נח׳'), (59, 'נט׳'), (60, 'ס׳'),
    (61, 'סא׳'), (62, 'סב׳'), (63, 'סג׳'), (64, 'סד׳'), (65, 'סה׳'), (66, 'סו׳'), (67, 'סז׳'), (68, 'סח׳'), (69, 'סט׳'), (70, 'ע׳'),
    (71, 'עא׳'), (72, 'עב׳')
]
hebrew_num_map = dict(hebrew_numerals)

# Build ordered chapter list
chapter_definitions = []

# 1. ארץ ישראל (8)
hebrew_letters = ['א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח']
for i, l in enumerate(hebrew_letters, start=1):
    chapter_definitions.append({
        'section_group': 'אורות מאופל - ארץ ישראל',
        'title': f'ארץ ישראל {hebrew_num_map[i]}',
        'wiki_title': f'אורות ארץ ישראל פרק {l}',
        'id': f'eretz_israel_{i}'
    })

# 2. המלחמה (10)
hebrew_letters_10 = ['א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט', 'י']
for i, l in enumerate(hebrew_letters_10, start=1):
    chapter_definitions.append({
        'section_group': 'אורות מאופל - המלחמה',
        'title': f'המלחמה {hebrew_num_map[i]}',
        'wiki_title': f'אורות המלחמה פרק {l}',
        'id': f'milchama_{i}'
    })

# 3. ישראל ותחייתו (32)
hebrew_letters_32 = [
    'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט', 'י',
    'יא', 'יב', 'יג', 'יד', 'טו', 'טז', 'יז', 'יח', 'יט', 'כ',
    'כא', 'כב', 'כג', 'כד', 'כה', 'כו', 'כז', 'כח', 'כט', 'ל',
    'לא', 'לב'
]
for i, l in enumerate(hebrew_letters_32, start=1):
    chapter_definitions.append({
        'section_group': 'אורות מאופל - ישראל ותחייתו',
        'title': f'ישראל ותחייתו {hebrew_num_map[i]}',
        'wiki_title': f'אורות ישראל ותחייתו פרק {l}',
        'id': f'israel_techiyato_{i}'
    })

# 4. אורות התחיה (72)
hebrew_letters_72 = [
    'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט', 'י',
    'יא', 'יב', 'יג', 'יד', 'טו', 'טז', 'יז', 'יח', 'יט', 'כ',
    'כא', 'כב', 'כג', 'כד', 'כה', 'כו', 'כז', 'כח', 'כט', 'ל',
    'לא', 'לב', 'לג', 'לד', 'לה', 'לו', 'לז', 'לח', 'לט', 'מ',
    'מא', 'מב', 'מג', 'מד', 'מה', 'מו', 'מז', 'מח', 'מט', 'נ',
    'נא', 'נב', 'נג', 'נד', 'נה', 'נו', 'נז', 'נח', 'נט', 'ס',
    'סא', 'סב', 'סג', 'סד', 'סה', 'סו', 'סז', 'סח', 'סט', 'ע',
    'עא', 'עב'
]
for i, l in enumerate(hebrew_letters_72, start=1):
    chapter_definitions.append({
        'section_group': 'אורות התחיה',
        'title': f'אורות התחיה {hebrew_num_map[i]}',
        'wiki_title': f'אורות התחיה פרק {l}',
        'id': f'orot_techia_{i}'
    })

# 5. קריאה גדולה (1)
chapter_definitions.append({
    'section_group': 'קריאה גדולה',
    'title': 'קריאה גדולה',
    'wiki_title': 'קריאה גדולה',
    'id': 'kriaa_gdola'
})

# 6. למהלך האידיאות בישראל (6)
ideot_subsections = [
    ('א', 'האידיאה האלהית והאידיאה הלאומית באדם', 'ideot_1'),
    ('ב', 'האידיאה האלהית והאידיאה הלאומית בישראל', 'ideot_2'),
    ('ג', 'מצב הירידה והפרוד בין האידיאות', 'ideot_3'),
    ('ד', 'המצב בגלות', 'ideot_4'),
    ('ה', 'בית ראשון ובית שני. האידיאה הדתית. המצב הישראלי ויחוסו לאנושיות', 'ideot_5'),
    ('ו', 'התאחדות האידיאות בכנסת ישראל בתחיתה בארצה, רשמי דרכיה ופעולותיה', 'ideot_6'),
]
for l, subtitle, cid in ideot_subsections:
    chapter_definitions.append({
        'section_group': 'למהלך האידיאות בישראל',
        'title': f'למהלך האידיאות {l}׳ - {subtitle}',
        'wiki_title': 'למהלך האידיאות בישראל',
        'id': cid,
        'ideot_part': l
    })

# 7. זרעונים (8)
zeraonim = [
    ('א. צמאון לאל חי', 'אורות/זרעונים/א. צמאון לאל חי', 'zeraonim_1'),
    ('ב. חכם עדיף מנביא', 'אורות/זרעונים/ב. חכם עדיף מנביא', 'zeraonim_2'),
    ('ג. הנשמות של עולם התוהו', 'ג. הנשמות של עולם התוהו', 'zeraonim_3'),
    ('ד. מעשי יצירה', 'ד. מעשי יצירה', 'zeraonim_4'),
    ('ה. יסורים ממרקים', 'ה. יסורים ממרקים', 'zeraonim_5'),
    ('ו. למלחמת הדעות והאמונות', 'ו. למלחמת הדעות והאמונות', 'zeraonim_6'),
    ('ז. נשמת הלאומיות וגופה', 'אורות/זרעונים/ז. נשמת הלאומיות וגופה', 'zeraonim_7'),
    ('ח. ערך התחיה', 'אורות/זרעונים/ח. ערך התחיה', 'zeraonim_8'),
]
for title, wiki_title, cid in zeraonim:
    chapter_definitions.append({
        'section_group': 'זרעונים',
        'title': f'זרעונים: {title}',
        'wiki_title': wiki_title,
        'id': cid
    })

# 8. אורות ישראל (9)
orot_israel_pages = [
    ('פרק א', 'אורות/אורות ישראל/פרק א', 'orot_israel_1'),
    ('פרק ב', 'אורות ישראל פרק ב', 'orot_israel_2'),
    ('פרק ג', 'אורות ישראל פרק ג', 'orot_israel_3'),
    ('פרק ד', 'אורות ישראל פרק ד', 'orot_israel_4'),
    ('פרק ה', 'אורות/אורות ישראל/פרק ה', 'orot_israel_5'),
    ('פרק ו', 'אורות ישראל פרק ו', 'orot_israel_6'),
    ('פרק ז', 'אורות ישראל פרק ז', 'orot_israel_7'),
    ('פרק ח', 'אורות ישראל פרק ח', 'orot_israel_8'),
    ('פרק ט', 'אורות ישראל פרק ט', 'orot_israel_9'),
]
for i, (title, wiki_title, cid) in enumerate(orot_israel_pages, start=1):
    chapter_definitions.append({
        'section_group': 'אורות ישראל',
        'title': f'אורות ישראל {hebrew_num_map[i]}',
        'wiki_title': wiki_title,
        'id': cid
    })

print(f"Total chapters to process: {len(chapter_definitions)}")

# Fetch in batches
unique_wiki_titles = list(dict.fromkeys(c['wiki_title'] for c in chapter_definitions))
print(f"Total unique wiki pages to fetch: {len(unique_wiki_titles)}")

fetched_content = {}
chunk_size = 25
for i in range(0, len(unique_wiki_titles), chunk_size):
    chunk = unique_wiki_titles[i:i + chunk_size]
    print(f"Fetching batch {i//chunk_size + 1}/{(len(unique_wiki_titles)+chunk_size-1)//chunk_size} ({len(chunk)} pages)...")
    res = fetch_batch_wikitext(chunk)
    fetched_content.update(res)
    time.sleep(0.3)

# Parse Lemehalach Haideot
ideot_full_text = fetched_content.get('למהלך האידיאות בישראל', '')
ideot_parts = {}
current_part_key = None
current_part_lines = []

for line in ideot_full_text.split('\n'):
    m = re.match(r'^==\s*([אבוגדהו])\b', line)
    if m:
        if current_part_key:
            ideot_parts[current_part_key] = '\n'.join(current_part_lines)
        current_part_key = m.group(1)
        current_part_lines = []
    else:
        if current_part_key:
            current_part_lines.append(line)

if current_part_key:
    ideot_parts[current_part_key] = '\n'.join(current_part_lines)

book = {
    'id': 'orot',
    'title': 'ספר אורות',
    'author': 'הראי״ה קוק זצ״ל'
}

chapters = []
paragraphs = []

for order_idx, cdef in enumerate(chapter_definitions):
    cid = cdef['id']
    title = cdef['title']
    wtitle = cdef['wiki_title']
    
    chapter = {
        'id': cid,
        'bookId': 'orot',
        'title': title,
        'orderIndex': order_idx
    }
    chapters.append(chapter)

    paras = []
    if 'ideot_part' in cdef:
        raw_text = ideot_parts.get(cdef['ideot_part'], '')
        paras = clean_wikitext_to_paragraphs(raw_text)
    else:
        raw_text = fetched_content.get(wtitle, '')
        # Check if contains transclusions {{:
        if '{{:' in raw_text or not raw_text.strip():
            print(f"Fetching rendered HTML for transcluded/empty page: {wtitle} ({title})")
            paras = fetch_parsed_html_paragraphs(wtitle)
        else:
            paras = clean_wikitext_to_paragraphs(raw_text)
            if len(paras) == 0:
                print(f"Fallback to rendered HTML for {wtitle} ({title})")
                paras = fetch_parsed_html_paragraphs(wtitle)

    if not paras:
        print(f"WARNING: Still no paragraphs for {title} (wiki: {wtitle})")
        paras = [f"תוכן {title}"]

    for p_idx, p_text in enumerate(paras):
        pid = f"{cid}_p_{p_idx + 1}"
        p_letter = hebrew_num_map.get(p_idx + 1, str(p_idx + 1))
        paragraphs.append({
            'id': pid,
            'chapterId': cid,
            'orderIndex': p_idx,
            'paragraphLetter': p_letter,
            'textContent': p_text
        })

output_data = {
    'book': book,
    'chapters': chapters,
    'paragraphs': paragraphs
}

output_path = 'app/src/main/assets/orot_data.json'
with open(output_path, 'w', encoding='utf-8') as f:
    json.dump(output_data, f, ensure_ascii=False, indent=2)

print(f"\n==========================================")
print(f"SUCCESSFULLY GENERATED {output_path}!")
print(f"Total Chapters: {len(chapters)}")
print(f"Total Paragraphs: {len(paragraphs)}")
print(f"==========================================")
