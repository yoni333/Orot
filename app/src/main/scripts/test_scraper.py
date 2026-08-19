import urllib.request
import json
import urllib.parse
import re
import time

headers = {'User-Agent': 'OrotReaderApp/1.0 (contact: support@orotapp.org)'}

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
    # Remove general HTML tags
    text = re.sub(r'<[^>]+>', '', text)
    # Remove templates {{...}} even multi-line
    text = re.sub(r'\{\{[^\{\}]*\}\}', '', text)
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
    for line in lines:
        cleaned = line.strip()
        if not cleaned:
            continue
        if cleaned.startswith('{{') or cleaned.startswith('}}'):
            continue
        if cleaned.startswith('__') and cleaned.endswith('__'):
            continue
        paragraphs.append(cleaned)
    return paragraphs

print("Testing scraper helper...")
batch = fetch_batch_wikitext(['אורות ארץ ישראל פרק א', 'קריאה גדולה'])
for t, content in batch.items():
    paras = clean_wikitext_to_paragraphs(content)
    print(f"Page '{t}': {len(paras)} paragraphs. First: {paras[0][:60] if paras else 'Empty'}")
