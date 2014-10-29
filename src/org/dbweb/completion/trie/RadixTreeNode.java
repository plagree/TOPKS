package org.dbweb.completion.trie;

import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.completion.trie.RadixTreeNode;
import org.dbweb.completion.trie.SortedArrayList;

/**
 * Represents a node of a Radix tree {@link RadixTreeImpl}
 * 
 * @author Tahseen Ur Rehman
 * @email tahseen.ur.rehman {at.spam.me.not} gmail.com
 */
public class RadixTreeNode implements Comparable {
	private String key;
	private SortedArrayList<RadixTreeNode> children; //To order by score
	private RadixTreeNode parent;
	private boolean real;
	private float value;
	private RadixTreeNode bestDescendant;

	/**
	 * initialize the fields with default values to avoid null reference checks
	 * all over the places
	 */
	public RadixTreeNode() {
		key = "";
		children = new SortedArrayList<RadixTreeNode>();
		real = false;
		parent = null;
		value = 0;
		bestDescendant = this;
	}

	public float getValue() {
		return value;
	}

	public void setValue(float data) {
		this.value = data;
	}

	public RadixTreeNode getBestDescendant() {
		return bestDescendant;
	}

	public void setBestDescendant(RadixTreeNode bd) {
		this.bestDescendant = bd;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String value) {
		this.key = value;
	}

	public boolean isReal() {
		return real;
	}

	public void setReal(boolean datanode) {
		this.real = datanode;
	}

	public SortedArrayList<RadixTreeNode> getChildren() {
		return children;
	}

	public void setChildren(SortedArrayList<RadixTreeNode> children) {
		for (RadixTreeNode node: children)
			node.parent = this;
		this.children = children;
	}

	public RadixTreeNode getParent() {
		return this.parent;
	}

	public void setParent(RadixTreeNode newParent) {
		this.parent = newParent;
	}

	public int getNumberOfMatchingCharacters(String key) {
		int numberOfMatchingCharacters = 0;
		while (numberOfMatchingCharacters < key.length() && numberOfMatchingCharacters < this.getKey().length()) {
			if (key.charAt(numberOfMatchingCharacters) != this.getKey().charAt(numberOfMatchingCharacters)) {
				break;
			}
			numberOfMatchingCharacters++;
		}
		return numberOfMatchingCharacters;
	}

	@Override
	public String toString() {
		return key;
	}

	public int compareTo(Object anotherRadixTreeNode) throws ClassCastException {
		if (!(anotherRadixTreeNode instanceof RadixTreeNode))
			throw new ClassCastException("A RadixTreeNode object expected.");
		float anotherNodeValue = ((RadixTreeNode)anotherRadixTreeNode).getValue();
		return (this.value - anotherNodeValue) < 0 ? 1 : -1;    
	}

	/**
	 * Recursive update through all parents when updating a value of the current node.
	 */
	public void updateValues() {
		if (this.isReal())
			this.parent.updateValues();
		else {
			this.setBestDescendant(this.getChildren().get(0).getBestDescendant());
			float val = this.getChildren().get(0).getValue();
			if (this.value >= val)
				return;
			else {
				if (this.parent != null)
					this.parent.getChildren().remove(this);
				this.value = val;
				if (this.parent != null) {
					this.parent.getChildren().insertSorted(this);
					this.parent.updateValues();
				}
			}
		}
		return;
	}

	/**
	 * Recursive update through all parents previous best descendant.
	 * Needs to continue getting updated even if a parent's value is better (it's normal
	 * because it corresponds to the previous best one)
	 */
	public void updatePreviousBestValue() {
		if (this.isReal())
			this.parent.updatePreviousBestValue();
		else {
			this.setBestDescendant(this.getChildren().get(0).getBestDescendant());
			float val = this.getChildren().get(0).getValue();
			this.value = val;
			if (this.parent == null)
				return;
			this.parent.getChildren().remove(this);
			this.parent.getChildren().insertSorted(this);
			this.parent.updatePreviousBestValue();
		}
		return;
	}

	/**
	 * Insert a node in the children list and update all ancestors if necessary
	 */
	public void insertChildWithRespectToAncestors(RadixTreeNode newChild) {
		newChild.setParent(this);
		this.getChildren().insertSorted(newChild);
		this.updateValues();
	}
}
