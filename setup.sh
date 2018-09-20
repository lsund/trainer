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

projectname=$(echo $(basename $(pwd)))

echo "Renaming src/{clj,cljs}/template to $projectname"

mv src/clj/template src/clj/$projectname
mv src/cljs/template src/cljs/$projectname

echo "Changing git remote url"

sed -i -e "s/component-template.git/$projectname.git/" .git/config
