#!/bin/bash

set +ex

export PATH=$PATH:/bin/pip3
export WORKON_HOME=$PWD/.virtualenvs
export PROJECT_HOME=$PWD/.virtualenvs
export VIRTUALENVWRAPPER_PYTHON=/bin/python3.4
export VIRTUALENVWRAPPER_VIRTUALENV=/usr/local/bin/virtualenv
export VIRTUALENVWRAPPER_VIRTUALENV_ARGS='--no-site-packages'
mkvirtualenv myenv --python=python3.4
workon myenv

set -ex