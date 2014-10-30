package org.dbweb.completion.trie;

import java.util.ArrayList;
import java.util.Formattable;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class RadixTreeImpl implements RadixTree, Formattable {

	protected RadixTreeNode root;

	protected long size;

	/**
	 * Create a Radix Tree with only the default node root.
	 */
	public RadixTreeImpl() {
		root = new RadixTreeNode("");
		root.setKey("#");
		size = 0;
	}

	public float find(String key) {
		Visitor<Float> visitor = new VisitorImpl<Float>() {

			public void visit(String key, RadixTreeNode parent,
					RadixTreeNode node) {
				//if (node.isReal()) 
					result = node.getValue();
			}
		};

		visit('#'+key, visitor);

		return visitor.getResult();
	}

	/*
	 * (non-Javadoc)
	 * @see org.completion.trie.structure.RadixTree#delete(java.lang.String)
	 * NOT IMPLEMENTED WITH NEW MODIFICATIONS - WON'T WORK
	 */
	public boolean delete(String key) {
		Visitor<Boolean> visitor = new VisitorImpl<Boolean>(Boolean.FALSE) {
			public void visit(String key, RadixTreeNode parent,
					RadixTreeNode node) {
				result = node.isReal();

				// if it is a real node
				if (result) {
					// If there no children of the node we need to
					// delete it from the its parent children list
					if (node.getChildren().size() == 0) {
						Iterator<RadixTreeNode> it = parent.getChildren()
								.iterator();
						while (it.hasNext()) {
							if (it.next().getKey().equals(node.getKey())) {
								it.remove();
								break;
							}
						}

						// if parent is not real node and has only one child
						// then they need to be merged.
						if (parent.getChildren().size() == 1
						&& parent.isReal() == false) {
							mergeNodes(parent, parent.getChildren().get(0));
						}
					} else if (node.getChildren().size() == 1) {
						// we need to merge the only child of this node with
						// itself
						mergeNodes(node, node.getChildren().get(0));
					} else { // we jus need to mark the node as non real.
						node.setReal(false);
					}
				}
			}

			/**
			 * Merge a child into its parent node. Operation only valid if it is
			 * only child of the parent node and parent node is not a real node.
			 * 
			 * @param parent
			 *            The parent Node
			 * @param child
			 *            The child Node
			 */
			private void mergeNodes(RadixTreeNode parent,
					RadixTreeNode child) {
				parent.setKey(parent.getKey() + child.getKey());
				parent.setReal(child.isReal());
				parent.setValue(child.getValue());
				parent.setChildren(child.getChildren());
			}

		};

		visit(key, visitor);

		if(visitor.getResult()) {
			size--;
		}
		return visitor.getResult().booleanValue();
	}

	/*
	 * (non-Javadoc)
	 * @see ds.tree.RadixTree#insert(java.lang.String, java.lang.Object)
	 */
	public void insert(String key, float value) throws DuplicateKeyException {
		try {
			insert(key, root, value, key);
		} catch (DuplicateKeyException e) {
			// re-throw the exception with 'key' in the message
			throw new DuplicateKeyException("Duplicate key: '" + key + "'");
		}
		size++;
	}

	/**
	 * Recursively insert the key in the radix tree.
	 * 
	 * @param key The key to be inserted
	 * @param node The current node
	 * @param value The value associated with the key 
	 * @throws DuplicateKeyException If the key already exists in the database.
	 */
	private void insert(String key, RadixTreeNode node, float value, String word)
			throws DuplicateKeyException {

		int numberOfMatchingCharacters = node.getNumberOfMatchingCharacters(key);

		// we are either at the root node
		// or we need to go down the tree
		if (node.getKey().equals("#") == true || numberOfMatchingCharacters == 0 || (numberOfMatchingCharacters < key.length() && numberOfMatchingCharacters >= node.getKey().length())) {
			boolean flag = false;
			String newText = key.substring(numberOfMatchingCharacters, key.length());
			for (RadixTreeNode child : node.getChildren()) {
				if (child.getKey().startsWith(newText.charAt(0) + "")) {
					flag = true;
					insert(newText, child, value, word);
					break;
				}
			}

			// just add the node as the child of the current node
			if (flag == false) {
				// the best match is a leaf => we create a leaf for this node
				if (node.getChildren().isEmpty() && node.isReal()) {
					RadixTreeNode n = new RadixTreeNode(node.getWord());
					n.setKey("");
					n.setReal(true);
					n.setValue(node.getValue());
					node.setReal(false);
					node.insertChildWithRespectToAncestors(n);
				}
				RadixTreeNode n = new RadixTreeNode(word);
				n.setKey(newText);
				n.setReal(true);
				n.setValue(value);

				node.insertChildWithRespectToAncestors(n);
			}
		}
		// there is an exact match just make the current node as data node
		else if (numberOfMatchingCharacters == key.length() && numberOfMatchingCharacters == node.getKey().length()) {
			if (node.isReal() == true) {
				throw new DuplicateKeyException("Duplicate key");
			}
			RadixTreeNode n = new RadixTreeNode(word);
			n.setKey("");
			n.setReal(true);
			n.setValue(value);
			node.insertChildWithRespectToAncestors(n);
		}
		// This node needs to be split as the key to be inserted
		// is a prefix of the current node key
		else if (numberOfMatchingCharacters > 0 && numberOfMatchingCharacters < node.getKey().length()) {
			RadixTreeNode n1 = new RadixTreeNode(node.getWord());
			String subs = node.getKey().substring(numberOfMatchingCharacters, node.getKey().length());

			n1.setKey(node.getKey().substring(numberOfMatchingCharacters, node.getKey().length()));
			n1.setReal(node.isReal());
			n1.setValue(node.getValue());
			n1.setChildren(node.getChildren());
			n1.setBestDescendant(node.getBestDescendant());

			node.setKey(key.substring(0, numberOfMatchingCharacters));
			node.setReal(false);
			node.setChildren(new SortedArrayList<RadixTreeNode>());
			node.setWord(greatestCommonPrefix(node.getWord(), word));
			node.insertChildWithRespectToAncestors(n1);

			RadixTreeNode n2 = new RadixTreeNode(word);
			n2.setKey(key.substring(numberOfMatchingCharacters, key.length()));
			n2.setReal(true);
			n2.setValue(value);
			node.insertChildWithRespectToAncestors(n2);
		}        
		// this key needs to be added as the child of the current node
		else {
			System.out.println("I DUNNO IF IT CAN BE USED");
			RadixTreeNode n = new RadixTreeNode("USED?");
			n.setKey(node.getKey().substring(numberOfMatchingCharacters, node.getKey().length()));
			n.setChildren(node.getChildren());
			n.setReal(node.isReal());
			n.setValue(node.getValue());

			node.setKey(key);
			node.setReal(true);
			node.setValue(value);

			node.getChildren().insertSorted(n);
		}
	}

	public ArrayList<Float> searchPrefixList(String key, int recordLimit) {
		ArrayList<Float> keys = new ArrayList<Float>();

		RadixTreeNode node = searchPrefix(key, root);

		if (node != null) {
			if (node.isReal()) {
				keys.add(node.getValue());
			}
			getNodes(node, keys, recordLimit);
		}

		return keys;
	}

	private void getNodes(RadixTreeNode parent, ArrayList<Float> keys, int limit) {
		Queue<RadixTreeNode> queue = new LinkedList<RadixTreeNode>();

		queue.addAll(parent.getChildren());

		while (!queue.isEmpty()) {
			RadixTreeNode node = queue.remove();
			if (node.isReal() == true) {
				keys.add(node.getValue());
			}

			if (keys.size() == limit) {
				break;
			}

			queue.addAll(node.getChildren());
		}
	}

	public RadixTreeNode searchPrefix(String key) {
		return searchPrefix(this.root.getKey()+key, this.root);
	}

	private RadixTreeNode searchPrefix(String key, RadixTreeNode node) {
		RadixTreeNode result = null;

		int numberOfMatchingCharacters = node.getNumberOfMatchingCharacters(key);

		if (numberOfMatchingCharacters == key.length() && numberOfMatchingCharacters < node.getKey().length()) {
			result = node;
		} else if(numberOfMatchingCharacters == key.length() && numberOfMatchingCharacters == node.getKey().length()) {
			for (RadixTreeNode child: node.getChildren()) {
				if (child.getKey().equals("")) {
					result = child;
					break;
				}
			}
			if (result == null)
				result = node;
		} else if (node.getKey().equals("") == true || (numberOfMatchingCharacters < key.length() && numberOfMatchingCharacters >= node.getKey().length())) {
			String newText = key.substring(numberOfMatchingCharacters, key.length());
			for (RadixTreeNode child : node.getChildren()) {
				if (child.getKey().startsWith(newText.charAt(0) + "")) {
					result = searchPrefix(newText, child);
					break;
				}
			}
		}

		return result;
	}

	public boolean contains(String key) {
		Visitor<Boolean> visitor = new VisitorImpl<Boolean>(Boolean.FALSE) {
			public void visit(String key, RadixTreeNode parent,
					RadixTreeNode node) {
				result = node.isReal();
			}
		};

		visit(key, visitor);

		return visitor.getResult().booleanValue();
	}

	/**
	 * visit the node those key matches the given key
	 * @param key The key that need to be visited
	 * @param visitor The visitor object
	 */
	public <R> void visit(String key, Visitor<R> visitor) {
		if (root != null) {
			visit(key, visitor, null, root);
		}
	}

	/**
	 * recursively visit the tree based on the supplied "key". calls the Visitor
	 * for the node those key matches the given prefix
	 * 
	 * @param prefix
	 *            The key o prefix to search in the tree
	 * @param visitor
	 *            The Visitor that will be called if a node with "key" as its
	 *            key is found
	 * @param node
	 *            The Node from where onward to search
	 */
	private <R> void visit(String prefix, Visitor<R> visitor,
			RadixTreeNode parent, RadixTreeNode node) {

		int numberOfMatchingCharacters = node.getNumberOfMatchingCharacters(prefix);

		// if the node key and prefix match, we found a match!
		if (numberOfMatchingCharacters == prefix.length() && numberOfMatchingCharacters == node.getKey().length()) {
			visitor.visit(prefix, parent, node);
		} else if (node.getKey().equals("") == true // either we are at the
				// root
				|| (numberOfMatchingCharacters < prefix.length() && numberOfMatchingCharacters >= node.getKey().length())) { // OR we need to
			// traverse the children
			String newText = prefix.substring(numberOfMatchingCharacters, prefix.length());
			for (RadixTreeNode child : node.getChildren()) {
				// recursively search the child nodes
				if (child.getKey().startsWith(newText.charAt(0) + "")) {
					visit(newText, visitor, node, child);
					break;
				}
			}
		}
	}

	public long getSize() {
		return size;
	}

	/**
	 * Display the Trie on console.
	 * 
	 * WARNING! Do not use this for a large Trie, it's for testing purpose only.
	 * @see formatTo
	 */
	@Deprecated
	public void display() {
		formatNodeTo(new Formatter(System.out), 0, root);
	}

	@Deprecated
	private void display(int level, RadixTreeNode node) {
		formatNodeTo(new Formatter(System.out), level, node);
	}

	/**
	 * WARNING! Do not use this for a large Trie, it's for testing purpose only.
	 */
	private void formatNodeTo(Formatter f, int level, RadixTreeNode node) {
		for (int i = 0; i < level; i++) {
			f.format(" ");
		}
		f.format("|");
		for (int i = 0; i < level; i++) {
			f.format("-");
		}

		if (node.isReal() == true)
			f.format("%s[%s]*%n", node.getKey()+"##"+node.getWord(),  node.getValue());
		else
			f.format("%s%n", node.getKey()+"##"+node.getWord());

		for (RadixTreeNode child : node.getChildren()) {
			formatNodeTo(f, level + 1, child);
		}		
	}

	/**
	 * Writes a textual representation of this tree to the given formatter.
	 * 
	 * Currently, all options are simply ignored.
	 * 
	 * WARNING! Do not use this for a large Trie, it's for testing purpose only.
	 */
	public void formatTo(Formatter formatter, int flags, int width, int precision) {
		formatNodeTo(formatter, 0, root);	
	}

	/**
	 * Complete the a prefix to the point where ambiguity starts.
	 * 
	 *  Example:
	 *  If a tree contain "blah1", "blah2"
	 *  complete("b") -> return "blah"
	 * 
	 * @param prefix The prefix we want to complete
	 * @return The unambiguous completion of the string.
	 */
	public String complete(String prefix) {
		return complete(prefix, root, "");
	}    

	private String complete(String key, RadixTreeNode node, String base) {
		int i = 0;
		int keylen = key.length();
		int nodelen = node.getKey().length();

		while (i < keylen && i < nodelen) {
			if (key.charAt(i) != node.getKey().charAt(i)) {
				break;
			}
			i++;
		}

		if (i == keylen && i <= nodelen) {
			return base + node.getKey();
		}
		else if (nodelen == 0 || (i < keylen && i >= nodelen)) {
			String beginning = key.substring(0, i);
			String ending = key.substring(i, keylen);
			for (RadixTreeNode child : node.getChildren()) {
				if (child.getKey().startsWith(ending.charAt(0) + "")) {
					return complete(ending, child, base + beginning);
				}
			}
		}

		return "";
	}
	
	public String greatestCommonPrefix(String a, String b) {
	    int minLength = Math.min(a.length(), b.length());
	    for (int i = 0; i < minLength; i++) {
	        if (a.charAt(i) != b.charAt(i)) {
	            return a.substring(0, i);
	        }
	    }
	    return a.substring(0, minLength);
	}
}