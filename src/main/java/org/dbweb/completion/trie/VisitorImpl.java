package org.dbweb.completion.trie;

import org.dbweb.completion.trie.RadixTreeNode;
import org.dbweb.completion.trie.Visitor;

public abstract class VisitorImpl<R> implements Visitor<R> {

	protected R result;


	public VisitorImpl() {
		this.result = null;
	}

	public VisitorImpl(R initialValue) {
		this.result = initialValue;
	}

	public R getResult() {
		return result;
	}

	abstract public void visit(String key, RadixTreeNode parent, RadixTreeNode node);

}