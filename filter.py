import json

with open('app/src/main/assets/orot_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

new_paragraphs = []
for p in data['paragraphs']:
    t = p['textContent']
    if "חזרה לתוכן" in t or "<<" in t or ">>" in t:
        continue
    new_paragraphs.append(p)

data['paragraphs'] = new_paragraphs

with open('app/src/main/assets/orot_data.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Filtered out boilerplate.")
