# RETIRED - do not run. See OLD_DATA/README.md.
# This script still writes app/src/main/assets/orot_data.json relative to the
# repo root; running it would overwrite the shipped asset with the old, broken
# paragraph split. Regenerate with: py app/src/main/scripts/build_orot_data.py
#
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
