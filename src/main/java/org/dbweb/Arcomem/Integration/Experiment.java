package org.dbweb.Arcomem.Integration;

public enum Experiment {
  NDCG_TIME,
  NDCG_USERS,
  NDCG_DISK_ACCESS,   // Budget associated to the number of disk accesses
  EXACT_TOPK,
  SUPERNODE,          // Supernode algorithm
  DEFAULT
}