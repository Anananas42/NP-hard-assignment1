import os
import re

# Manually specify files or directories here
sources = ['./',]

# Specify paths to exclude, using absolute paths for reliability
exclusions = [
    os.path.abspath('./tests/'),  # Convert to absolute path
    os.path.abspath('./WeblabOutput.java'),  # Convert to absolute path
]

def process_source(source, exclusions):
    abs_source = os.path.abspath(source)
    if os.path.isfile(abs_source) and all(not abs_source.startswith(excl) for excl in exclusions):
        return [abs_source]
    elif os.path.isdir(abs_source):
        files = []
        for root, _, filenames in os.walk(abs_source):
            abs_root = os.path.abspath(root)
            if any(abs_root.startswith(excl) for excl in exclusions):
                continue
            files.extend(os.path.join(root, f) for f in filenames if f.endswith('.java'))
        return files
    return []

def process_java_sources(sources, exclusions):
    import_pattern = re.compile(r'import\s+(.+?);')
    valid_import_pattern = re.compile(r'import\s+(game.*?|java\.util\.\*);')
    package_pattern = re.compile(r'^package\s+.+?;\n?', re.MULTILINE)
    class_decl_pattern = re.compile(r'(public\s+|protected\s+|private\s+)?(abstract\s+|static\s+|final\s+)?(class|interface|enum)\s+(\w+)')

    all_imports = set()
    all_names = {}
    combined_content = ''

    # First pass: Collect class/interface/enum names
    for source in sources:
        files = process_source(source, exclusions)
        for file_path in files:
            with open(file_path, 'r') as file:
                content = file.read()

                # Find all class/interface/enum names to be renamed
                for match in class_decl_pattern.findall(content):
                    original_name = match[3]
                    new_name = f'{original_name}NonWeblab'
                    all_names[original_name] = new_name

                # Extract and conditionally accumulate valid imports
                imports = valid_import_pattern.findall(content)
                all_imports.update(imports)
                    

    combined_content = ''

    # Second pass: Replace names and remove modifiers
    for source in sources:
        files = process_source(source, exclusions)
        for file_path in files:
            with open(file_path, 'r') as file:
                content = file.read()

                # Remove package declarations and all imports
                content = package_pattern.sub('', content)
                content = import_pattern.sub('', content)

                def replacer(match):
                    matched_string = match.group(0)  # The entire match
                    return all_names.get(matched_string, matched_string)

                content = re.compile(r'\b[A-Za-z_]\w*\b').sub(replacer, content)

                # Remove modifiers
                def removeModifiers(match):
                    modifier, abstract_keyword, type_keyword, name = match.groups()
                    if abstract_keyword != None:
                        return f'{abstract_keyword} {type_keyword} {name}'
                    return f'{type_keyword} {name}'

                content = class_decl_pattern.sub(removeModifiers, content)

                # Ensure only two newlines before appending new content
                combined_content += '\n\n' + content.strip()


    # Prepare final content with filtered imports at the beginning
    final_imports = '\n'.join(f'import {imp};' for imp in sorted(all_imports))
    final_content = final_imports + '\n\n' + combined_content.lstrip('\n')

    # Output the final combined Java file
    output_path = 'WeblabOutput.java'
    with open(output_path, 'w') as output_file:
        output_file.write(final_content)
    print(f'Combined Java file created at: {output_path}')

# Example usage
process_java_sources(sources, exclusions)

