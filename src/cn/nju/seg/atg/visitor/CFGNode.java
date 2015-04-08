package cn.nju.seg.atg.visitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;

public class CFGNode{	
	List<CFGNode> parents = null;
	List<CFGNode> children = null;
	CFGNode ifChild = null;
	CFGNode elseChild = null;
	IASTBinaryExpression iabe = null;
	int offset = -1;
	int sign = -1; //	0:if分支节点;		1：else分支节点;
				   //	-1:顺序执行节点;		2：for节点;
				   //	3:条件节点;			4:return节点;      
	               //   5:while节点;        6:do-while节点; 
	               //   7:break节点;        8:continue节点 
	int nodeNumber = -1;
	String operator = "";
	
	public void addParent(CFGNode parent){
		if(this.parents == null){
			this.parents = new ArrayList<CFGNode>();
			this.parents.add(parent);
		}else{
			this.parents.add(parent);
		}
	}
	
	public void deleteParent(int offset){
		if(this.parents != null){
			for(int i=0; i<this.parents.size(); i++){
				if(this.parents.get(i).getOffset() == offset){
					this.parents.remove(i);
				}
			}
		}
	}
	
	public void addChild(CFGNode child){
		if(this.children == null){
			this.children = new ArrayList<CFGNode>();
			this.children.add(child);
		}else{
			this.children.add(child);
		}
	}
	
	public void deleteChild(int offset){
		if(this.children != null){
			for(int i=0; i<this.children.size(); i++){
				if(this.children.get(i).getOffset() == offset){
					this.children.remove(i);
				}
			}
		}
	}
	
	public void setIfChild(CFGNode ifChild){
		this.ifChild = ifChild;
	}
	
	public void setElseChild(CFGNode elseChild){
		this.elseChild = elseChild;
	}
	
	public void setBinaryExpression(IASTBinaryExpression iabe){
		this.iabe = iabe;
	}
	
	public void setOffset(int offset){
		this.offset = offset;
	}
	
	public void setSign(int sign){
		this.sign = sign;
	}
	
	public void setNodeNumber(int nodeNumber){
		this.nodeNumber = nodeNumber;
	}
	
	public void setOperator(String operator){
		this.operator = operator;
	}
	
	public ArrayList<CFGNode> getParents(){
		ArrayList<CFGNode> parents = (ArrayList<CFGNode>) this.parents;
		return parents;
	}
	
	public ArrayList<CFGNode> getChildren(){
		ArrayList<CFGNode> children = (ArrayList<CFGNode>)this.children;
		return children;
	}
	
	public CFGNode getIfChild(){
		CFGNode ifChild = this.ifChild;
		return ifChild;
	}
	
	public CFGNode getElseChild(){
		CFGNode elseChild = this.elseChild;
		return elseChild;
	}
	
	public IASTBinaryExpression getBinaryExpression(){
		IASTBinaryExpression iabe = this.iabe;
		return iabe;
	}
	
	public int getOffset(){
		int offset = this.offset;
		return offset;
	}
	
	public int getSign(){
		int sign = this.sign;
		return sign;
	}
	
	public int getNodeNumber(){
		int nodeNumber = this.nodeNumber;
		return nodeNumber;
	}
	
	public int getType(){
		return this.sign;
	}
	
	public String getOperator(){
		String operator = this.operator;
		return operator;
	}
}