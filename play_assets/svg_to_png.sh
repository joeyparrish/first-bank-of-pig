#!/bin/bash

cd "$(dirname "$0")"

inkscape app_icon.svg --export-filename=app_icon.png --export-width=512 --export-height=512
