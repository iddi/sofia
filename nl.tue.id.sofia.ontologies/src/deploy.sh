#!/bin/bash

echo "Committing all files"
git add .
git ci -m "Auto deploying"
echo "Switching to GitHub Pages branch"
git co gh-pages
git co master UserInteractionEvents.owl
git add .
echo "Deploying"
git ci -m "Auto deploying"
git push origin gh-pages
echo "Switching back to master branch"
git co master
