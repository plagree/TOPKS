# Script for generating plots

import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
import sys


# VARIABLES TO BE CHANGED
precisions = [2, 5, 20, 75, 200, 500]
ALPHA=0.0
TIME=3
THRESHOLD=0.2

# OTHER STUFF
parameters = ['t', 'l', 'alpha', 'theta']
columns = parameters + ['ranking']


# PLOT TIME LIMIT EFFECTS
def plot_t(f, A, matrix, times, lengths):
    for k in precisions:
        P = matrix[k]
        # TIME EVOLUTIONS
        alpha = ALPHA
        theta = THRESHOLD
        fig = plt.figure()
        for t in times:
            res = []
            for l in lengths:
                res.append(P[(t,l,alpha,theta)])
            plt.plot(lengths, res, label="t="+str(t))
        plt.xlabel('Prefix length l')
        plt.ylabel('Precision '+str(k))
        plt.legend()
        plt.savefig('./plots/'+f.rstrip('.txt')+'-time-precision-'+str(k)+'.eps')
        plt.close(fig)

# PLOTS ALPHA EFFECTS
def plot_alpha(f, A, matrix, alphas, lengths):
    for k in precisions:
        P = matrix[k]
        # TIME EVOLUTIONS
        t = TIME
        theta = THRESHOLD
        fig = plt.figure()
        for alpha in alphas:
            res = []
            for l in lengths:
                res.append(P[(t,l,alpha,theta)])
            plt.plot(lengths, res, label="alpha="+str(alpha))
        plt.xlabel('Prefix length l')
        plt.ylabel('Precision '+str(k))
        plt.legend()
        plt.savefig('./plots/'+f.rstrip('.txt')+'-alpha-precision-'+str(k)+'.eps')
        plt.close(fig)

# PLOT THETA EFFECTS
def plot_threshold(f, A, matrix, thresholds, lengths):
    for k in precisions:
        P = matrix[k]
        # TIME EVOLUTIONS
        t = TIME
        alpha = ALPHA
        fig = plt.figure()
        for theta in thresholds:
            res = []
            for l in lengths:
                res.append(P[(t,l,alpha,theta)])
            plt.plot(lengths, res, label="theta="+str(theta))
        plt.xlabel('Prefix length l')
        plt.ylabel('Precision '+str(k))
        plt.legend()
        plt.savefig('./plots/'+f.rstrip('.txt')+'-theta-precision-'+str(k)+'-theta-'+str(k)+'.eps')
        plt.close(fig)

# Script to generate the test input file

def datafixed(f):
    oldf = f
    name = f.rstrip('.txt')+'-fixed'+'.txt'
    with open(oldf, 'r') as f:
        maximum = 0
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            if len(res) != 10:
                print 'ok'
                continue
            if maximum < int(res[5]):
                maximum = int(res[5])
    print str(maximum)
    with open(name, 'w') as newFile:
        with open(oldf, 'r') as f:
            currU = -1
            currI = -1
            currTag = -1
            currFreq = -1
            currTime = -1
            currL = -1
            currAlpha = -1
            currThres = -1
            currRanking = -1
            #currNbWords = -1
            for line in f:
                line = line.rstrip('\n')
                res = line.split('\t')
                if len(res) != 10:
                    print 'ok2'
                    continue
                if ((currU!=res[0]) or currTime!=res[4]) and (currU!=-1) and (currAlpha==str(ALPHA)):# and (currNbWords=='1'):
                    for i in range(int(currL)+1, maximum+1):
                        newFile.write(currU+'\t'+currI+'\t'+currTag+'\t'+currFreq+'\t'+currTime+'\t'+str(i)+'\t'+currAlpha+'\t'+currThres+'\t'+currRanking+'\n')#'\t'+currNbWords+'\n')
                if len(res)!=10:
                    print "ok"
                    continue
                currU = res[0]
                currI = res[1]
                currTag = res[2]
                currFreq = res[3]
                currTime = res[4]
                currL = res[5]
                currAlpha = res[6]
                currThres = res[7]
                currRanking = res[8]
                #currNbWords = res[9]
                if (currAlpha==str(ALPHA)):# and (currNbWords=='1')):
                    newFile.write(currU+'\t'+currI+'\t'+currTag+'\t'+currFreq+'\t'+currTime+'\t'+currL+'\t'+currAlpha+'\t'+currThres+'\t'+currRanking+'\n')#'\t'+currNbWords+'\n')
            if (currAlpha==str(ALPHA)):# and (currNbWords=='1'):
                for i in range(int(currL)+1, maximum+1):
                    newFile.write(currU+'\t'+currI+'\t'+currTag+'\t'+currFreq+'\t'+currTime+'\t'+str(i)+'\t'+currAlpha+'\t'+currThres+'\t'+currRanking+'\n')#'\t'+currNbWords+'\n')
    return name

if __name__=='__main__':
    if len(sys.argv)<=1:
        sys.exit(0)
    files = [arg for arg in sys.argv[1:]]
    files_fixed = []
    for f in files:
        A = pd.read_csv(f, sep='\t', names=['user', 'item', 'tag', 'freq', 't', 'l', 'alpha', 'theta', 'ranking', 'nbWords'])
        A = A.loc[A['nbWords']==1]
        A.to_csv('fixed1.txt',sep='\t',header=False,index=False)
        files_fixed.append(datafixed('fixed1.txt'))
    for f in files_fixed:
        A = pd.read_csv(f, sep='\t', names=['user', 'item', 'tag', 'freq', 't', 'l', 'alpha', 'theta', 'ranking'])
        A = A.dropna()
        # Matrix generation
        g=A.groupby(parameters)
        matrixKeys = g.groups.viewkeys()
 
        matrix = {}
        for k in precisions:
            matrix[k] = {}
            for key in matrixKeys:
                DF = g.get_group(key)
                DF = DF[["ranking"]]
                DF = DF.replace(0, np.nan)
                val = float(len(DF[(DF.ranking<k)]))/len(DF)
                matrix[k][key]=val
 
        # TIMES
        times = []
        g_times = A.groupby('t')
        keys = g_times.groups.viewkeys()
        for t in keys:
            times.append(int(t))
        times = sorted(times)
        print str(times)
        # ALPHAS
        alphas = []
        g_alphas = A.groupby('alpha')
        keys = g_alphas.groups.viewkeys()
        for alpha in keys:
            alphas.append(float(alpha))
        alphas = sorted(alphas)
        print str(alphas)
        # THRESHOLDS
        thresholds = []
        g_thresholds = A.groupby('theta')
        keys = g_thresholds.groups.viewkeys()
        for theta in keys:
            thresholds.append(float(theta))
        thresholds = sorted(thresholds)
        print str(thresholds)
        THRESHOLD = min(thresholds)
        # LENGTHS
        lengths = []
        g_l = A.groupby('l')
        keys = g_l.groups.viewkeys()
        for l in keys:
            if (int(l) > 12):
                continue
            lengths.append(int(l))
        lengths = sorted(lengths)
        
        # PLOTS HERE
        plot_t(f, A, matrix, times, lengths)
        plot_alpha(f, A, matrix, alphas, lengths)
        plot_threshold(f, A, matrix, thresholds, lengths)
