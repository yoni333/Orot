# RETIRED - do not run. See OLD_DATA/README.md.
# This script still writes app/src/main/assets/orot_data.json relative to the
# repo root; running it would overwrite the shipped asset with the old, broken
# paragraph split. Regenerate with: py app/src/main/scripts/build_orot_data.py
#
import json
import urllib.request
import urllib.parse
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
            if len(text) > 10:
                self.paragraphs.append(text)

    def handle_data(self, data):
        if self.recording:
            self.data.append(data)

pages = [
    ("אורות_התחיה_פרק_א", "אורות התחיה א׳"),
    ("אורות_התחיה_פרק_ב", "אורות התחיה ב׳"),
    ("אורות_התחיה_פרק_ג", "אורות התחיה ג׳"),
    ("אורות_התחיה_פרק_ד", "אורות התחיה ד׳"),
    ("אורות_התחיה_פרק_ה", "אורות התחיה ה׳"),
    ("אורות_ישראל_פרק_א", "אורות ישראל א׳"),
    ("אורות_ישראל_פרק_ב", "אורות ישראל ב׳"),
    ("זרעונים_א", "זרעונים א׳")
]

chapters = []
paragraphs = []
book_id = "orot"

for idx, (page, title) in enumerate(pages):
    url = f"https://he.wikisource.org/w/api.php?action=parse&page={urllib.parse.quote(page)}&prop=text&format=json"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AndroidBuildAgent/1.0'})
        response = urllib.request.urlopen(req)
        res = json.loads(response.read())
        html = res['parse']['text']['*']
        
        chapter_id = f"{book_id}_ch_{idx}"
        chapters.append({
            "id": chapter_id,
            "bookId": book_id,
            "title": title,
            "orderIndex": idx
        })
        
        parser = MyHTMLParser()
        parser.feed(html)
        
        for p_idx, text in enumerate(parser.paragraphs):
            paragraphs.append({
                "id": f"{chapter_id}_p_{p_idx}",
                "chapterId": chapter_id,
                "textContent": text,
                "paragraphLetter": "", 
                "orderIndex": p_idx
            })
    except Exception as e:
        print(f"Failed {page}: {e}")

data = {
    "book": { "id": book_id, "title": "אורות", "author": "הרב אברהם יצחק הכהן קוק" },
    "chapters": chapters,
    "paragraphs": paragraphs
}

with open('app/src/main/assets/orot_data.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print(f"Scraped {len(chapters)} chapters and {len(paragraphs)} paragraphs.")
