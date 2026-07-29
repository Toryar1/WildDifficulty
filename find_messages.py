import re
import os

def find_hardcoded_messages():
    files = [
        'src/main/java/fr/wilddifficulty/listener/GuiListener.java',
        'src/main/java/fr/wilddifficulty/commands/WDGuiCommand.java',
        'src/main/java/fr/wilddifficulty/listener/ZoneToolListener.java',
        'src/main/java/fr/wilddifficulty/listener/SpawnerToolListener.java',
        'src/main/java/fr/wilddifficulty/listener/InspectorToolListener.java'
    ]
    
    for java_file in files:
        if not os.path.exists(java_file): continue
        with open(java_file, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        for i, line in enumerate(lines):
            # Check for sendMessage("...")
            m = re.search(r'sendMessage\(\s*("[^"\\]+")\s*\)', line)
            if m:
                print(f"{os.path.basename(java_file)}:{i+1} : {m.group(1)}")
                
            # Check for ChatPromptUtil.prompt(..., "...")
            m2 = re.search(r'ChatPromptUtil\.prompt\([^,]+,\s*[^,]+,\s*("[^"\\]+")', line)
            if m2:
                print(f"{os.path.basename(java_file)}:{i+1} : {m2.group(1)}")

if __name__ == "__main__":
    find_hardcoded_messages()
