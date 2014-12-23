#!/usr/bin/env python
#-*-coding: utf-8 -*-

import sys
import operator
import re
from nltk.tokenize import word_tokenize
from nltk.corpus import stopwords

NB_EXPEND_MAX = 5

minlength = 2
stopwordsEN = stopwords.words('english')
stopwordsES = stopwords.words('spanish')


def nlp_cleaning(text, hashtags):
    text = text.lower()
    hashtags = [tag for tag in text.split() if tag.startswith("#")]
    text = re.sub(r'@(\w+)', ' ', text)
    for hashtag in hashtags:
        text = text.replace(hashtag, " ")
    text = re.sub(r'(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:\'".,<>?«»“”‘’]))', '', text)
    text = text.replace('?', ' ')
    text = text.replace('.', ' ')
    text = text.replace('!', ' ')
    text = text.replace("'", '')
    text = text.replace('"', '')
    text = text.replace('#', '')
    text = text.replace('@', '')
    tokens = word_tokenize(text)
    tokens = [k for k in tokens if (not k in stopwordsEN) and
              (not k in stopwordsES) and len(k) > minlength]
    return tokens

if __name__=='__main__':
    if (len(sys.argv)!=3):
        print "Problem of args: python expended.py input.txt output.txt"
        sys.exit(0)
    file_to_read = sys.argv[1]
    file_to_write = sys.argv[2]
    user_spaces = {}
    #with open(file_good_users, 'r') as myFile:
    #    for l in myFile:
    #        l = l.rstrip('\n')
    #        if l not in user_spaces:
    #            user_spaces[l] = {}
    with open(file_to_read, 'r') as f:
        nb_lines = 0
        for l in f:
            nb_lines += 1
            if nb_lines%10000==0:
                print nb_lines
            l = l.rstrip('\n')
            data = l.split('\t')
            if len(data)!=6:
                print "Problem"
            user = data[0]
            item = data[1]
            hashtags = data[3].split(',')
            text = data[2]
            words = nlp_cleaning(text, hashtags)
            if user not in user_spaces:
                user_spaces[user] = {}
            us = user_spaces[user]
            for tag in hashtags:
                if tag not in us:
                    us[tag] = {}
                for w in words:
                    if w not in us[tag]:
                        us[tag][w] = 0
                    us[tag][w] += 1
    for u in user_spaces:
        us = user_spaces[u]
        for tag in us:
            u_t_words = us[tag]
            liste = sorted(u_t_words.items(), key=operator.itemgetter(1), reverse=True)
            counter = 0
            for w in liste:
                counter += 1
                if counter > NB_EXPEND_MAX:
                    u_t_words.pop(w[0])
    with open(file_to_write, 'a') as f2:
        with open(file_to_read, 'r') as f:
            n = 0
            for l in f:
                try:
                    l = l.rstrip('\n')
                    data = l.split('\t')
                    if len(data)!=6:
                        print "Problem"
                    user = data[0]
                    if user not in user_spaces:
                        continue
                    item = data[1]
                    hashtags = data[3].split(',')
                    for tag in hashtags:
                        for w in user_spaces[user][tag]:
                            f2.write(user+'\t'+item+'\t'+w+'\n')
                except Exception:
                    continue
