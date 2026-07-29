import re

with open('src/main/java/fr/wilddifficulty/gui/GuiManager.java', 'r', encoding='utf-8') as f:
    content = f.read()

pattern = r'createItem\([^,]+,\s*("[^"]+")(?:,\s*("[^"]+"))?(?:,\s*("[^"]+"))?(?:,\s*("[^"]+"))?'
matches = re.finditer(pattern, content)

with open('extracted_strings.txt', 'w', encoding='utf-8') as f:
    for m in matches:
        f.write(m.group(0) + '\n')
