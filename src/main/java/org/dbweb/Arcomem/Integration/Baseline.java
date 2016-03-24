package org.dbweb.Arcomem.Integration;

public enum Baseline {

  TEXTUAL_SOCIAL,   // we run with alpha = 1 (textual), then with right alpha
  TOPK_MERGE,       // Get top-k with alpha = 1, alpha = 0, then merge lists
  AUTOCOMPLETION,   // No prefix, an auto-completion is extracted from query on which we run TOPKS

}