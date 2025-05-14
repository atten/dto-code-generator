import os
import subprocess
import sys

abspath = os.path.abspath(__file__)
current_dir = os.path.dirname(abspath)
script_path = os.path.join(current_dir, 'bin', 'ez-codegen')
subprocess.call([script_path] + sys.argv[1:])
