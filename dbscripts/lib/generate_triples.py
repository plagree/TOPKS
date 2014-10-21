#!/usr/bin/env python

import sys
import json

if __name__=='__main__':
    for line in sys.stdin:
        try:
            tweet = json.loads(line)
        except:
            tweet = {}
        user_id = ''
        rt_tweet = ''
        hashtags = []
        try:
            user_id = tweet['user_id']
        except:
            user_id = ''
        try:
            hashtags = tweet['entities']['hashtags']
        except:
            hashtags = []
        try:
            rt_tweet = tweet['original_tweet_id']
        except:
            rt_tweet = ''

        if user_id!='' and rt_tweet!='':
            for htag in hashtags:
                try:
                    print '%s\t%s\t%s' % (user_id, rt_tweet, htag['text'].decode('utf-8'))
                except:
                    l=0
