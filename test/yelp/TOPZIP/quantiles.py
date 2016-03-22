#!/usr/bin/env python
#-*-coding: utf-8 -*-

import sys
import pandas as pd

if __name__=='__main__':
    f = sys.argv[1]
    A = pd.read_csv(f, sep='\t', names=['u1', 'u2', 'w'])
    print A.quantile([0., 1./3, 2./3])
