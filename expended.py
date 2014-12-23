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


def nlp_cleaning(text):
    text = text.lower()
    #hashtags = [tag for tag in text.split() if tag.startswith("#")]
    text = re.sub(r'@(\w+)', ' ', text)
    #for hashtag in hashtags:
    #    text = text.replace(hashtag, " ")
    text = re.sub(r'(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:\'".,<>?«»“”‘’]))', '', text)
    text = text.replace('?', ' ')
    text = text.replace('.', ' ')
    text = text.replace('!', ' ')
    text = text.replace("'", '')
    text = text.replace('"', '')
    #text = text.replace('#', '')
    text = text.replace('@', '')
    hashtags = [tag for tag in text.split() if tag.startswith("#")]
    #text = text.replace('#', '')
    tokens = word_tokenize(text)
    tokens = [k for k in tokens if (not k in stopwordsEN) and
              (not k in stopwordsES) and (not k in hashtags) and (len(k) > minlength)]
    print "Hashtags: ", str(hashtags)
    print "Expended: ", str(tokens)
    return tokens

if __name__=='__main__':
    if (len(sys.argv)!=3):
        print "Problem of args: python expended.py input.txt output.txt"
        sys.exit(0)
    file_to_read = sys.argv[1]
    file_to_write = sys.argv[2]
    i = 0
    with open(file_to_read, "r") as f:
        for line in f:
            l = l.rstrip('\n')
            data = l.split('\t')
            i +=1
            if i> 10:
                sys.exit(1)
            nlp_cleaning(data[2])
