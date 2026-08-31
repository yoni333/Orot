# RETIRED - do not run. See OLD_DATA/README.md.
# This script still writes app/src/main/assets/orot_data.json relative to the
# repo root; running it would overwrite the shipped asset with the old, broken
# paragraph split. Regenerate with: py app/src/main/scripts/build_orot_data.py
#
import json
import urllib.request
import urllib.parse
import re
import time

def get_page_text(page):
    url = f"https://he.wikisource.org/w/api.php?action=parse&page={urllib.parse.quote(page)}&prop=text&format=json"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) OrotAppScraper/1.0'})
    try:
        response = urllib.request.urlopen(req)
        res = json.loads(response.read())
        if 'parse' in res:
            return res['parse']['text']['*']
    except Exception as e:
        print(f"Error fetching {page}: {e}")
    return None

from html.parser import HTMLParser

class MyHTMLParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.recording = 0
        self.data = []
        self.paragraphs = []

    def handle_starttag(self, tag, attrs):
        if tag == 'p':
            self.recording = 1
            self.data = []

    def handle_endtag(self, tag):
        if tag == 'p' and self.recording:
            self.recording = 0
            text = "".join(self.data).strip()
            text = re.sub(r'<[^>]+>', '', text)
            if len(text) > 10 and "<<" not in text and "חזרה לתוכן" not in text:
                self.paragraphs.append(text)

    def handle_data(self, data):
        if self.recording:
            self.data.append(data)

main_html = get_page_text("אורות")
if not main_html:
    print("Failed to get main page")
    exit(1)

links = re.findall(r'href="/wiki/([^"]+)"', main_html)
chapters_to_scrape = []
seen = set()
for link in links:
    link = urllib.parse.unquote(link)
    if ':' in link: continue
    if 'אורות_התחיה' in link or 'אורות_ישראל' in link or 'זרעונים' in link or 'ארץ_ישראל' in link or 'המלחמה' in link or 'המהלך_האידיאות' in link:
        if link not in seen:
            seen.add(link)
            chapters_to_scrape.append(link)

print(f"Found {len(chapters_to_scrape)} potential chapters.")

chapters = []
paragraphs = []
book_id = "orot"

def clean_title(title):
    t = title.replace('_', ' ').replace('אורות/', '')
    return t

for idx, page in enumerate(chapters_to_scrape):
    print(f"Scraping {page}...")
    html = get_page_text(page)
    if not html:
        time.sleep(1)
        continue
        
    title = clean_title(page)
    chapter_id = f"{book_id}_ch_{idx}"
    
    parser = MyHTMLParser()
    parser.feed(html)
    
    if len(parser.paragraphs) > 0:
        chapters.append({
            "id": chapter_id,
            "bookId": book_id,
            "title": title,
            "orderIndex": idx
        })
        for p_idx, text in enumerate(parser.paragraphs):
            paragraphs.append({
                "id": f"{chapter_id}_p_{p_idx}",
                "chapterId": chapter_id,
                "textContent": text,
                "paragraphLetter": "", 
                "orderIndex": p_idx
            })
            
    time.sleep(0.5)

data = {
    "book": { "id": book_id, "title": "אורות", "author": "הרב אברהם יצחק הכהן קוק" },
    "chapters": chapters,
    "paragraphs": paragraphs
}

with open('app/src/main/assets/orot_data.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print(f"Scraped {len(chapters)} chapters and {len(paragraphs)} paragraphs.")
