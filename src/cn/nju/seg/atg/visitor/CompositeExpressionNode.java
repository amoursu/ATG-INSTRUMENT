package cn.nju.seg.atg.visitor;

public class CompositeExpressionNode {

	String operator = null;
	CompositeExpressionNode leftNode = null;
	CompositeExpressionNode rightNode = null;
	
	public String getOperator() {
		return operator;
	}
	public CompositeExpressionNode getLeftNode() {
		return leftNode;
	}
	public CompositeExpressionNode getRightNode() {
		return rightNode;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	public void setLeftNode(CompositeExpressionNode leftNode) {
		this.leftNode = leftNode;
	}
	public void setRightNode(CompositeExpressionNode rightNode) {
		this.rightNode = rightNode;
	}
}
