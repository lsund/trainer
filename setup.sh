#!/bin/zsh

CSS_DIR=resources/public/css/

echo "Downloading MUI CSS"

curl -X GET https://cdn.muicss.com/mui-0.9.39.zip -o mui.zip

echo "Installing mui in $CSS_DIR"

unzip mui.zip
cp mui-0.9.39/css/mui.css $CSS_DIR

echo "Cleaning up"

trash mui.zip mui-0.9.39

echo -e "\n\n\n"

echo "If you want this to work, you need to:"
echo "1. Change the name 'template' to the name of your program,"
echo "   everywhere in your project. I reccomend ag"
echo "2. specify the database configuration under [:db :name]"
