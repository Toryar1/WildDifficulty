import re
import os
import glob

def generate_key(text):
    clean = re.sub(r'[&§][0-9a-fk-or]', '', text)
    clean = re.sub(r'[^a-zA-Z0-9À-ÿ\s_]', '', clean)
    clean = clean.strip().lower()
    clean = re.sub(r'\s+', '_', clean)
    if not clean:
        return "empty"
    words = clean.split('_')
    key = '_'.join(words[:5])
    return f"gui.item.{key}"

def main():
    java_file = 'src/main/java/fr/wilddifficulty/gui/GuiManager.java'
    with open(java_file, 'r', encoding='utf-8') as f:
        content = f.read()

    call_pattern = r'(createItem|createToggleItem)\s*\([^;]+;'
    lang_keys = {}
    
    def call_replacer(match):
        call_text = match.group(0)
        
        def string_replacer(m):
            text = m.group(1)
            original = m.group(0)
            
            if not text.strip() or len(text.strip()) < 2: return original
            if not re.search(r'[a-zA-ZÀ-ÿ]', text): return original
            
            # Skip technical strings and already wrapped LangManager calls
            if text.isupper() and not " " in text: return original
            if "gui." in text or "wd_" in text or ".yml" in text: return original
            if text in ["true", "false"]: return original
            
            # Additional check: if this string is part of plugin.getLangManager().getRaw(...)
            # Since regex is doing search inside the entire call text, we can just check if
            # the text itself doesn't look like an English technical string (which we did above with gui.)
            
            key = generate_key(text)
            original_key = key
            counter = 1
            while key in lang_keys and lang_keys[key] != text:
                key = f"{original_key}_{counter}"
                counter += 1
                
            lang_keys[key] = text
            return f'plugin.getLangManager().getRaw("{key}")'
            
        new_call_text = re.sub(r'"([^"\\]*)"', string_replacer, call_text)
        return new_call_text

    new_content = re.sub(call_pattern, call_replacer, content)
    
    with open(java_file, 'w', encoding='utf-8') as f:
        f.write(new_content)
        
    print(f"Replaced {len(lang_keys)} unique strings in GuiManager.")
    
    lang_file = 'src/main/java/fr/wilddifficulty/config/LangManager.java'
    with open(lang_file, 'r', encoding='utf-8') as f:
        lang_content = f.read()
        
    insert_idx = lang_content.find("private void createLanguageFile")
    if insert_idx != -1:
        lines_to_add = []
        for k, v in lang_keys.items():
            safe_v = v.replace('"', '\\"')
            lines_to_add.append(f'        setIfNotExists("{k}", "{safe_v}");\n')
        
        new_lang = lang_content[:insert_idx] + "".join(lines_to_add) + "\n    " + lang_content[insert_idx:]
        with open(lang_file, 'w', encoding='utf-8') as f:
            f.write(new_lang)
            
    yml_files = glob.glob('src/main/resources/lang/*.yml')
    for yf in yml_files:
        # Check if file has a trailing newline
        with open(yf, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            needs_newline = len(lines) > 0 and not lines[-1].endswith('\n')
            
        with open(yf, 'a', encoding='utf-8') as f:
            if needs_newline:
                f.write("\n")
            f.write("\n")
            for k, v in lang_keys.items():
                safe_v = v.replace('"', '\\"')
                f.write(f'"{k}": "{safe_v}"\n')

if __name__ == "__main__":
    main()
