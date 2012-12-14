import os
import shutil

from markdown import markdown
from django.template.loader import render_to_string
from django.template import Context, Template
import django.conf

ROOT = os.path.abspath(os.path.dirname(__file__))
OSES = ['mac', 'win', 'linux']

def main():
    django.conf.settings.configure(
            TEMPLATE_DIRS=[os.path.join(ROOT, 'templates')])
    for os_name in OSES:
        dirname = os_name
        path = os.path.join(ROOT, dirname)
        try:
            shutil.rmtree(os.path.join(ROOT, dirname))
        except OSError:
            pass
        os.makedirs(path)
        for fn in os.listdir(os.path.join(ROOT, 'templates')):
            if fn in OSES or fn.startswith('.'):
                continue
            elif fn.endswith('.md'):
                newfn = fn[:-3] + '.html'
                with open(os.path.join(ROOT, 'templates', fn), 'r') as f:
                    source = markdown(f.read())
                with open(os.path.join(path, newfn), 'w') as f:
                    t = Template(source)
                    f.write('<link href="style.css" rel="stylesheet" />')
                    f.write(t.render(Context({'os': os_name,
                        'is_mac': os_name == 'mac',
                        'is_win': os_name == 'win',
                        'is_linux': os_name == 'linux'})))
            elif fn.endswith('.html'):
                with open(os.path.join(path, fn), 'w') as f:
                    f.write(render_to_string(fn, {'os': os_name,
                        'is_mac': os_name == 'mac',
                        'is_win': os_name == 'win',
                        'is_linux': os_name == 'linux'}))
            else:
                shutil.copy(os.path.join(ROOT, 'templates', fn), path)

if __name__ == '__main__':
    main()
