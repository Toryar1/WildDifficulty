import re
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
    return f"gui.msg.{key}"

def process_file(file_path, lang_keys):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    def replacer(match):
        original = match.group(1) # The string literal "..."
        text = match.group(2) # The content without quotes
        
        # Skip technical / empty strings
        if not text.strip() or len(text.strip()) < 2: return match.group(0)
        if not re.search(r'[a-zA-ZÀ-ÿ]', text): return match.group(0)
        
        # Skip if already using LangManager (though regex should only match pure string literals anyway)
        if "gui." in text or "wd_" in text or ".yml" in text: return match.group(0)
        
        key = generate_key(text)
        
        original_key = key
        counter = 1
        while key in lang_keys and lang_keys[key] != text:
            key = f"{original_key}_{counter}"
            counter += 1
            
        lang_keys[key] = text
        
        # Replace the original string literal with the method call
        # the entire match is something like `sendMessage("foo")`
        # We only want to replace `"foo"`
        replacement = f'plugin.getLangManager().getRaw("{key}")'
        return match.group(0).replace(original, replacement)

    # 1. ChatPromptUtil.prompt(..., "...")
    # match ChatPromptUtil.prompt(arg1, arg2, "string")
    prompt_pattern = r'ChatPromptUtil\.prompt\([^,]+,\s*[^,]+,\s*("([^"\\]+)")'
    new_content = re.sub(prompt_pattern, replacer, content)
    
    # 2. player.sendMessage("...")
    # match .sendMessage("string")
    msg_pattern = r'sendMessage\(\s*("([^"\\]+)")\s*\)'
    new_content = re.sub(msg_pattern, replacer, new_content)
    
    # Write if modified
    if content != new_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

def main():
    files = [
        'src/main/java/fr/wilddifficulty/listener/GuiListener.java',
        'src/main/java/fr/wilddifficulty/commands/WDGuiCommand.java',
        'src/main/java/fr/wilddifficulty/listener/ZoneToolListener.java',
        'src/main/java/fr/wilddifficulty/listener/SpawnerToolListener.java',
        'src/main/java/fr/wilddifficulty/listener/InspectorToolListener.java'
    ]
    
    lang_keys = {}
    modified_count = 0
    
    for f in files:
        if process_file(f, lang_keys):
            modified_count += 1
            
    print(f"Modified {modified_count} files, extracted {len(lang_keys)} unique strings.")
    
    if len(lang_keys) > 0:
        # Update LangManager.java
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
                
        # Update all .yml files
        yml_files = glob.glob('src/main/resources/lang/*.yml')
        for yf in yml_files:
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
