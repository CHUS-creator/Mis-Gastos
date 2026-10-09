import re, sys, subprocess
xml_path = sys.argv[1]
wants = sys.argv[2:]
try:
    xml = open(xml_path, encoding='utf-8', errors='replace').read()
except Exception:
    sys.exit(1)
for m in re.finditer(r'<node[^>]*>', xml):
    node = m.group(0)
    desc = re.search(r'content-desc="([^"]*)"', node)
    text = re.search(r'text="([^"]*)"', node)
    vals = []
    if desc: vals.append(desc.group(1))
    if text: vals.append(text.group(1))
    if any(w in v for w in wants for v in vals):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        if b:
            x = (int(b.group(1)) + int(b.group(3))) // 2
            y = (int(b.group(2)) + int(b.group(4))) // 2
            subprocess.run(['adb', 'shell', 'input', 'tap', str(x), str(y)])
            sys.exit(0)
sys.exit(1)
