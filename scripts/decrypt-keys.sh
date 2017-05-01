#!/bin/sh

openssl aes-256-cbc -K $encrypted_53c898739c54_key -iv $encrypted_53c898739c54_iv -in travis-deploy-key.enc -out travis-deploy-key -d
chmod 600 travis-deploy-key;
cp travis-deploy-key ~/.ssh/id_rsa;
