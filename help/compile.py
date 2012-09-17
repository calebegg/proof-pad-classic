import os
import shutil

from django.template.loader import render_to_string
import django.conf

ROOT = os.path.abspath(os.path.dirname(__file__))
OSES = ['osx', 'win', 'linux']

def main():
    django.conf.settings.configure(
            TEMPLATE_DIRS=[os.path.join(ROOT, 'templates')])
    for os_name in OSES:
        dirname = os_name
        path = os.path.join(ROOT, dirname)
        if os_name == 'osx':
            dirname = 'Proof Pad.help'
            path = os.path.join(ROOT, dirname, 'Contents', 'Resources',
                    'English.lproj')
        try:
            shutil.rmtree(os.path.join(ROOT, dirname))
        except OSError:
            pass
        if os_name == 'osx':
            shutil.copytree(os.path.join(ROOT, 'templates', 'osx'),
                    os.path.join(ROOT, 'Proof Pad.help'))
        os.makedirs(path)
        for fn in os.listdir(os.path.join(ROOT, 'templates')):
            if fn in OSES or fn.startswith('.'):
                continue
            elif fn.endswith('.html'):
                with open(os.path.join(path, fn), 'w') as f:
                    f.write(render_to_string(fn, {'os': os_name,
                        'is_osx': os_name == 'osx',
                        'is_win': os_name == 'win',
                        'is_linux': os_name == 'linux'}))
            else:
                shutil.copy(fn, path)

if __name__ == '__main__':
    main()
