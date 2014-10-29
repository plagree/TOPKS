package org.dbweb.completion.trie;

import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.completion.trie.RadixTreeNode;
import org.dbweb.completion.trie.Visitor;

public interface Visitor<R> {
	/**
	 * This method gets called by {@link RadixTreeImpl#visit(String, Visitor) visit} 
	 * when it finds a node matching the key given to it.
	 * 
	 * @param key The key that matched the node
	 * @param parent The parent of the node being visited
	 * @param node The node that is being visited
	 */
	public void visit(String key, RadixTreeNode parent, RadixTreeNode node);

	/**
	 * The visitor can store any type of result object, depending on the context of 
	 * what it is being used for.
	 * 
	 * @return The result captured by the visitor.
	 */
	public R getResult();
}
