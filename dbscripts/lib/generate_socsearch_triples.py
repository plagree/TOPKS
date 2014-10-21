#!/usr/bin/env python

import sys
import json

if __name__=='__main__':
    for line in sys.stdin:
        user_id = ''
        rt_tweet = ''
        hashtags = []
        tweet_seq = []	

	try:
          tweet_seq = json.loads(line)
	  #print json.dumps(tweet_seq['statuses'])
	  #exit(-1)
	  tweet_seq = tweet_seq['statuses']
        except:
          tweet_seq = []
	#print len(tweet_seq)

	if len(tweet_seq) >0:
		for idx in range(1,len(tweet_seq)):
			
			try:
			   retweet = tweet_seq[idx]
			except:
			   retweet = {"error":'true'}

			try:
			    user_id = retweet['user']['id_str']
			except:
			    user_id = ''
			try:
			    hashtags = retweet['retweeted_status']['entities']['hashtags']
			except:
			    hashtags = []
			try:
			    rt_tweet = retweet['retweeted_status']['id']
			except:
			    rt_tweet = ''
			
			#print json.dumps(retweet)
			
			if user_id!='' and rt_tweet!='':
			    for htag in hashtags:
				try:
				    print '%s\t%s\t%s' % (user_id, rt_tweet, htag['text'].decode('utf-8'))
				except:
				    l=0
			
